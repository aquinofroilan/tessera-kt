package com.froilan.synectix.model

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime
import java.util.UUID

enum class InvitationStatus {
    PENDING,
    ACCEPTED,
    EXPIRED,
    REVOKED,
}

@Document(collection = "invitations")
data class Invitation(
    @Id
    val id: String = UUID.randomUUID().toString(),
    @Indexed
    val email: String,
    val organizationId: String,
    val role: String,
    @Indexed(unique = true)
    val tokenHash: String,
    val invitedBy: String,
    val status: InvitationStatus = InvitationStatus.PENDING,
    @Indexed(expireAfterSeconds = 0)
    val expiryAt: LocalDateTime,
    @CreatedDate
    var createdAt: LocalDateTime? = null,
)
