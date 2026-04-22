package com.loom.synectix.service

import com.loom.synectix.config.PostHogProperties
import com.posthog.java.PostHog
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Service

@Service
class PostHogLoggingService(
    private val postHogProperties: PostHogProperties,
    private val postHogProvider: ObjectProvider<PostHog>,
) {
    /**
     * Captures an exception event to PostHog using only non-sensitive metadata.
     * Raw exception/log message text is intentionally NOT sent, since it can
     * contain PII or secrets; only the exception type and code origin are.
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

        postHog.capture(postHogProperties.distinctId, "application_log", properties)
    }
}
