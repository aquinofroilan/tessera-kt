package com.froilan.synectix.repository

import com.froilan.synectix.model.Invitation
import com.froilan.synectix.model.InvitationStatus
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface InvitationRepository : MongoRepository<Invitation, String> {
    fun findByTokenHash(tokenHash: String): Optional<Invitation>

    fun findByOrganizationIdAndStatus(
        organizationId: String,
        status: InvitationStatus,
    ): List<Invitation>

    fun findByEmailAndOrganizationIdAndStatus(
        email: String,
        organizationId: String,
        status: InvitationStatus,
    ): Optional<Invitation>
}
