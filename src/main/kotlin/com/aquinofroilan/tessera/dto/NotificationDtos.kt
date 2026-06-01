package com.aquinofroilan.tessera.dto

import com.aquinofroilan.tessera.model.Notification
import com.aquinofroilan.tessera.model.NotificationCategory
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateNotificationRequest(
    @field:NotBlank(message = "Recipient is required")
    val recipientUserId: String,
    val category: NotificationCategory = NotificationCategory.INFO,
    @field:NotBlank(message = "Kind is required")
    @field:Size(max = 100)
    val kind: String,
    @field:NotBlank(message = "Title is required")
    @field:Size(max = 200)
    val title: String,
    @field:Size(max = 2000)
    val body: String? = null,
    @field:Size(max = 500)
    val link: String? = null,
)

data class NotificationResponse(
    val id: String,
    val recipientUserId: String,
    val category: NotificationCategory,
    val kind: String,
    val title: String,
    val body: String?,
    val link: String?,
    val readAt: String?,
    val createdAt: String?,
) {
    companion object {
        fun from(notification: Notification) =
            NotificationResponse(
                id = notification.id,
                recipientUserId = notification.recipientUserId,
                category = notification.category,
                kind = notification.kind,
                title = notification.title,
                body = notification.body,
                link = notification.link,
                readAt = notification.readAt?.toString(),
                createdAt = notification.createdAt?.toString(),
            )
    }
}

data class NotificationUnreadCountResponse(
    val unread: Long,
)

data class NotificationMarkAllReadResponse(
    val markedRead: Int,
)
