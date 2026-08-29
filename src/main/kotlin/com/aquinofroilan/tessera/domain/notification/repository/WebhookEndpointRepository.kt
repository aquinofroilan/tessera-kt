package com.aquinofroilan.tessera.domain.notification.repository

import com.aquinofroilan.tessera.domain.notification.model.WebhookEndpoint
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface WebhookEndpointRepository : JpaRepository<WebhookEndpoint, UUID> {
    fun findByOrganizationIdAndIsActiveTrue(organizationId: UUID): List<WebhookEndpoint>
}
