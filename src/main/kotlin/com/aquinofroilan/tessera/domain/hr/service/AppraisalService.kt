package com.aquinofroilan.tessera.domain.hr.service

import com.aquinofroilan.tessera.domain.hr.dto.CreateAppraisalCycleRequest
import com.aquinofroilan.tessera.domain.hr.dto.CreateAppraisalRequest
import com.aquinofroilan.tessera.domain.hr.dto.SubmitAppraisalReviewRequest
import com.aquinofroilan.tessera.domain.hr.model.Appraisal
import com.aquinofroilan.tessera.domain.hr.model.AppraisalCycle
import com.aquinofroilan.tessera.domain.hr.model.AppraisalCycleStatus
import com.aquinofroilan.tessera.domain.hr.model.AppraisalGoal
import com.aquinofroilan.tessera.domain.hr.model.AppraisalStatus
import com.aquinofroilan.tessera.domain.hr.repository.AppraisalCycleRepository
import com.aquinofroilan.tessera.domain.hr.repository.AppraisalRepository
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class AppraisalService(
    private val appraisalCycleRepository: AppraisalCycleRepository,
    private val appraisalRepository: AppraisalRepository,
    private val employeeService: EmployeeService,
) {
    @Transactional
    fun createCycle(
        request: CreateAppraisalCycleRequest,
        organizationId: UUID,
    ): AppraisalCycle {
        val cycle =
            AppraisalCycle(
                organizationId = organizationId,
                name = request.name ?: throw BusinessRuleException("Name is required"),
                startDate = request.startDate ?: throw BusinessRuleException("Start date is required"),
                endDate = request.endDate ?: throw BusinessRuleException("End date is required"),
            )
        return appraisalCycleRepository.save(cycle)
    }

    @Transactional
    fun activateCycle(
        cycleId: UUID,
        organizationId: UUID,
    ): AppraisalCycle {
        val cycle = getCycle(cycleId, organizationId)
        if (cycle.status != AppraisalCycleStatus.DRAFT) {
            throw BusinessRuleException("Only draft cycles can be activated")
        }
        cycle.status = AppraisalCycleStatus.ACTIVE
        return appraisalCycleRepository.save(cycle)
    }

    @Transactional
    fun closeCycle(
        cycleId: UUID,
        organizationId: UUID,
    ): AppraisalCycle {
        val cycle = getCycle(cycleId, organizationId)
        if (cycle.status != AppraisalCycleStatus.ACTIVE) {
            throw BusinessRuleException("Only active cycles can be closed")
        }
        cycle.status = AppraisalCycleStatus.CLOSED
        return appraisalCycleRepository.save(cycle)
    }

    fun getCycle(
        cycleId: UUID,
        organizationId: UUID,
    ): AppraisalCycle {
        val cycle =
            appraisalCycleRepository.findById(cycleId).orElseThrow {
                ResourceNotFoundException("Appraisal cycle not found")
            }
        if (cycle.organizationId != organizationId) {
            throw ResourceNotFoundException("Appraisal cycle not found")
        }
        return cycle
    }

    @Transactional
    fun createAppraisal(
        cycleId: UUID,
        request: CreateAppraisalRequest,
        organizationId: UUID,
    ): Appraisal {
        val cycle = getCycle(cycleId, organizationId)
        if (cycle.status == AppraisalCycleStatus.CLOSED) {
            throw BusinessRuleException("Cannot create appraisals for closed cycles")
        }

        val employee = employeeService.getEmployee(request.employeeId, organizationId)
        val managerId = request.managerId ?: employee.departmentId // Assuming we could look up the department manager in reality

        val appraisal =
            Appraisal(
                organizationId = organizationId,
                cycleId = cycle.id,
                employeeId = employee.id,
                managerId = managerId,
                status = AppraisalStatus.DRAFT,
            )

        request.goals?.forEach { goalReq ->
            appraisal.goals.add(
                AppraisalGoal(
                    appraisalId = appraisal.id,
                    title = goalReq.title ?: throw BusinessRuleException("Goal title is required"),
                    description = goalReq.description,
                    weight = goalReq.weight,
                ),
            )
        }

        return appraisalRepository.save(appraisal)
    }

    fun getAppraisal(
        appraisalId: UUID,
        organizationId: UUID,
    ): Appraisal {
        val appraisal =
            appraisalRepository.findById(appraisalId).orElseThrow {
                ResourceNotFoundException("Appraisal not found")
            }
        if (appraisal.organizationId != organizationId) {
            throw ResourceNotFoundException("Appraisal not found")
        }
        return appraisal
    }

    @Transactional
    fun submitSelfReview(
        appraisalId: UUID,
        request: SubmitAppraisalReviewRequest,
        organizationId: UUID,
    ): Appraisal {
        val appraisal = getAppraisal(appraisalId, organizationId)
        if (appraisal.status != AppraisalStatus.DRAFT) {
            throw BusinessRuleException("Self review can only be submitted in DRAFT status")
        }

        request.goals?.forEach { goalUpdate ->
            val goal =
                appraisal.goals.find { it.id == goalUpdate.goalId }
                    ?: throw BusinessRuleException("Goal not found in this appraisal")
            goal.selfRating = goalUpdate.selfRating
            goal.selfComments = goalUpdate.selfComments
        }

        appraisal.status = AppraisalStatus.MANAGER_REVIEW
        return appraisalRepository.save(appraisal)
    }

    @Transactional
    fun submitManagerReview(
        appraisalId: UUID,
        request: SubmitAppraisalReviewRequest,
        organizationId: UUID,
    ): Appraisal {
        val appraisal = getAppraisal(appraisalId, organizationId)
        if (appraisal.status != AppraisalStatus.MANAGER_REVIEW) {
            throw BusinessRuleException("Manager review can only be submitted after self review")
        }

        request.goals?.forEach { goalUpdate ->
            val goal =
                appraisal.goals.find { it.id == goalUpdate.goalId }
                    ?: throw BusinessRuleException("Goal not found in this appraisal")
            goal.managerRating = goalUpdate.managerRating
            goal.managerComments = goalUpdate.managerComments
        }

        appraisal.overallRating = request.overallRating
        appraisal.managerSummary = request.managerSummary
        appraisal.status = AppraisalStatus.COMPLETED

        return appraisalRepository.save(appraisal)
    }
}
