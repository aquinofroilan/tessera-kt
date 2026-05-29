package com.loom.synectix.service

import com.loom.synectix.dto.CreateLeaveRequestRequest
import com.loom.synectix.dto.LeaveBalanceResponse
import com.loom.synectix.exception.BusinessRuleException
import com.loom.synectix.exception.ResourceNotFoundException
import com.loom.synectix.model.EmploymentStatus
import com.loom.synectix.model.LeaveRequest
import com.loom.synectix.model.LeaveRequestStatus
import com.loom.synectix.repository.LeaveRequestRepository
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
) {
    @Transactional
    fun createLeaveRequest(
        request: CreateLeaveRequestRequest,
        organizationId: String,
        requestedBy: String,
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
        id: String,
        organizationId: String,
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
        organizationId: String,
        employeeId: String? = null,
        status: LeaveRequestStatus? = null,
    ): List<LeaveRequest> =
        when {
            employeeId != null -> leaveRequestRepository.findByOrganizationIdAndEmployeeId(organizationId, employeeId)
            status != null -> leaveRequestRepository.findByOrganizationIdAndStatus(organizationId, status)
            else -> leaveRequestRepository.findByOrganizationId(organizationId)
        }

    @Transactional
    fun approveLeaveRequest(
        id: String,
        organizationId: String,
        decidedBy: String,
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

        val approved =
            leaveRequestRepository.save(
                req.copy(
                    status = LeaveRequestStatus.APPROVED,
                    decidedBy = decidedBy,
                    decidedAt = LocalDateTime.now(ZoneOffset.UTC),
                ),
            )

        // If the leave covers today, reflect it on the employee record.
        val today = LocalDate.now(ZoneOffset.UTC)
        if (!today.isBefore(req.startDate) && !today.isAfter(req.endDate)) {
            val employee = employeeService.getEmployee(req.employeeId, organizationId)
            if (employee.status == EmploymentStatus.ACTIVE) {
                employeeService.placeOnLeave(employee.id, organizationId)
            }
        }
        return approved
    }

    @Transactional
    fun rejectLeaveRequest(
        id: String,
        reason: String?,
        organizationId: String,
        decidedBy: String,
    ): LeaveRequest {
        val req = getLeaveRequest(id, organizationId)
        if (req.status != LeaveRequestStatus.PENDING) {
            throw BusinessRuleException("Only pending leave requests can be rejected")
        }
        return leaveRequestRepository.save(
            req.copy(
                status = LeaveRequestStatus.REJECTED,
                decisionReason = reason,
                decidedBy = decidedBy,
                decidedAt = LocalDateTime.now(ZoneOffset.UTC),
            ),
        )
    }

    @Transactional
    fun cancelLeaveRequest(
        id: String,
        organizationId: String,
    ): LeaveRequest {
        val req = getLeaveRequest(id, organizationId)
        if (req.status != LeaveRequestStatus.PENDING && req.status != LeaveRequestStatus.APPROVED) {
            throw BusinessRuleException("Only pending or approved leave requests can be cancelled")
        }
        return leaveRequestRepository.save(req.copy(status = LeaveRequestStatus.CANCELLED))
    }

    fun balance(
        employeeId: String,
        leaveTypeId: String,
        year: Int,
        organizationId: String,
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
        organizationId: String,
        employeeId: String,
        leaveTypeId: String,
        year: Int,
        excludingRequestId: String?,
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
