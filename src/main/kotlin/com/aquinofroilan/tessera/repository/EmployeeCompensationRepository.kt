package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.EmployeeCompensation
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface EmployeeCompensationRepository : JpaRepository<EmployeeCompensation, String> {
    fun findByOrganizationIdAndEmployeeIdOrderByEffectiveDateDesc(
        organizationId: String,
        employeeId: String,
    ): List<EmployeeCompensation>
}
