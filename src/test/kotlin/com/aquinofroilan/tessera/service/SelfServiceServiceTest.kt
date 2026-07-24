package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.CreateLeaveRequestRequest
import com.aquinofroilan.tessera.dto.SubmitSelfLeaveRequest
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import com.aquinofroilan.tessera.model.Employee
import com.aquinofroilan.tessera.model.EmploymentStatus
import com.aquinofroilan.tessera.model.LeaveRequest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate

class SelfServiceServiceTest {
    private lateinit var employeeService: EmployeeService
    private lateinit var leaveRequestService: LeaveRequestService
    private lateinit var employeeCompensationService: EmployeeCompensationService
    private lateinit var service: SelfServiceService

    private val orgId = java.util.UUID.fromString("e5628ca4-87a8-3e6f-8ae2-20213cc7ef92")
    private val userId = java.util.UUID.fromString("d4763ac6-a6a6-34ed-aeb4-dd91bdcf7fbb")

    @BeforeEach
    fun setup() {
        employeeService = mock(EmployeeService::class.java)
        leaveRequestService = mock(LeaveRequestService::class.java)
        employeeCompensationService = mock(EmployeeCompensationService::class.java)
        whenever(employeeService.getEmployeeByUser(userId, orgId)).thenReturn(me())
        service = SelfServiceService(employeeService, leaveRequestService, employeeCompensationService)
    }

    private fun me() =
        Employee(
            id = java.util.UUID.fromString("535fd4f7-eb3b-30d3-b784-d16e1d946ff4"),
            employeeNumber = "EMP-0001",
            firstName = "Ada",
            lastName = "Lovelace",
            hireDate = LocalDate.of(2020, 1, 1),
            status = EmploymentStatus.ACTIVE,
            userId = userId,
            organizationId = orgId,
        )

    @Test
    fun `my profile resolves the linked employee`() {
        assertThat(service.myProfile(userId, orgId).id).isEqualTo(java.util.UUID.fromString("535fd4f7-eb3b-30d3-b784-d16e1d946ff4"))
    }

    @Test
    fun `my profile surfaces not-found when the user has no employee record`() {
        whenever(employeeService.getEmployeeByUser(java.util.UUID.fromString("731b3177-ae56-32b2-9b3b-f13f4cde1ee2"), orgId))
            .thenThrow(ResourceNotFoundException("No employee record is linked to your account"))

        assertThatThrownBy { service.myProfile(java.util.UUID.fromString("731b3177-ae56-32b2-9b3b-f13f4cde1ee2"), orgId) }
            .isInstanceOf(ResourceNotFoundException::class.java)
    }

    @Test
    fun `submit leave forces the caller's own employee id`() {
        whenever(leaveRequestService.createLeaveRequest(any(), eq(orgId), eq(userId)))
            .thenReturn(mock(LeaveRequest::class.java))

        service.submitLeave(
            userId,
            orgId,
            SubmitSelfLeaveRequest(
                leaveTypeId = java.util.UUID.fromString("82d745af-a33b-3e13-adff-05141b0d976d"),
                startDate = LocalDate.of(2026, 5, 1),
                endDate = LocalDate.of(2026, 5, 3),
            ),
        )

        val captor = argumentCaptor<CreateLeaveRequestRequest>()
        verify(leaveRequestService).createLeaveRequest(captor.capture(), eq(orgId), eq(userId))
        assertThat(captor.firstValue.employeeId).isEqualTo(java.util.UUID.fromString("535fd4f7-eb3b-30d3-b784-d16e1d946ff4"))
        assertThat(captor.firstValue.leaveTypeId).isEqualTo(java.util.UUID.fromString("82d745af-a33b-3e13-adff-05141b0d976d"))
    }

    @Test
    fun `leave balance delegates with the caller's own employee id`() {
        service.myLeaveBalance(userId, orgId, java.util.UUID.fromString("82d745af-a33b-3e13-adff-05141b0d976d"), 2026)
        verify(
            leaveRequestService,
        ).balance(
            eq(java.util.UUID.fromString("535fd4f7-eb3b-30d3-b784-d16e1d946ff4")),
            eq(java.util.UUID.fromString("82d745af-a33b-3e13-adff-05141b0d976d")),
            eq(2026),
            eq(orgId),
        )
    }

    @Test
    fun `compensation history delegates with the caller's own employee id`() {
        service.myCompensationHistory(userId, orgId)
        verify(
            employeeCompensationService,
        ).listCompensation(eq(java.util.UUID.fromString("535fd4f7-eb3b-30d3-b784-d16e1d946ff4")), eq(orgId))
    }
}
