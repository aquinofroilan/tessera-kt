package com.aquinofroilan.tessera.domain.hr.repository

import com.aquinofroilan.tessera.domain.hr.model.EmployeeCompensation
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface EmployeeCompensationRepository : JpaRepository<EmployeeCompensation, java.util.UUID> {
    fun findByOrganizationIdAndEmployeeIdOrderByEffectiveDateDesc(
        organizationId: java.util.UUID,
        employeeId: java.util.UUID,
    ): List<EmployeeCompensation>
}
