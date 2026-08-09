package com.aquinofroilan.tessera.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime
import java.util.UUID

enum class NotificationCategory {
    SYSTEM,
    APPROVAL,
    REMINDER,
    EVENT,
    INFO,
}

@Entity
@Table(name = "notifications")
@EntityListeners(AuditingEntityListener::class)
data class Notification(
    @Id
    @Column(columnDefinition = "uuid")
    val id: java.util.UUID = java.util.UUID.randomUUID(),
    @Column(name = "organization_id", columnDefinition = "uuid")
    val organizationId: java.util.UUID,
    @Column(name = "recipient_user_id", columnDefinition = "uuid")
    val recipientUserId: java.util.UUID,
    @Enumerated(EnumType.STRING)
    val category: NotificationCategory,
    val kind: String,
    val title: String,
    val body: String? = null,
    val link: String? = null,
    @Column(name = "read_at")
    val readAt: LocalDateTime? = null,
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    val createdAt: LocalDateTime? = null,
)
