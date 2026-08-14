package com.aquinofroilan.tessera.domain.hr.service

import com.aquinofroilan.tessera.domain.hr.dto.CreateLeaveTypeRequest
import com.aquinofroilan.tessera.domain.hr.model.LeaveType
import com.aquinofroilan.tessera.domain.hr.repository.LeaveTypeRepository
import com.aquinofroilan.tessera.exception.BusinessRuleException
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

    private val orgId = java.util.UUID.fromString("e5628ca4-87a8-3e6f-8ae2-20213cc7ef92")

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
        whenever(repository.findById(java.util.UUID.fromString("82d745af-a33b-3e13-adff-05141b0d976d")))
            .thenReturn(
                Optional.of(
                    LeaveType(
                        id = java.util.UUID.fromString("82d745af-a33b-3e13-adff-05141b0d976d"),
                        code = "AL",
                        name = "Annual",
                        organizationId = orgId,
                        isActive = false,
                    ),
                ),
            )

        assertThatThrownBy { service.deactivateLeaveType(java.util.UUID.fromString("82d745af-a33b-3e13-adff-05141b0d976d"), orgId) }
            .isInstanceOf(BusinessRuleException::class.java)
    }
}
