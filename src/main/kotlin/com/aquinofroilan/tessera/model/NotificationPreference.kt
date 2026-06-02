package com.aquinofroilan.tessera.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime
import java.util.UUID

enum class NotificationChannel {
    IN_APP,
    EMAIL,
}

@Entity
@Table(name = "notification_preferences")
@EntityListeners(AuditingEntityListener::class)
data class NotificationPreference(
    @Id
    @Column(columnDefinition = "uuid")
    val id: String = UUID.randomUUID().toString(),
    @Column(name = "organization_id", columnDefinition = "uuid")
<<<<<<< HEAD
    val organizationId: java.util.UUID,
    @Column(name = "user_id", columnDefinition = "uuid")
    val userId: java.util.UUID,
=======
    val organizationId: String,
    @Column(name = "user_id", columnDefinition = "uuid")
    val userId: String,
>>>>>>> 61cc253 (feat(notifications): per-user delivery preferences (channel x kind) (#251))
    val kind: String,
    @Enumerated(EnumType.STRING)
    val channel: NotificationChannel,
    val enabled: Boolean = true,
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    val createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    val updatedAt: LocalDateTime? = null,
)
