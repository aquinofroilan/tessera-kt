package com.aquinofroilan.tessera.domain.notification.service

import com.aquinofroilan.tessera.config.RabbitMqConfig
import com.aquinofroilan.tessera.domain.notification.repository.WebhookEndpointRepository
import com.aquinofroilan.tessera.event.DomainEvent
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

data class WebhookDomainEventMessage(
    val webhookId: java.util.UUID,
    val eventJson: String,
)

@Component
class DomainEventWebhookEnqueuer(
    private val webhookRepository: WebhookEndpointRepository,
    private val rabbitTemplate: RabbitTemplate,
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(DomainEventWebhookEnqueuer::class.java)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun onDomainEvent(event: DomainEvent) {
        val eventType = event::class.simpleName ?: "UnknownEvent"

        // 1. Publish to Message Bus (Topic Exchange) for external consumers
        rabbitTemplate.convertAndSend(RabbitMqConfig.DOMAIN_EVENT_EXCHANGE, eventType, event)
        log.debug("Published {} to domain.event.exchange", eventType)

        // 2. Publish to individual webhooks
        val webhooks = webhookRepository.findByOrganizationIdAndIsActiveTrue(event.organizationId)
        val payload = objectMapper.writeValueAsString(event)

        webhooks
            .filter {
                val supportedKinds = it.eventTypes.split(",").map(String::trim)
                supportedKinds.contains("*") || supportedKinds.contains(eventType)
            }.forEach { webhook ->
                val message =
                    WebhookDomainEventMessage(
                        webhookId = webhook.id,
                        eventJson = payload,
                    )

                rabbitTemplate.convertAndSend(
                    RabbitMqConfig.NOTIFICATION_EXCHANGE,
                    RabbitMqConfig.DOMAIN_EVENT_WEBHOOK_ROUTING_KEY,
                    message,
                )

                log.debug(
                    "Published webhook domain event message to RabbitMQ for event {} to webhook {}",
                    eventType,
                    webhook.id,
                )
            }
    }
}
