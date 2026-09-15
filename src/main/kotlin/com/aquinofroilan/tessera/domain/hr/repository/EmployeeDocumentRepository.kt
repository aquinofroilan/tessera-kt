package com.aquinofroilan.tessera.domain.hr.repository

import com.aquinofroilan.tessera.domain.hr.model.EmployeeDocument
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface EmployeeDocumentRepository : JpaRepository<EmployeeDocument, java.util.UUID> {
    fun findByOrganizationIdAndEmployeeId(
        organizationId: java.util.UUID,
        employeeId: java.util.UUID,
    ): List<EmployeeDocument>

    fun findByOrganizationIdAndExpiryDateBetween(
        organizationId: java.util.UUID,
        start: LocalDate,
        end: LocalDate,
    ): List<EmployeeDocument>
}
