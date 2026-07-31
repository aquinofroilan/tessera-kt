package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.CreateWorkCenterRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.model.Warehouse
import com.aquinofroilan.tessera.model.WorkCenter
import com.aquinofroilan.tessera.repository.WorkCenterRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.util.Optional

class WorkCenterServiceTest {
    private lateinit var repository: WorkCenterRepository
    private lateinit var warehouseService: WarehouseService
    private lateinit var service: WorkCenterService

    private val orgId  = java.util.UUID.fromString("550e8400-e29b-41d4-a716-446655440000")

    @BeforeEach
    fun setup() {
        repository = mock(WorkCenterRepository::class.java)
        warehouseService = mock(WarehouseService::class.java)
        whenever(repository.save(any<WorkCenter>())).thenAnswer { it.arguments[0] }
        whenever(repository.findByOrganizationIdAndCode(any(), any())).thenReturn(Optional.empty())
        whenever(warehouseService.getWarehouse(any(), any())).thenReturn(
            Warehouse(
                id = java.util.UUID.fromString("550e8400-e29b-41d4-a716-446655440009"),
                code = "MAIN",
                name = "Main",
                organizationId = orgId,
                isActive = true,
            ),
        )
        service = WorkCenterService(repository, warehouseService)
    }

    @Test
    fun `create normalises code to upper-case`() {
        val wc =
            service.createWorkCenter(
                CreateWorkCenterRequest(code = " cnc-1 ", name = "CNC #1", capacityPerHour = BigDecimal.TEN),
                orgId,
            )
        assertThat(wc.code).isEqualTo("CNC-1")
        assertThat(wc.capacityPerHour).isEqualByComparingTo(BigDecimal.TEN)
    }

    @Test
    fun `create rejects duplicate code`() {
        whenever(repository.findByOrganizationIdAndCode(orgId, "CNC-1")).thenReturn(
            Optional.of(WorkCenter(code = "CNC-1", name = "CNC #1", organizationId = orgId)),
        )
        assertThatThrownBy {
            service.createWorkCenter(CreateWorkCenterRequest(code = "CNC-1", name = "CNC #1"), orgId)
        }.isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `deactivate rejects double-deactivation`() {
        whenever(repository.findById(java.util.UUID.fromString("00000000-0000-0000-0000-000000000071"))).thenReturn(
            Optional.of(WorkCenter(id = java.util.UUID.fromString("00000000-0000-0000-0000-000000000071"), code = "C", name = "C", organizationId = orgId, isActive = false)),
        )
        assertThatThrownBy { service.deactivateWorkCenter(java.util.UUID.fromString("00000000-0000-0000-0000-000000000071"), orgId) }
            .isInstanceOf(BusinessRuleException::class.java)
    }
}
