package com.loom.synectix.config

import com.posthog.java.DefaultPostHogLogger
import com.posthog.java.PostHog
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class PostHogConfig(
    private val postHogProperties: PostHogProperties,
) {
    private val logger = LoggerFactory.getLogger(PostHogConfig::class.java)
    private var postHogClient: PostHog? = null

    @Bean
    @ConditionalOnProperty(prefix = "posthog", name = ["enabled"], havingValue = "true")
    fun postHog(): PostHog {
        require(postHogProperties.apiKey.isNotBlank()) {
            "posthog.api-key must be configured when posthog.enabled=true"
        }

        val client =
            PostHog
                .Builder(postHogProperties.apiKey)
                .host(postHogProperties.host)
                .logger(DefaultPostHogLogger())
                .build()

        postHogClient = client
        logger.info("PostHog SDK initialized")
        return client
    }

    @PreDestroy
    fun shutdownPostHog() {
        postHogClient?.shutdown()
    }
}
