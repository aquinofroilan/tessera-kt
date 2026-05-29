package com.loom.synectix.repository

import com.loom.synectix.model.EmployeeCompensation
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface EmployeeCompensationRepository : JpaRepository<EmployeeCompensation, String> {
    fun findByOrganizationIdAndEmployeeIdOrderByEffectiveDateDesc(
        organizationId: String,
        employeeId: String,
    ): List<EmployeeCompensation>
}
