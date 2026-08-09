package com.aquinofroilan.tessera.controller

import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.service.NotificationService
import com.aquinofroilan.tessera.service.notification.NotificationStreamRegistry
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

/**
 * Server-sent events stream that backs the live unread-count bell.
 * Each connection is a long-lived [SseEmitter] held in
 * [NotificationStreamRegistry]; the registry pushes 'unread' and
 * 'notification' events whenever [NotificationService.publish] saves a
 * row for the caller.
 *
 * The endpoint immediately pushes the current unread count so the
 * client doesn't have to make a second request to bootstrap.
 */
@RestController
@RequestMapping("/notifications")
class NotificationStreamController(
    private val notificationService: NotificationService,
    private val streamRegistry: NotificationStreamRegistry,
    private val authContext: AuthenticationContext,
) {
    private val log = LoggerFactory.getLogger(NotificationStreamController::class.java)

    @GetMapping("/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    @PreAuthorize("isAuthenticated()")
    fun stream(): SseEmitter {
        val orgId =
            authContext.organizationId()
                ?: throw IllegalStateException("Stream endpoint called without an org context — auth filter misconfigured")
        val userId =
            authContext.userId()
                ?: throw IllegalStateException("Stream endpoint called without a user context — auth filter misconfigured")

        // 30-minute idle timeout; browsers auto-reconnect on EventSource
        // disconnect so this is effectively a heartbeat ceiling, not a
        // session lifetime.
        val emitter = SseEmitter(STREAM_TIMEOUT_MS)
        streamRegistry.register(userId, orgId, emitter)

        try {
            val unread = notificationService.unreadCountFor(userId, orgId)
            emitter.send(SseEmitter.event().name("unread").data(mapOf("unread" to unread)))
        } catch (e: Exception) {
            log.warn("Failed to push initial unread count for user {}: {}", userId, e.message)
            emitter.completeWithError(e)
        }

        return emitter
    }

    companion object {
        private const val STREAM_TIMEOUT_MS: Long = 30L * 60_000
    }
}
