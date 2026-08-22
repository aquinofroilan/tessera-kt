package com.aquinofroilan.tessera.domain.notification.service

import com.aquinofroilan.tessera.domain.notification.dto.NotificationResponse
import com.aquinofroilan.tessera.domain.notification.model.Notification
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * In-memory fan-out for the bell-badge SSE stream. One entry per
 * (recipient user, organization) tuple holds every active emitter for
 * that user across browser tabs / sessions. Auto-cleans when an emitter
 * completes, times out, or errors.
 *
 * Single-process only — a horizontal scale-out would need a Redis pubsub
 * or similar to fan messages between instances. For a hobby ERP this is
 * intentionally simple.
 */
@Component
class NotificationStreamRegistry {
    private val log = LoggerFactory.getLogger(NotificationStreamRegistry::class.java)
    private val emittersByKey = ConcurrentHashMap<Key, CopyOnWriteArrayList<SseEmitter>>()

    fun register(
        userId: java.util.UUID,
        organizationId: java.util.UUID,
        emitter: SseEmitter,
    ) {
        val key = Key(userId, organizationId)
        val bag = emittersByKey.computeIfAbsent(key) { CopyOnWriteArrayList() }
        bag.add(emitter)

        val cleanup =
            Runnable {
                bag.remove(emitter)
                if (bag.isEmpty()) emittersByKey.remove(key, bag)
            }
        emitter.onCompletion(cleanup)
        emitter.onTimeout(cleanup)
        emitter.onError {
            log.debug("SSE emitter for user {} errored: {}", userId, it.message)
            cleanup.run()
        }
    }

    fun broadcastUnread(
        userId: java.util.UUID,
        organizationId: java.util.UUID,
        unread: Long,
    ) {
        sendAll(userId, organizationId, "unread", mapOf("unread" to unread))
    }

    fun broadcastNotification(
        userId: java.util.UUID,
        organizationId: java.util.UUID,
        notification: Notification,
    ) {
        sendAll(userId, organizationId, "notification", NotificationResponse.from(notification))
    }

    private fun sendAll(
        userId: java.util.UUID,
        organizationId: java.util.UUID,
        eventName: String,
        payload: Any,
    ) {
        val key = Key(userId, organizationId)
        val bag = emittersByKey[key] ?: return
        bag.forEach { emitter ->
            try {
                emitter.send(SseEmitter.event().name(eventName).data(payload))
            } catch (e: Exception) {
                log.debug("Dropping dead SSE emitter for user {}: {}", userId, e.message)
                bag.remove(emitter)
                emitter.completeWithError(e)
            }
        }
        if (bag.isEmpty()) emittersByKey.remove(key, bag)
    }

    private data class Key(
        val userId: java.util.UUID,
        val organizationId: java.util.UUID,
    )
}
