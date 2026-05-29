package com.loom.synectix.service

import com.loom.synectix.dto.CreateLeaveTypeRequest
import com.loom.synectix.exception.BusinessRuleException
import com.loom.synectix.model.LeaveType
import com.loom.synectix.repository.LeaveTypeRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.util.Optional

class LeaveTypeServiceTest {
    private lateinit var repository: LeaveTypeRepository
    private lateinit var service: LeaveTypeService

    private val orgId = "org-1"

    @BeforeEach
    fun setup() {
        repository = mock(LeaveTypeRepository::class.java)
        whenever(repository.save(any<LeaveType>())).thenAnswer { it.arguments[0] }
        whenever(repository.findByOrganizationIdAndCode(any(), any())).thenReturn(Optional.empty())
        service = LeaveTypeService(repository)
    }

    @Test
    fun `create persists a leave type`() {
        val lt =
            service.createLeaveType(
                CreateLeaveTypeRequest(code = " AL ", name = " Annual Leave ", paid = true, defaultAnnualDays = 20),
                orgId,
            )

        assertThat(lt.code).isEqualTo("AL")
        assertThat(lt.name).isEqualTo("Annual Leave")
        assertThat(lt.defaultAnnualDays).isEqualTo(20)
        assertThat(lt.isActive).isTrue()
    }

    @Test
    fun `create rejects a duplicate code`() {
        whenever(repository.findByOrganizationIdAndCode(orgId, "AL"))
            .thenReturn(Optional.of(LeaveType(code = "AL", name = "Annual", organizationId = orgId)))

        assertThatThrownBy { service.createLeaveType(CreateLeaveTypeRequest(code = "AL", name = "Annual"), orgId) }
            .isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `deactivate rejects double-deactivation`() {
        whenever(repository.findById("lt1"))
            .thenReturn(Optional.of(LeaveType(id = "lt1", code = "AL", name = "Annual", organizationId = orgId, isActive = false)))

        assertThatThrownBy { service.deactivateLeaveType("lt1", orgId) }
            .isInstanceOf(BusinessRuleException::class.java)
    }
}
