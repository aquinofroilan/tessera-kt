package com.loom.synectix.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "posthog")
data class PostHogProperties(
    var enabled: Boolean = false,
    var apiKey: String = "",
    var host: String = "https://us.i.posthog.com",
    var distinctId: String = "synectix-backend",
    var loggingEnabled: Boolean = true,
)
