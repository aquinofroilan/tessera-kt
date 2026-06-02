package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.CreateNotificationRequest
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import com.aquinofroilan.tessera.model.Notification
import com.aquinofroilan.tessera.repository.NotificationRepository
import com.aquinofroilan.tessera.service.notification.NotificationEmailEnqueuer
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * In-app notification feed. Any service can call [publish] to drop a row
 * for the recipient; the feed endpoint reads it back. Email delivery is
 * fanned out via [NotificationEmailEnqueuer] inside the same transaction
 * (sub-PR #2). Per-user preferences (#3) and event-driven hooks (#4) land
 * in later sub-PRs.
 */
@Service
class NotificationService(
    private val notificationRepository: NotificationRepository,
    private val emailEnqueuer: NotificationEmailEnqueuer,
) {
    @Transactional
    fun publish(
        request: CreateNotificationRequest,
        organizationId: java.util.UUID,
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
        return saved
    }

    fun listFor(
        recipientUserId: java.util.UUID,
        organizationId: java.util.UUID,
    ): List<Notification> =
        notificationRepository.findByRecipientUserIdAndOrganizationIdOrderByCreatedAtDesc(
            recipientUserId,
            organizationId,
        )

    fun unreadCountFor(
        recipientUserId: java.util.UUID,
        organizationId: java.util.UUID,
    ): Long =
        notificationRepository.countByRecipientUserIdAndOrganizationIdAndReadAtIsNull(
            recipientUserId,
            organizationId,
        )

    @Transactional
    fun markRead(
        id: java.util.UUID,
        recipientUserId: java.util.UUID,
        organizationId: java.util.UUID,
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
        return notificationRepository.save(notification.copy(readAt = LocalDateTime.now(ZoneOffset.UTC)))
    }

    @Transactional
    fun markAllRead(
        recipientUserId: java.util.UUID,
        organizationId: java.util.UUID,
    ): Int =
        notificationRepository.markAllReadFor(
            recipientUserId,
            organizationId,
            LocalDateTime.now(ZoneOffset.UTC),
        )
}
