package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.Employee
import com.aquinofroilan.tessera.model.EmploymentStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface EmployeeRepository : JpaRepository<Employee, java.util.UUID> {
    fun findByOrganizationId(organizationId: java.util.UUID): List<Employee>

    fun findByOrganizationIdAndUserId(
        organizationId: java.util.UUID,
        userId: java.util.UUID,
    ): Optional<Employee>

    fun findByOrganizationIdAndStatus(
        organizationId: java.util.UUID,
        status: EmploymentStatus,
    ): List<Employee>

    fun findByOrganizationIdAndDepartmentId(
        organizationId: java.util.UUID,
        departmentId: java.util.UUID,
    ): List<Employee>

    fun countByOrganizationId(organizationId: java.util.UUID): Long
}
