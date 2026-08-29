package com.aquinofroilan.tessera.domain.notification.service

import com.aquinofroilan.tessera.config.RabbitMqConfig
import com.aquinofroilan.tessera.domain.notification.repository.NotificationRepository
import com.aquinofroilan.tessera.domain.notification.repository.WebhookEndpointRepository
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Component
class NotificationWebhookConsumer(
    private val notificationRepository: NotificationRepository,
    private val webhookRepository: WebhookEndpointRepository,
) {
    private val log = LoggerFactory.getLogger(NotificationWebhookConsumer::class.java)

    private val restTemplate = RestTemplate()

    @RabbitListener(queues = [RabbitMqConfig.WEBHOOK_QUEUE])
    fun consume(message: WebhookNotificationMessage) {
        log.debug("Received webhook notification message for notification {}", message.notificationId)

        val notification =
            notificationRepository.findById(message.notificationId).orElse(null)
                ?: run {
                    log.warn("Message references missing notification {} — skipping", message.notificationId)
                    return
                }

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

        val payload =
            """
            {
                "id": "${notification.id}",
                "kind": "${notification.kind}",
                "title": "${notification.title}",
                "body": "${notification.body?.replace("\"", "\\\"") ?: ""}",
                "link": "${notification.link ?: ""}"
            }
            """.trimIndent()

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
                log.debug("Successfully delivered webhook {} for notification {}", webhook.id, notification.id)
            } else {
                log.warn("Webhook {} returned status {} for notification {}", webhook.id, response.statusCode, notification.id)
                throw RuntimeException("Webhook returned non-2xx status: ${response.statusCode}")
            }
        } catch (e: Exception) {
            log.error("Failed to deliver webhook {} for notification {}: {}", webhook.id, notification.id, e.message)
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
