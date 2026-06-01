package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.CreateNotificationRequest
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import com.aquinofroilan.tessera.model.Notification
import com.aquinofroilan.tessera.repository.NotificationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * In-app notification feed. Any service can call [publish] to drop a row
 * for the recipient; the feed endpoint reads it back. Email delivery,
 * per-user preferences, and event-driven hooks land in later sub-PRs.
 */
@Service
class NotificationService(
    private val notificationRepository: NotificationRepository,
) {
    @Transactional
    fun publish(
        request: CreateNotificationRequest,
        organizationId: String,
    ): Notification =
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

    fun listFor(
        recipientUserId: String,
        organizationId: String,
    ): List<Notification> =
        notificationRepository.findByRecipientUserIdAndOrganizationIdOrderByCreatedAtDesc(
            recipientUserId,
            organizationId,
        )

    fun unreadCountFor(
        recipientUserId: String,
        organizationId: String,
    ): Long =
        notificationRepository.countByRecipientUserIdAndOrganizationIdAndReadAtIsNull(
            recipientUserId,
            organizationId,
        )

    @Transactional
    fun markRead(
        id: String,
        recipientUserId: String,
        organizationId: String,
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
        recipientUserId: String,
        organizationId: String,
    ): Int =
        notificationRepository.markAllReadFor(
            recipientUserId,
            organizationId,
            LocalDateTime.now(ZoneOffset.UTC),
        )
}
