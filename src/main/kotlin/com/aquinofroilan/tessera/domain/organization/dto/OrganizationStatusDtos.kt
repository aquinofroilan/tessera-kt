package com.aquinofroilan.tessera.domain.organization.dto

import com.aquinofroilan.tessera.domain.organization.model.OrganizationStatus
import com.aquinofroilan.tessera.domain.organization.model.Organizations
import jakarta.validation.constraints.NotNull
import java.util.UUID

data class OrganizationStatusResponse(
    val organizationId: UUID,
    val orgSlug: String,
    val name: String,
    val status: OrganizationStatus,
    val readOnly: Boolean,
    val accessBlocked: Boolean,
    val allowedTransitions: List<OrganizationStatus>,
) {
    companion object {
        fun from(
            org: Organizations,
            allowedTransitions: List<OrganizationStatus>,
        ): OrganizationStatusResponse =
            OrganizationStatusResponse(
                organizationId = org.uuid,
                orgSlug = org.orgSlug,
                name = org.name,
                status = org.status,
                readOnly = org.status == OrganizationStatus.ARCHIVED,
                accessBlocked = org.status == OrganizationStatus.SUSPENDED,
                allowedTransitions = allowedTransitions,
            )
    }
}

data class TransitionOrganizationStatusRequest(
    @field:NotNull(message = "Target status is required")
    val targetStatus: OrganizationStatus,
    val reason: String? = null,
)
