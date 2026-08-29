package com.aquinofroilan.tessera.domain.organization.repository

import com.aquinofroilan.tessera.domain.organization.model.OrganizationFeatureFlag
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional
import java.util.UUID

@Repository
interface OrganizationFeatureFlagRepository : JpaRepository<OrganizationFeatureFlag, UUID> {
    fun findByOrganizationId(organizationId: UUID): List<OrganizationFeatureFlag>

    fun findByOrganizationIdAndFeatureKey(
        organizationId: UUID,
        featureKey: String,
    ): Optional<OrganizationFeatureFlag>

    fun deleteByOrganizationIdAndFeatureKey(
        organizationId: UUID,
        featureKey: String,
    ): Long

    fun deleteByOrganizationId(organizationId: UUID): Long
}
