package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.CreateLeaveRequestRequest
import com.aquinofroilan.tessera.event.DomainEventPublisher
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
    private lateinit var eventPublisher: DomainEventPublisher
    private lateinit var service: LeaveRequestService

    private val orgId = java.util.UUID.fromString("e5628ca4-87a8-3e6f-8ae2-20213cc7ef92")
    private val empId = java.util.UUID.fromString("535fd4f7-eb3b-30d3-b784-d16e1d946ff4")
    private val typeId = java.util.UUID.fromString("82d745af-a33b-3e13-adff-05141b0d976d")

    @BeforeEach
    fun setup() {
        repository = mock(LeaveRequestRepository::class.java)
        employeeService = mock(EmployeeService::class.java)
        leaveTypeService = mock(LeaveTypeService::class.java)
        eventPublisher = mock(DomainEventPublisher::class.java)
        whenever(repository.save(any<LeaveRequest>())).thenAnswer { it.arguments[0] }
        whenever(employeeService.getEmployee(empId, orgId)).thenReturn(employee())
        whenever(leaveTypeService.getLeaveType(typeId, orgId)).thenReturn(leaveType(20))
        whenever(
            repository.findByOrganizationIdAndEmployeeIdAndLeaveTypeIdAndStatus(any(), any(), any(), any()),
        ).thenReturn(emptyList())
        service = LeaveRequestService(repository, employeeService, leaveTypeService, eventPublisher)
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
        id = java.util.UUID.fromString("bb7f4762-d646-3869-9c36-7f6c06fb377e"),
        employeeId = empId,
        leaveTypeId = typeId,
        startDate = start,
        endDate = start.plusDays((days - 1).toLong()),
        days = days,
        organizationId = orgId,
        requestedBy = java.util.UUID.fromString("d4763ac6-a6a6-34ed-aeb4-dd91bdcf7fbb"),
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
                java.util.UUID.fromString("d4763ac6-a6a6-34ed-aeb4-dd91bdcf7fbb"),
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
                java.util.UUID.fromString("d4763ac6-a6a6-34ed-aeb4-dd91bdcf7fbb"),
            )
        }.isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `approve within entitlement marks APPROVED without touching past-dated employee status`() {
        whenever(repository.findById(java.util.UUID.fromString("bb7f4762-d646-3869-9c36-7f6c06fb377e"))).thenReturn(Optional.of(pending(5)))

        val approved =
            service.approveLeaveRequest(
                java.util.UUID.fromString("bb7f4762-d646-3869-9c36-7f6c06fb377e"),
                orgId,
                java.util.UUID.fromString("339851d6-a2ee-38e3-9908-6d48907f4a92"),
            )

        assertThat(approved.status).isEqualTo(LeaveRequestStatus.APPROVED)
        assertThat(approved.decidedBy).isEqualTo(java.util.UUID.fromString("339851d6-a2ee-38e3-9908-6d48907f4a92"))
        // Past-dated leave does not flip employment status.
        verify(employeeService, never()).placeOnLeave(any(), any())
    }

    @Test
    fun `approve exceeding entitlement is rejected`() {
        whenever(leaveTypeService.getLeaveType(typeId, orgId)).thenReturn(leaveType(3))
        whenever(repository.findById(java.util.UUID.fromString("bb7f4762-d646-3869-9c36-7f6c06fb377e"))).thenReturn(Optional.of(pending(5)))

        assertThatThrownBy {
            service.approveLeaveRequest(
                java.util.UUID.fromString("bb7f4762-d646-3869-9c36-7f6c06fb377e"),
                orgId,
                java.util.UUID.fromString("339851d6-a2ee-38e3-9908-6d48907f4a92"),
            )
        }.isInstanceOf(BusinessRuleException::class.java)
        verify(repository, never()).save(any<LeaveRequest>())
    }

    @Test
    fun `approving leave covering today places the employee on leave`() {
        val today = LocalDate.now(java.time.ZoneOffset.UTC)
        whenever(
            repository.findById(java.util.UUID.fromString("bb7f4762-d646-3869-9c36-7f6c06fb377e")),
        ).thenReturn(Optional.of(pending(1, start = today)))

        service.approveLeaveRequest(
            java.util.UUID.fromString("bb7f4762-d646-3869-9c36-7f6c06fb377e"),
            orgId,
            java.util.UUID.fromString("339851d6-a2ee-38e3-9908-6d48907f4a92"),
        )

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
