package com.aquinofroilan.tessera.domain.auth.repository

import com.aquinofroilan.tessera.domain.auth.model.OAuth2Client
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface OAuth2ClientRepository : JpaRepository<OAuth2Client, UUID> {
    fun findByClientId(clientId: String): OAuth2Client?

    fun findByOrganizationId(organizationId: UUID): List<OAuth2Client>
}
