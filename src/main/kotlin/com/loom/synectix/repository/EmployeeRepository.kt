package com.loom.synectix.repository

import com.loom.synectix.model.Employee
import com.loom.synectix.model.EmploymentStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface EmployeeRepository : JpaRepository<Employee, String> {
    fun findByOrganizationId(organizationId: String): List<Employee>

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
