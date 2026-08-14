package com.aquinofroilan.tessera.domain.auth.repository

import com.aquinofroilan.tessera.domain.auth.model.Invitation
import com.aquinofroilan.tessera.domain.auth.model.InvitationStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.util.Optional

@Repository
interface InvitationRepository : JpaRepository<Invitation, java.util.UUID> {
    fun findByTokenHash(tokenHash: String): Optional<Invitation>

    fun findByOrganizationIdAndStatusAndExpiryAtAfter(
        organizationId: java.util.UUID,
        status: InvitationStatus,
        expiryAt: LocalDateTime,
    ): List<Invitation>

    fun findByEmailAndOrganizationIdAndStatusAndExpiryAtAfter(
        email: String,
        organizationId: java.util.UUID,
        status: InvitationStatus,
        expiryAt: LocalDateTime,
    ): Optional<Invitation>

    fun findByEmailAndOrganizationIdAndStatus(
        email: String,
        organizationId: java.util.UUID,
        status: InvitationStatus,
    ): Optional<Invitation>
}
