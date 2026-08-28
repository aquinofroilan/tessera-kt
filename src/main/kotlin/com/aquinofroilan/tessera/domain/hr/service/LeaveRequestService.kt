package com.aquinofroilan.tessera.domain.hr.service

import com.aquinofroilan.tessera.domain.hr.dto.CreateLeaveRequestRequest
import com.aquinofroilan.tessera.domain.hr.dto.LeaveBalanceResponse
import com.aquinofroilan.tessera.domain.hr.model.EmploymentStatus
import com.aquinofroilan.tessera.domain.hr.model.LeaveRequest
import com.aquinofroilan.tessera.domain.hr.model.LeaveRequestStatus
import com.aquinofroilan.tessera.domain.hr.repository.LeaveRequestRepository
import com.aquinofroilan.tessera.event.DomainEventPublisher
import com.aquinofroilan.tessera.event.LeaveRequestApprovedEvent
import com.aquinofroilan.tessera.event.LeaveRequestRejectedEvent
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

@Service
class LeaveRequestService(
    private val leaveRequestRepository: LeaveRequestRepository,
    private val employeeService: EmployeeService,
    private val leaveTypeService: LeaveTypeService,
    private val domainEventPublisher: DomainEventPublisher,
) {
    @Transactional
    fun createLeaveRequest(
        request: CreateLeaveRequestRequest,
        organizationId: java.util.UUID,
        requestedBy: java.util.UUID,
    ): LeaveRequest {
        val start = request.startDate ?: throw BusinessRuleException("Start date is required")
        val end = request.endDate ?: throw BusinessRuleException("End date is required")
        if (end.isBefore(start)) {
            throw BusinessRuleException("End date must be on or after the start date")
        }
        val employee = employeeService.getEmployee(request.employeeId, organizationId)
        if (employee.status == EmploymentStatus.TERMINATED) {
            throw BusinessRuleException("Cannot request leave for a terminated employee")
        }
        val leaveType = leaveTypeService.getLeaveType(request.leaveTypeId, organizationId)
        if (!leaveType.isActive) {
            throw BusinessRuleException("Leave type '${leaveType.code}' is inactive")
        }

        return leaveRequestRepository.save(
            LeaveRequest(
                employeeId = employee.id,
                leaveTypeId = leaveType.id,
                startDate = start,
                endDate = end,
                days = dayCount(start, end),
                reason = request.reason,
                organizationId = organizationId,
                requestedBy = requestedBy,
            ),
        )
    }

    fun getLeaveRequest(
        id: java.util.UUID,
        organizationId: java.util.UUID,
    ): LeaveRequest {
        val req =
            leaveRequestRepository.findById(id).orElseThrow {
                ResourceNotFoundException("Leave request not found")
            }
        if (req.organizationId != organizationId) {
            throw ResourceNotFoundException("Leave request not found")
        }
        return req
    }

    fun listLeaveRequests(
        organizationId: java.util.UUID,
        employeeId: java.util.UUID? = null,
        status: LeaveRequestStatus? = null,
    ): List<LeaveRequest> =
        when {
            employeeId != null -> leaveRequestRepository.findByOrganizationIdAndEmployeeId(organizationId, employeeId)
            status != null -> leaveRequestRepository.findByOrganizationIdAndStatus(organizationId, status)
            else -> leaveRequestRepository.findByOrganizationId(organizationId)
        }

    @Transactional
    fun approveLeaveRequest(
        id: java.util.UUID,
        organizationId: java.util.UUID,
        decidedBy: java.util.UUID,
    ): LeaveRequest {
        val req = getLeaveRequest(id, organizationId)
        if (req.status != LeaveRequestStatus.PENDING) {
            throw BusinessRuleException("Only pending leave requests can be approved")
        }
        val leaveType = leaveTypeService.getLeaveType(req.leaveTypeId, organizationId)
        val year = req.startDate.year
        val used = usedDays(organizationId, req.employeeId, req.leaveTypeId, year, excludingRequestId = req.id)
        if (used + req.days > leaveType.defaultAnnualDays) {
            throw BusinessRuleException(
                "Insufficient ${leaveType.code} balance: ${leaveType.defaultAnnualDays - used} day(s) remaining, ${req.days} requested",
            )
        }

        req.status = LeaveRequestStatus.APPROVED
        req.decidedBy = decidedBy
        req.decidedAt = LocalDateTime.now(ZoneOffset.UTC)
        val approved = leaveRequestRepository.save(req)

        // If the leave covers today, reflect it on the employee record.
        val today = LocalDate.now(ZoneOffset.UTC)
        if (!today.isBefore(req.startDate) && !today.isAfter(req.endDate)) {
            val employee = employeeService.getEmployee(req.employeeId, organizationId)
            if (employee.status == EmploymentStatus.ACTIVE) {
                employeeService.placeOnLeave(employee.id, organizationId)
            }
        }
        domainEventPublisher.publish(
            LeaveRequestApprovedEvent(
                organizationId = organizationId,
                leaveRequestId = approved.id,
                requesterUserId = approved.requestedBy,
                startDate = approved.startDate,
                endDate = approved.endDate,
                days = approved.days,
            ),
        )
        return approved
    }

    @Transactional
    fun rejectLeaveRequest(
        id: java.util.UUID,
        reason: String?,
        organizationId: java.util.UUID,
        decidedBy: java.util.UUID,
    ): LeaveRequest {
        val req = getLeaveRequest(id, organizationId)
        if (req.status != LeaveRequestStatus.PENDING) {
            throw BusinessRuleException("Only pending leave requests can be rejected")
        }
        req.status = LeaveRequestStatus.REJECTED
        req.decisionReason = reason
        req.decidedBy = decidedBy
        req.decidedAt = LocalDateTime.now(ZoneOffset.UTC)
        val rejected = leaveRequestRepository.save(req)

        domainEventPublisher.publish(
            LeaveRequestRejectedEvent(
                organizationId = organizationId,
                leaveRequestId = rejected.id,
                requesterUserId = rejected.requestedBy,
                reason = reason,
            ),
        )
        return rejected
    }

    @Transactional
    fun cancelLeaveRequest(
        id: java.util.UUID,
        organizationId: java.util.UUID,
    ): LeaveRequest {
        val req = getLeaveRequest(id, organizationId)
        if (req.status != LeaveRequestStatus.PENDING && req.status != LeaveRequestStatus.APPROVED) {
            throw BusinessRuleException("Only pending or approved leave requests can be cancelled")
        }
        req.status = LeaveRequestStatus.CANCELLED
        return leaveRequestRepository.save(req)
    }

    fun balance(
        employeeId: java.util.UUID,
        leaveTypeId: java.util.UUID,
        year: Int,
        organizationId: java.util.UUID,
    ): LeaveBalanceResponse {
        employeeService.getEmployee(employeeId, organizationId)
        val leaveType = leaveTypeService.getLeaveType(leaveTypeId, organizationId)
        val used = usedDays(organizationId, employeeId, leaveTypeId, year, excludingRequestId = null)
        return LeaveBalanceResponse(
            employeeId = employeeId,
            leaveTypeId = leaveTypeId,
            year = year,
            entitlementDays = leaveType.defaultAnnualDays,
            usedDays = used,
            remainingDays = leaveType.defaultAnnualDays - used,
        )
    }

    private fun usedDays(
        organizationId: java.util.UUID,
        employeeId: java.util.UUID,
        leaveTypeId: java.util.UUID,
        year: Int,
        excludingRequestId: java.util.UUID?,
    ): Int =
        leaveRequestRepository
            .findByOrganizationIdAndEmployeeIdAndLeaveTypeIdAndStatus(
                organizationId,
                employeeId,
                leaveTypeId,
                LeaveRequestStatus.APPROVED,
            ).filter { it.id != excludingRequestId && it.startDate.year == year }
            .sumOf { it.days }

    private fun dayCount(
        start: LocalDate,
        end: LocalDate,
    ): Int = (ChronoUnit.DAYS.between(start, end) + 1).toInt()
}
