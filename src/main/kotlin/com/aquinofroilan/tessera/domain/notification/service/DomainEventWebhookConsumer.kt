package com.aquinofroilan.tessera.domain.notification.service

import com.aquinofroilan.tessera.config.RabbitMqConfig
import com.aquinofroilan.tessera.domain.notification.repository.WebhookEndpointRepository
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import java.time.Duration
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Component
class DomainEventWebhookConsumer(
    private val webhookRepository: WebhookEndpointRepository,
    restTemplateBuilder: RestTemplateBuilder,
) {
    private val log = LoggerFactory.getLogger(DomainEventWebhookConsumer::class.java)

    private val restTemplate =
        restTemplateBuilder
            .setConnectTimeout(Duration.ofSeconds(10))
            .setReadTimeout(Duration.ofSeconds(10))
            .build()

    @RabbitListener(queues = [RabbitMqConfig.DOMAIN_EVENT_WEBHOOK_QUEUE])
    fun consume(message: WebhookDomainEventMessage) {
        log.debug("Received domain event webhook message for webhook {}", message.webhookId)

        val webhook =
            webhookRepository.findById(message.webhookId).orElse(null)
                ?: run {
                    log.warn("Message references missing webhook {} — skipping", message.webhookId)
                    return
                }

        if (!webhook.isActive) {
            log.debug("Webhook {} is no longer active — skipping", webhook.id)
            return
        }

        val payload = message.eventJson
        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON

        webhook.secret?.takeIf { it.isNotBlank() }?.let { secret ->
            val signature = calculateHmacSha256(payload, secret)
            headers.set("X-Tessera-Signature", signature)
        }

        val request = HttpEntity(payload, headers)

        try {
            val response = restTemplate.postForEntity(webhook.url, request, String::class.java)
            if (response.statusCode.is2xxSuccessful) {
                log.debug("Successfully delivered webhook {}", webhook.id)
            } else {
                log.warn("Webhook {} returned status {}", webhook.id, response.statusCode)
                throw RuntimeException("Webhook returned non-2xx status: ${response.statusCode}")
            }
        } catch (e: Exception) {
            log.error("Failed to deliver webhook {}: {}", webhook.id, e.message)
            throw e // Let Spring AMQP handle retry / DLQ routing
        }
    }

    private fun calculateHmacSha256(
        payload: String,
        secret: String,
    ): String {
        val mac = Mac.getInstance("HmacSHA256")
        val secretKeySpec = SecretKeySpec(secret.toByteArray(), "HmacSHA256")
        mac.init(secretKeySpec)
        val hmacBytes = mac.doFinal(payload.toByteArray())
        return hmacBytes.joinToString("") { "%02x".format(it) }
    }
}
