package com.aquinofroilan.tessera.service.notification

import com.aquinofroilan.tessera.config.NotificationEmailProperties
import com.aquinofroilan.tessera.model.Notification
import com.aquinofroilan.tessera.model.NotificationEmailOutbox
import com.aquinofroilan.tessera.repository.NotificationEmailOutboxRepository
import com.aquinofroilan.tessera.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * Sits between [com.aquinofroilan.tessera.service.NotificationService] and the
 * outbox table. Snapshots the recipient's email at enqueue time so a later
 * address change doesn't silently retarget queued mail. Drops cleanly (no
 * row written) when:
 *
 * - the email channel is disabled in config,
 * - the recipient user can't be found, or
 * - the recipient has no email address on file.
 *
 * Per-user delivery preferences land in sub-PR #3 — until then this is the
 * one place to gate email.
 */
@Component
class NotificationEmailEnqueuer(
    private val outboxRepository: NotificationEmailOutboxRepository,
    private val userRepository: UserRepository,
    private val properties: NotificationEmailProperties,
) {
    private val log = LoggerFactory.getLogger(NotificationEmailEnqueuer::class.java)

    @Transactional
    fun enqueue(notification: Notification) {
        if (!properties.enabled) return

        val user =
            userRepository.findById(notification.recipientUserId).orElse(null)
                ?: run {
                    log.warn(
                        "Skipping email enqueue for notification {}: recipient user {} not found",
                        notification.id,
                        notification.recipientUserId,
                    )
                    return
                }

        val email = user.email
        if (email.isBlank()) {
            log.debug(
                "Skipping email enqueue for notification {}: recipient {} has no email on file",
                notification.id,
                notification.recipientUserId,
            )
            return
        }

        outboxRepository.save(
            NotificationEmailOutbox(
                notificationId = notification.id,
                recipientEmail = email,
            ),
        )
    }
}
