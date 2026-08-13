package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.CreateNotificationRequest
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import com.aquinofroilan.tessera.model.Notification
import com.aquinofroilan.tessera.repository.NotificationRepository
import com.aquinofroilan.tessera.service.notification.NotificationEmailEnqueuer
import com.aquinofroilan.tessera.service.notification.NotificationStreamRegistry
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

/**
 * In-app notification feed. Any service can call [publish] to drop a row
 * for the recipient; the feed endpoint reads it back. Email delivery is
 * fanned out via [NotificationEmailEnqueuer] inside the same transaction;
 * the live bell-badge SSE stream is fanned out via [NotificationStreamRegistry]
 * after save (best-effort — a backend hiccup in the registry doesn't roll
 * back the source transaction).
 */
@Service
class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val emailEnqueuer: NotificationEmailEnqueuer,
    private val streamRegistry: NotificationStreamRegistry,
) {
    @Transactional
    fun publish(
        request: CreateNotificationRequest,
        organizationId: UUID,
    ): Notification {
        val saved =
            notificationRepository.save(
                Notification(
                    organizationId = organizationId,
                    recipientUserId = request.recipientUserId,
                    category = request.category,
                    kind = request.kind,
                    title = request.title,
                    body = request.body,
                    link = request.link,
                ),
            )
        emailEnqueuer.enqueue(saved)
        val unread = unreadCountFor(saved.recipientUserId, saved.organizationId)
        streamRegistry.broadcastNotification(saved.recipientUserId, saved.organizationId, saved)
        streamRegistry.broadcastUnread(saved.recipientUserId, saved.organizationId, unread)
        return saved
    }

    fun listFor(
        recipientUserId: UUID,
        organizationId: UUID,
    ): List<Notification> =
        notificationRepository.findByRecipientUserIdAndOrganizationIdOrderByCreatedAtDesc(
            recipientUserId,
            organizationId,
        )

    fun unreadCountFor(
        recipientUserId: UUID,
        organizationId: UUID,
    ): Long =
        notificationRepository.countByRecipientUserIdAndOrganizationIdAndReadAtIsNull(
            recipientUserId,
            organizationId,
        )

    @Transactional
    fun markRead(
        id: UUID,
        recipientUserId: UUID,
        organizationId: UUID,
    ): Notification {
        val notification =
            notificationRepository.findById(id).orElseThrow {
                ResourceNotFoundException("Notification $id not found")
            }
        // Same guard pattern as the other org-scoped lookups: cross-tenant or
        // wrong-recipient access surfaces as "not found".
        if (notification.organizationId != organizationId || notification.recipientUserId != recipientUserId) {
            throw ResourceNotFoundException("Notification $id not found")
        }
        if (notification.readAt != null) return notification
        val updated = notificationRepository.save(notification.copy(readAt = LocalDateTime.now(ZoneOffset.UTC)))
        val unread = unreadCountFor(recipientUserId, organizationId)
        streamRegistry.broadcastUnread(recipientUserId, organizationId, unread)
        return updated
    }

    @Transactional
    fun markAllRead(
        recipientUserId: UUID,
        organizationId: UUID,
    ): Int {
        val touched =
            notificationRepository.markAllReadFor(
                recipientUserId,
                organizationId,
                LocalDateTime.now(ZoneOffset.UTC),
            )
        if (touched > 0) {
            streamRegistry.broadcastUnread(recipientUserId, organizationId, 0L)
        }
        return touched
    }
}
