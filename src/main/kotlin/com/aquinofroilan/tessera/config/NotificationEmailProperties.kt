package com.aquinofroilan.tessera.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("tessera.notifications.email")
data class NotificationEmailProperties(
    val enabled: Boolean = true,
    val from: String = "noreply@tessera.local",
    val senderName: String = "Tessera",
    val dispatcher: Dispatcher = Dispatcher(),
) {
    data class Dispatcher(
        val cron: String = "0 * * * * *",
        val maxAttempts: Int = 5,
        val batchSize: Int = 25,
        val retryBackoffSeconds: Long = 60,
    )
}
