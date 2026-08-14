package com.aquinofroilan.tessera.domain.notification.service

import com.aquinofroilan.tessera.config.NotificationEmailProperties
import com.aquinofroilan.tessera.domain.notification.model.EmailDeliveryStatus
import com.aquinofroilan.tessera.domain.notification.model.NotificationEmailOutbox
import com.aquinofroilan.tessera.domain.notification.repository.NotificationEmailOutboxRepository
import com.aquinofroilan.tessera.domain.notification.repository.NotificationRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.domain.Limit
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Scheduled outbox drain. Picks up PENDING rows whose scheduled_at has come
 * due, asks [EmailSender] to deliver, and:
 *
 * - on success → marks SENT with sent_at.
 * - on intentional skip (sender returned false) → marks SKIPPED.
 * - on transport failure → bumps attempts, records last_error, and either
 *   reschedules (linear backoff = attempts × retryBackoffSeconds) or marks
 *   FAILED once max-attempts is exhausted.
 *
 * Disabled via `tessera.notifications.email.enabled=false` — when off the
 * bean isn't loaded, so even unconfigured dev environments don't churn the
 * scheduler.
 */
@Component
@ConditionalOnProperty(
    name = ["tessera.notifications.email.enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
class NotificationEmailDispatcher(
    private val outboxRepository: NotificationEmailOutboxRepository,
    private val notificationRepository: NotificationRepository,
    private val emailSender: EmailSender,
    private val renderer: NotificationEmailRenderer,
    private val properties: NotificationEmailProperties,
) {
    private val log = LoggerFactory.getLogger(NotificationEmailDispatcher::class.java)

    @Scheduled(cron = "\${tessera.notifications.email.dispatcher.cron:0 * * * * *}", zone = "UTC")
    @Transactional
    fun drain() {
        val now = LocalDateTime.now(ZoneOffset.UTC)
        val batch =
            outboxRepository.findByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(
                EmailDeliveryStatus.PENDING,
                now,
                Limit.of(properties.dispatcher.batchSize),
            )
        if (batch.isEmpty()) return

        log.debug("Dispatching {} pending notification email(s)", batch.size)
        batch.forEach { dispatchOne(it) }
    }

    private fun dispatchOne(row: NotificationEmailOutbox) {
        val notification =
            notificationRepository.findById(row.notificationId).orElse(null)
                ?: run {
                    log.warn("Outbox row {} references missing notification {} — marking FAILED", row.id, row.notificationId)
                    outboxRepository.save(
                        row.copy(
                            status = EmailDeliveryStatus.FAILED,
                            lastError = "Notification row deleted before delivery",
                        ),
                    )
                    return
                }

        try {
            val delivered = emailSender.send(row.recipientEmail, renderer.render(notification))
            if (delivered) {
                outboxRepository.save(
                    row.copy(
                        status = EmailDeliveryStatus.SENT,
                        attempts = row.attempts + 1,
                        sentAt = LocalDateTime.now(ZoneOffset.UTC),
                        lastError = null,
                    ),
                )
            } else {
                outboxRepository.save(
                    row.copy(
                        status = EmailDeliveryStatus.SKIPPED,
                        attempts = row.attempts + 1,
                    ),
                )
            }
        } catch (e: Exception) {
            val attempts = row.attempts + 1
            val exhausted = attempts >= properties.dispatcher.maxAttempts
            val next =
                if (exhausted) {
                    row.copy(
                        status = EmailDeliveryStatus.FAILED,
                        attempts = attempts,
                        lastError = e.message?.take(MAX_ERROR_LENGTH),
                    )
                } else {
                    row.copy(
                        attempts = attempts,
                        lastError = e.message?.take(MAX_ERROR_LENGTH),
                        scheduledAt = LocalDateTime.now(ZoneOffset.UTC).plusSeconds(attempts * properties.dispatcher.retryBackoffSeconds),
                    )
                }
            outboxRepository.save(next)
            log.warn(
                "Failed to dispatch notification email for outbox row {} (attempt {}/{}): {}",
                row.id,
                attempts,
                properties.dispatcher.maxAttempts,
                e.message,
            )
        }
    }

    companion object {
        private const val MAX_ERROR_LENGTH = 500
    }
}
