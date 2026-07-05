package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.CreateLeaveRequestRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.model.Employee
import com.aquinofroilan.tessera.model.EmploymentStatus
import com.aquinofroilan.tessera.model.LeaveRequest
import com.aquinofroilan.tessera.model.LeaveRequestStatus
import com.aquinofroilan.tessera.model.LeaveType
import com.aquinofroilan.tessera.repository.LeaveRequestRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.util.Optional

class LeaveRequestServiceTest {
    private lateinit var repository: LeaveRequestRepository
    private lateinit var employeeService: EmployeeService
    private lateinit var leaveTypeService: LeaveTypeService
    private lateinit var service: LeaveRequestService

    private val orgId = "org-1"
    private val empId = "e1"
    private val typeId = "lt1"

    @BeforeEach
    fun setup() {
        repository = mock(LeaveRequestRepository::class.java)
        employeeService = mock(EmployeeService::class.java)
        leaveTypeService = mock(LeaveTypeService::class.java)
        whenever(repository.save(any<LeaveRequest>())).thenAnswer { it.arguments[0] }
        whenever(employeeService.getEmployee(empId, orgId)).thenReturn(employee())
        whenever(leaveTypeService.getLeaveType(typeId, orgId)).thenReturn(leaveType(20))
        whenever(
            repository.findByOrganizationIdAndEmployeeIdAndLeaveTypeIdAndStatus(any(), any(), any(), any()),
        ).thenReturn(emptyList())
        service = LeaveRequestService(repository, employeeService, leaveTypeService)
    }

    private fun employee(status: EmploymentStatus = EmploymentStatus.ACTIVE) =
        Employee(
            id = empId,
            employeeNumber = "EMP-0001",
            firstName = "Ada",
            lastName = "Lovelace",
            hireDate = LocalDate.of(2020, 1, 1),
            status = status,
            organizationId = orgId,
        )

    private fun leaveType(annualDays: Int) =
        LeaveType(id = typeId, code = "AL", name = "Annual", defaultAnnualDays = annualDays, organizationId = orgId)

    private fun pending(
        days: Int,
        start: LocalDate = LocalDate.of(2020, 3, 2),
    ) = LeaveRequest(
        id = "lr1",
        employeeId = empId,
        leaveTypeId = typeId,
        startDate = start,
        endDate = start.plusDays((days - 1).toLong()),
        days = days,
        organizationId = orgId,
        requestedBy = "u1",
    )

    @Test
    fun `create computes the inclusive day count and starts PENDING`() {
        val req =
            service.createLeaveRequest(
                CreateLeaveRequestRequest(
                    employeeId = empId,
                    leaveTypeId = typeId,
                    startDate = LocalDate.of(2020, 3, 2),
                    endDate = LocalDate.of(2020, 3, 6),
                ),
                orgId,
                "u1",
            )

        assertThat(req.days).isEqualTo(5)
        assertThat(req.status).isEqualTo(LeaveRequestStatus.PENDING)
    }

    @Test
    fun `create rejects a terminated employee`() {
        whenever(employeeService.getEmployee(empId, orgId)).thenReturn(employee(EmploymentStatus.TERMINATED))

        assertThatThrownBy {
            service.createLeaveRequest(
                CreateLeaveRequestRequest(empId, typeId, LocalDate.of(2020, 3, 2), LocalDate.of(2020, 3, 6)),
                orgId,
                "u1",
            )
        }.isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `approve within entitlement marks APPROVED without touching past-dated employee status`() {
        whenever(repository.findById("lr1")).thenReturn(Optional.of(pending(5)))

        val approved = service.approveLeaveRequest("lr1", orgId, "mgr")

        assertThat(approved.status).isEqualTo(LeaveRequestStatus.APPROVED)
        assertThat(approved.decidedBy).isEqualTo("mgr")
        // Past-dated leave does not flip employment status.
        verify(employeeService, never()).placeOnLeave(any(), any())
    }

    @Test
    fun `approve exceeding entitlement is rejected`() {
        whenever(leaveTypeService.getLeaveType(typeId, orgId)).thenReturn(leaveType(3))
        whenever(repository.findById("lr1")).thenReturn(Optional.of(pending(5)))

        assertThatThrownBy { service.approveLeaveRequest("lr1", orgId, "mgr") }
            .isInstanceOf(BusinessRuleException::class.java)
        verify(repository, never()).save(any<LeaveRequest>())
    }

    @Test
    fun `approving leave covering today places the employee on leave`() {
        val today = LocalDate.now(java.time.ZoneOffset.UTC)
        whenever(repository.findById("lr1")).thenReturn(Optional.of(pending(1, start = today)))

        service.approveLeaveRequest("lr1", orgId, "mgr")

        verify(employeeService).placeOnLeave(eq(empId), eq(orgId))
    }

    @Test
    fun `balance reports entitlement minus approved used days`() {
        whenever(
            repository.findByOrganizationIdAndEmployeeIdAndLeaveTypeIdAndStatus(orgId, empId, typeId, LeaveRequestStatus.APPROVED),
        ).thenReturn(listOf(pending(4).apply { status = LeaveRequestStatus.APPROVED }))

        val balance = service.balance(empId, typeId, 2020, orgId)

        assertThat(balance.entitlementDays).isEqualTo(20)
        assertThat(balance.usedDays).isEqualTo(4)
        assertThat(balance.remainingDays).isEqualTo(16)
    }
}
