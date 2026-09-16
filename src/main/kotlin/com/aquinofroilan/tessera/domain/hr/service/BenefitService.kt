package com.aquinofroilan.tessera.domain.hr.service

import com.aquinofroilan.tessera.domain.hr.dto.CreateBenefitPlanRequest
import com.aquinofroilan.tessera.domain.hr.dto.EnrollBenefitRequest
import com.aquinofroilan.tessera.domain.hr.model.BenefitEnrollment
import com.aquinofroilan.tessera.domain.hr.model.BenefitPlan
import com.aquinofroilan.tessera.domain.hr.repository.BenefitEnrollmentRepository
import com.aquinofroilan.tessera.domain.hr.repository.BenefitPlanRepository
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class BenefitService(
    private val benefitPlanRepository: BenefitPlanRepository,
    private val benefitEnrollmentRepository: BenefitEnrollmentRepository,
    private val employeeService: EmployeeService,
) {
    @Transactional
    fun createPlan(
        request: CreateBenefitPlanRequest,
        organizationId: UUID,
    ): BenefitPlan {
        val plan =
            BenefitPlan(
                organizationId = organizationId,
                name = request.name ?: throw BusinessRuleException("Name is required"),
                description = request.description,
                employeeContribution = request.employeeContribution ?: throw BusinessRuleException("Employee contribution is required"),
                employerContribution = request.employerContribution ?: throw BusinessRuleException("Employer contribution is required"),
            )
        return benefitPlanRepository.save(plan)
    }

    fun getPlan(
        planId: UUID,
        organizationId: UUID,
    ): BenefitPlan {
        val plan =
            benefitPlanRepository.findById(planId).orElseThrow {
                ResourceNotFoundException("Benefit plan not found")
            }
        if (plan.organizationId != organizationId) {
            throw ResourceNotFoundException("Benefit plan not found")
        }
        return plan
    }

    @Transactional
    fun enroll(
        request: EnrollBenefitRequest,
        organizationId: UUID,
    ): BenefitEnrollment {
        val employeeId = request.employeeId ?: throw BusinessRuleException("Employee ID is required")
        val planId = request.planId ?: throw BusinessRuleException("Plan ID is required")

        employeeService.getEmployee(employeeId, organizationId)
        getPlan(planId, organizationId)

        // Check if already enrolled
        val existing =
            benefitEnrollmentRepository
                .findByOrganizationIdAndEmployeeIdAndIsActiveTrue(organizationId, employeeId)
                .find { it.planId == planId }

        if (existing != null) {
            throw BusinessRuleException("Employee is already enrolled in this plan")
        }

        val enrollment =
            BenefitEnrollment(
                organizationId = organizationId,
                employeeId = employeeId,
                planId = planId,
            )
        return benefitEnrollmentRepository.save(enrollment)
    }

    fun getActiveEnrollments(
        employeeId: UUID,
        organizationId: UUID,
    ): List<BenefitEnrollment> = benefitEnrollmentRepository.findByOrganizationIdAndEmployeeIdAndIsActiveTrue(organizationId, employeeId)
}
