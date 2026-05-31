package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.WorkCenter
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface WorkCenterRepository : JpaRepository<WorkCenter, String> {
    fun findByOrganizationId(organizationId: String): List<WorkCenter>

    fun findByOrganizationIdAndIsActive(
        organizationId: String,
        isActive: Boolean,
    ): List<WorkCenter>

    fun findByOrganizationIdAndCode(
        organizationId: String,
        code: String,
    ): Optional<WorkCenter>
}
