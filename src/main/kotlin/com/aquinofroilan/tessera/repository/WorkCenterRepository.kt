package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.WorkCenter
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface WorkCenterRepository : JpaRepository<WorkCenter, java.util.UUID> {
    fun findByOrganizationId(organizationId: java.util.UUID): List<WorkCenter>

    fun findByOrganizationIdAndIsActive(
        organizationId: java.util.UUID,
        isActive: Boolean,
    ): List<WorkCenter>

    fun findByOrganizationIdAndCode(
        organizationId: java.util.UUID,
        code: String,
    ): Optional<WorkCenter>
}
