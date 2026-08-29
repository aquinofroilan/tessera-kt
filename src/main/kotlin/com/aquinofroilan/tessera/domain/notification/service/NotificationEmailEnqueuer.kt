package com.aquinofroilan.tessera.domain.notification.service

import com.aquinofroilan.tessera.config.NotificationEmailProperties
import com.aquinofroilan.tessera.config.RabbitMqConfig
import com.aquinofroilan.tessera.domain.auth.repository.UserRepository
import com.aquinofroilan.tessera.domain.notification.model.Notification
import com.aquinofroilan.tessera.domain.notification.model.NotificationChannel
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Component

data class EmailNotificationMessage(
    val notificationId: java.util.UUID,
    val recipientEmail: String
)

@Component
class NotificationEmailEnqueuer(
    private val userRepository: UserRepository,
    private val preferenceService: NotificationPreferenceService,
    private val properties: NotificationEmailProperties,
    private val rabbitTemplate: RabbitTemplate
) {
    private val log = LoggerFactory.getLogger(NotificationEmailEnqueuer::class.java)

    fun enqueue(notification: Notification) {
        if (!properties.enabled) return

        val user = userRepository.findById(notification.recipientUserId).orElse(null)
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

        val enabled = preferenceService.isEnabled(
            userId = notification.recipientUserId,
            organizationId = notification.organizationId,
            kind = notification.kind,
            channel = NotificationChannel.EMAIL,
        )
        if (!enabled) {
            log.debug(
                "Skipping email enqueue for notification {}: recipient {} opted out of kind '{}'",
                notification.id,
                notification.recipientUserId,
                notification.kind,
            )
            return
        }

        val message = EmailNotificationMessage(
            notificationId = notification.id,
            recipientEmail = email
        )
        
        rabbitTemplate.convertAndSend(
            RabbitMqConfig.NOTIFICATION_EXCHANGE,
            RabbitMqConfig.EMAIL_ROUTING_KEY,
            message
        )
        
        log.debug("Published email notification message to RabbitMQ for notification {}", notification.id)
    }
}
