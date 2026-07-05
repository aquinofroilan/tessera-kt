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

enum class InvitationStatus {
    PENDING,
    ACCEPTED,
    EXPIRED,
    REVOKED,
}

@Entity
@Table(name = "invitations")
@EntityListeners(AuditingEntityListener::class)
class Invitation(
    @Id
    @Column(columnDefinition = "uuid")
    var id: String = UUID.randomUUID().toString(),
    var email: String,
    @Column(name = "organization_id", columnDefinition = "uuid")
    var organizationId: String,
    var role: String,
    @Column(name = "token_hash")
    var tokenHash: String,
    @Column(name = "invited_by", columnDefinition = "uuid")
    var invitedBy: String,
    @Enumerated(EnumType.STRING)
    var status: InvitationStatus = InvitationStatus.PENDING,
    @Column(name = "expiry_at")
    var expiryAt: LocalDateTime,
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null,
)
