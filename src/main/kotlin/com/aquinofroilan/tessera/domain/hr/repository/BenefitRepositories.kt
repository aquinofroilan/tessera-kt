package com.aquinofroilan.tessera.domain.hr.repository

import com.aquinofroilan.tessera.domain.hr.model.BenefitEnrollment
import com.aquinofroilan.tessera.domain.hr.model.BenefitPlan
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface BenefitPlanRepository : JpaRepository<BenefitPlan, java.util.UUID> {
    fun findByOrganizationId(organizationId: java.util.UUID): List<BenefitPlan>
}

@Repository
interface BenefitEnrollmentRepository : JpaRepository<BenefitEnrollment, java.util.UUID> {
    fun findByOrganizationIdAndEmployeeIdAndIsActiveTrue(
        organizationId: java.util.UUID,
        employeeId: java.util.UUID,
    ): List<BenefitEnrollment>
}
