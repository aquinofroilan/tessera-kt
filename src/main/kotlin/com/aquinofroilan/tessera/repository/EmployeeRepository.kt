package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.Employee
import com.aquinofroilan.tessera.model.EmploymentStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface EmployeeRepository : JpaRepository<Employee, String> {
    fun findByOrganizationId(organizationId: String): List<Employee>

    fun findByOrganizationIdAndUserId(
        organizationId: String,
        userId: String,
    ): Optional<Employee>

    fun findByOrganizationIdAndStatus(
        organizationId: String,
        status: EmploymentStatus,
    ): List<Employee>

    fun findByOrganizationIdAndDepartmentId(
        organizationId: String,
        departmentId: String,
    ): List<Employee>

    fun countByOrganizationId(organizationId: String): Long
}
