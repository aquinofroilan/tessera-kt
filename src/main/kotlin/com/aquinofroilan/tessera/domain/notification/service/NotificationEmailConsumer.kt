package com.aquinofroilan.tessera.domain.notification.service

import com.aquinofroilan.tessera.config.RabbitMqConfig
import com.aquinofroilan.tessera.domain.notification.repository.NotificationRepository
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(
    name = ["tessera.notifications.email.enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
class NotificationEmailConsumer(
    private val notificationRepository: NotificationRepository,
    private val emailSender: EmailSender,
    private val renderer: NotificationEmailRenderer,
) {
    private val log = LoggerFactory.getLogger(NotificationEmailConsumer::class.java)

    @RabbitListener(queues = [RabbitMqConfig.EMAIL_QUEUE])
    fun consume(message: EmailNotificationMessage) {
        log.debug("Received email notification message for notification {}", message.notificationId)

        val notification =
            notificationRepository.findById(message.notificationId).orElse(null)
                ?: run {
                    log.warn("Message references missing notification {} — skipping", message.notificationId)
                    return
                }

        try {
            val delivered = emailSender.send(message.recipientEmail, renderer.render(notification))
            if (delivered) {
                log.debug("Successfully delivered email for notification {}", message.notificationId)
            } else {
                log.debug("Skipped email delivery for notification {} (sender returned false)", message.notificationId)
            }
        } catch (e: Exception) {
            log.error("Failed to deliver email for notification {}: {}", message.notificationId, e.message, e)
            throw e // Let Spring AMQP handle retry / DLQ routing
        }
    }
}
