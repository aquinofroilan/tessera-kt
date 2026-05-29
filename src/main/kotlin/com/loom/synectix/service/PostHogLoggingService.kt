package com.loom.synectix.service

import com.loom.synectix.config.PostHogProperties
import com.posthog.java.PostHog
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Service

@Service
class PostHogLoggingService(
    private val postHogProperties: PostHogProperties,
    private val postHogProvider: ObjectProvider<PostHog>,
) {
    private val log = LoggerFactory.getLogger(PostHogLoggingService::class.java)

    /**
     * Captures an exception event to PostHog using only non-sensitive metadata.
     * Raw exception/log message text is intentionally NOT sent, since it can
     * contain PII or secrets; only the exception type and code origin are.
     *
     * This method must never throw: it is invoked from @AfterThrowing advice on
     * the service layer, so a thrown exception here would mask the original
     * application exception and re-trigger the aspect recursively.
     */
    fun captureException(
        level: String,
        source: String,
        throwable: Throwable,
    ) {
        if (!postHogProperties.enabled || !postHogProperties.loggingEnabled) {
            return
        }

        val postHog = postHogProvider.ifAvailable ?: return

        val properties =
            mutableMapOf<String, Any>(
                "level" to level,
                "source" to source,
                "exception_type" to throwable.javaClass.name,
            )

        throwable.stackTrace.firstOrNull()?.let { frame ->
            properties["exception_origin"] = "${frame.className}.${frame.methodName}(${frame.fileName}:${frame.lineNumber})"
        }

        runCatching {
            postHog.capture(postHogProperties.distinctId, "application_log", properties)
        }.onFailure { e ->
            log.warn("Failed to forward exception to PostHog: {}", e.message)
        }
    }
}
