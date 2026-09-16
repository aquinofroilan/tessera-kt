package com.aquinofroilan.tessera.domain.hr.repository

import com.aquinofroilan.tessera.domain.hr.model.ChecklistTemplate
import com.aquinofroilan.tessera.domain.hr.model.ChecklistType
import com.aquinofroilan.tessera.domain.hr.model.EmployeeChecklist
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ChecklistTemplateRepository : JpaRepository<ChecklistTemplate, java.util.UUID> {
    fun findByOrganizationIdAndType(
        organizationId: java.util.UUID,
        type: ChecklistType,
    ): List<ChecklistTemplate>
}

@Repository
interface EmployeeChecklistRepository : JpaRepository<EmployeeChecklist, java.util.UUID> {
    fun findByOrganizationIdAndEmployeeId(
        organizationId: java.util.UUID,
        employeeId: java.util.UUID,
    ): List<EmployeeChecklist>
}
