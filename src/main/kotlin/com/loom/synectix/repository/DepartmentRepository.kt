package com.loom.synectix.repository

import com.loom.synectix.model.Department
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface DepartmentRepository : JpaRepository<Department, String> {
    fun findByOrganizationId(organizationId: String): List<Department>

    fun findByOrganizationIdAndIsActive(
        organizationId: String,
        isActive: Boolean,
    ): List<Department>

    fun findByOrganizationIdAndCode(
        organizationId: String,
        code: String,
    ): Optional<Department>
}
