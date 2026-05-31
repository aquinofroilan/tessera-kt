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

    private val orgId = "org-1"
    private val userId = "u1"

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
            id = "e1",
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
        assertThat(service.myProfile(userId, orgId).id).isEqualTo("e1")
    }

    @Test
    fun `my profile surfaces not-found when the user has no employee record`() {
        whenever(employeeService.getEmployeeByUser("u2", orgId))
            .thenThrow(ResourceNotFoundException("No employee record is linked to your account"))

        assertThatThrownBy { service.myProfile("u2", orgId) }
            .isInstanceOf(ResourceNotFoundException::class.java)
    }

    @Test
    fun `submit leave forces the caller's own employee id`() {
        whenever(leaveRequestService.createLeaveRequest(any(), eq(orgId), eq(userId)))
            .thenReturn(mock(LeaveRequest::class.java))

        service.submitLeave(
            userId,
            orgId,
            SubmitSelfLeaveRequest(leaveTypeId = "lt1", startDate = LocalDate.of(2026, 5, 1), endDate = LocalDate.of(2026, 5, 3)),
        )

        val captor = argumentCaptor<CreateLeaveRequestRequest>()
        verify(leaveRequestService).createLeaveRequest(captor.capture(), eq(orgId), eq(userId))
        assertThat(captor.firstValue.employeeId).isEqualTo("e1")
        assertThat(captor.firstValue.leaveTypeId).isEqualTo("lt1")
    }

    @Test
    fun `leave balance delegates with the caller's own employee id`() {
        service.myLeaveBalance(userId, orgId, "lt1", 2026)
        verify(leaveRequestService).balance(eq("e1"), eq("lt1"), eq(2026), eq(orgId))
    }

    @Test
    fun `compensation history delegates with the caller's own employee id`() {
        service.myCompensationHistory(userId, orgId)
        verify(employeeCompensationService).listCompensation(eq("e1"), eq(orgId))
    }
}
