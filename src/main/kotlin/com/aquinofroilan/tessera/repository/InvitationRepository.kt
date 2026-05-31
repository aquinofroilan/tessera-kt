package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.Invitation
import com.aquinofroilan.tessera.model.InvitationStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.Optional

@Repository
interface InvitationRepository : JpaRepository<Invitation, String> {
    fun findByTokenHash(tokenHash: String): Optional<Invitation>

    fun findByOrganizationIdAndStatusAndExpiryAtAfter(
        organizationId: String,
        status: InvitationStatus,
        expiryAt: LocalDateTime,
    ): List<Invitation>

    fun findByEmailAndOrganizationIdAndStatusAndExpiryAtAfter(
        email: String,
        organizationId: String,
        status: InvitationStatus,
        expiryAt: LocalDateTime,
    ): Optional<Invitation>

    fun findByEmailAndOrganizationIdAndStatus(
        email: String,
        organizationId: String,
        status: InvitationStatus,
    ): Optional<Invitation>
}
