package com.aquinofroilan.tessera.domain.hr.repository

import com.aquinofroilan.tessera.domain.hr.model.Appraisal
import com.aquinofroilan.tessera.domain.hr.model.AppraisalCycle
import com.aquinofroilan.tessera.domain.hr.model.AppraisalCycleStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface AppraisalCycleRepository : JpaRepository<AppraisalCycle, java.util.UUID> {
    fun findByOrganizationId(organizationId: java.util.UUID): List<AppraisalCycle>

    fun findByOrganizationIdAndStatus(
        organizationId: java.util.UUID,
        status: AppraisalCycleStatus,
    ): List<AppraisalCycle>
}

@Repository
interface AppraisalRepository : JpaRepository<Appraisal, java.util.UUID> {
    fun findByOrganizationIdAndCycleId(
        organizationId: java.util.UUID,
        cycleId: java.util.UUID,
    ): List<Appraisal>

    fun findByOrganizationIdAndEmployeeId(
        organizationId: java.util.UUID,
        employeeId: java.util.UUID,
    ): List<Appraisal>
}
