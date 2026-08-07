package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.Department
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface DepartmentRepository : JpaRepository<Department, java.util.UUID> {
    fun findByOrganizationId(organizationId: java.util.UUID): List<Department>

    fun findByOrganizationIdAndIsActive(
        organizationId: java.util.UUID,
        isActive: Boolean,
    ): List<Department>

    fun findByOrganizationIdAndCode(
        organizationId: java.util.UUID,
        code: String,
    ): Optional<Department>
}
