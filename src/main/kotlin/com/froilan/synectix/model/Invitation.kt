package com.froilan.synectix.model

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

enum class InvitationStatus {
    PENDING,
    ACCEPTED,
    EXPIRED,
    REVOKED,
}

@Entity
@Table(name = "invitations")
@EntityListeners(AuditingEntityListener::class)
data class Invitation(
    @Id
    @Column(columnDefinition = "uuid")
    val id: String = UUID.randomUUID().toString(),
    val email: String,
    @Column(name = "organization_id", columnDefinition = "uuid")
    val organizationId: String,
    val role: String,
    @Column(name = "token_hash")
    val tokenHash: String,
    @Column(name = "invited_by", columnDefinition = "uuid")
    val invitedBy: String,
    @Enumerated(EnumType.STRING)
    val status: InvitationStatus = InvitationStatus.PENDING,
    @Column(name = "expiry_at")
    val expiryAt: LocalDateTime,
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null,
)
