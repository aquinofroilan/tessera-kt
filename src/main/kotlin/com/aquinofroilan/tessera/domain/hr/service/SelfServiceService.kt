package com.aquinofroilan.tessera.domain.hr.service

import com.aquinofroilan.tessera.domain.hr.dto.CreateLeaveRequestRequest
import com.aquinofroilan.tessera.domain.hr.dto.LeaveBalanceResponse
import com.aquinofroilan.tessera.domain.hr.model.Employee
import com.aquinofroilan.tessera.domain.hr.model.EmployeeCompensation
import com.aquinofroilan.tessera.domain.hr.model.LeaveRequest
import com.aquinofroilan.tessera.domain.platform.dto.SubmitSelfLeaveRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

/**
 * Employee self-service: a self-scoped facade over the HR services. Every call
 * resolves the caller's own employee record from their login user, so an
 * employee can only ever see or act on their own data.
 */
@Service
class SelfServiceService(
    private val employeeService: EmployeeService,
    private val leaveRequestService: LeaveRequestService,
    private val employeeCompensationService: EmployeeCompensationService,
) {
    fun myProfile(
        userId: java.util.UUID,
        organizationId: java.util.UUID,
    ): Employee = employeeService.getEmployeeByUser(userId, organizationId)

    fun myLeaveRequests(
        userId: java.util.UUID,
        organizationId: java.util.UUID,
    ): List<LeaveRequest> {
        val me = employeeService.getEmployeeByUser(userId, organizationId)
        return leaveRequestService.listLeaveRequests(organizationId, employeeId = me.id)
    }

    @Transactional
    fun submitLeave(
        userId: java.util.UUID,
        organizationId: java.util.UUID,
        request: SubmitSelfLeaveRequest,
    ): LeaveRequest {
        val me = employeeService.getEmployeeByUser(userId, organizationId)
        return leaveRequestService.createLeaveRequest(
            CreateLeaveRequestRequest(
                employeeId = me.id,
                leaveTypeId = request.leaveTypeId,
                startDate = request.startDate,
                endDate = request.endDate,
                reason = request.reason,
            ),
            organizationId,
            requestedBy = userId,
        )
    }

    fun myLeaveBalance(
        userId: java.util.UUID,
        organizationId: java.util.UUID,
        leaveTypeId: java.util.UUID,
        year: Int,
    ): LeaveBalanceResponse {
        val me = employeeService.getEmployeeByUser(userId, organizationId)
        return leaveRequestService.balance(me.id, leaveTypeId, year, organizationId)
    }

    fun myCompensationHistory(
        userId: java.util.UUID,
        organizationId: java.util.UUID,
    ): List<EmployeeCompensation> {
        val me = employeeService.getEmployeeByUser(userId, organizationId)
        return employeeCompensationService.listCompensation(me.id, organizationId)
    }

    fun myCurrentCompensation(
        userId: java.util.UUID,
        organizationId: java.util.UUID,
        asOf: LocalDate,
    ): EmployeeCompensation {
        val me = employeeService.getEmployeeByUser(userId, organizationId)
        return employeeCompensationService.currentCompensation(me.id, organizationId, asOf)
    }
}
