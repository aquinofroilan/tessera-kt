package com.aquinofroilan.tessera.domain.notification.service

import com.aquinofroilan.tessera.config.RabbitMqConfig
import com.aquinofroilan.tessera.domain.notification.model.Notification
import com.aquinofroilan.tessera.domain.notification.repository.WebhookEndpointRepository
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Component

data class WebhookNotificationMessage(
    val notificationId: java.util.UUID,
    val webhookId: java.util.UUID,
)

@Component
class NotificationWebhookEnqueuer(
    private val webhookRepository: WebhookEndpointRepository,
    private val rabbitTemplate: RabbitTemplate,
) {
    private val log = LoggerFactory.getLogger(NotificationWebhookEnqueuer::class.java)

    fun enqueue(notification: Notification) {
        val webhooks = webhookRepository.findByOrganizationIdAndIsActiveTrue(notification.organizationId)

        webhooks
            .filter {
                val supportedKinds = it.eventTypes.split(",").map(String::trim)
                supportedKinds.contains("*") || supportedKinds.contains(notification.kind)
            }.forEach { webhook ->
                val message =
                    WebhookNotificationMessage(
                        notificationId = notification.id,
                        webhookId = webhook.id,
                    )

                rabbitTemplate.convertAndSend(
                    RabbitMqConfig.NOTIFICATION_EXCHANGE,
                    RabbitMqConfig.WEBHOOK_ROUTING_KEY,
                    message,
                )

                log.debug(
                    "Published webhook notification message to RabbitMQ for notification {} to webhook {}",
                    notification.id,
                    webhook.id,
                )
            }
    }
}
