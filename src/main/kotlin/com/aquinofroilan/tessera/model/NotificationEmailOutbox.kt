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

enum class EmailDeliveryStatus {
    PENDING,
    SENT,
    FAILED,
    SKIPPED,
}

@Entity
@Table(name = "notification_email_outbox")
@EntityListeners(AuditingEntityListener::class)
data class NotificationEmailOutbox(
    @Id
    @Column(columnDefinition = "uuid")
    val id: String = UUID.randomUUID().toString(),
    @Column(name = "notification_id", columnDefinition = "uuid")
    val notificationId: java.util.UUID,
    @Column(name = "recipient_email")
    val recipientEmail: String,
    @Enumerated(EnumType.STRING)
    val status: EmailDeliveryStatus = EmailDeliveryStatus.PENDING,
    val attempts: Int = 0,
    @Column(name = "last_error")
    val lastError: String? = null,
    @Column(name = "scheduled_at")
    val scheduledAt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "sent_at")
    val sentAt: LocalDateTime? = null,
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    val createdAt: LocalDateTime? = null,
)
