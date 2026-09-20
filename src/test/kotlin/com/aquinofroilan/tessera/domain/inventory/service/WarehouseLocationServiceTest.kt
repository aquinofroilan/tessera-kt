package com.aquinofroilan.tessera.domain.inventory.service

import com.aquinofroilan.tessera.domain.inventory.dto.CreateWarehouseLocationRequest
import com.aquinofroilan.tessera.domain.inventory.model.LocationType
import com.aquinofroilan.tessera.domain.inventory.model.Warehouse
import com.aquinofroilan.tessera.domain.inventory.model.WarehouseLocation
import com.aquinofroilan.tessera.domain.inventory.repository.WarehouseLocationRepository
import com.aquinofroilan.tessera.domain.inventory.repository.WarehouseRepository
import com.aquinofroilan.tessera.exception.BusinessRuleException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.Optional
import java.util.UUID

class WarehouseLocationServiceTest {

    private lateinit var warehouseRepository: WarehouseRepository
    private lateinit var warehouseLocationRepository: WarehouseLocationRepository
    private lateinit var service: WarehouseLocationService

    private val orgId = UUID.randomUUID()
    private val warehouseId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        warehouseRepository = mock()
        warehouseLocationRepository = mock()
        service = WarehouseLocationService(warehouseLocationRepository, warehouseRepository)

        val warehouse = Warehouse(
            id = warehouseId,
            organizationId = orgId,
            code = "WH1",
            name = "Warehouse 1"
        )
        whenever(warehouseRepository.findById(warehouseId)).thenReturn(Optional.of(warehouse))
    }

    @Test
    fun `createLocation succeeds`() {
        whenever(warehouseLocationRepository.existsByOrganizationIdAndWarehouseIdAndCode(orgId, warehouseId, "A1")).thenReturn(false)
        whenever(warehouseLocationRepository.save(any())).thenAnswer { it.arguments[0] }

        val request = CreateWarehouseLocationRequest(
            warehouseId = warehouseId,
            code = "A1",
            type = LocationType.BIN
        )

        val result = service.createLocation(request, orgId)
        assertThat(result.code).isEqualTo("A1")
        assertThat(result.type).isEqualTo(LocationType.BIN)
    }

    @Test
    fun `createLocation fails if code exists`() {
        whenever(warehouseLocationRepository.existsByOrganizationIdAndWarehouseIdAndCode(orgId, warehouseId, "A1")).thenReturn(true)

        val request = CreateWarehouseLocationRequest(
            warehouseId = warehouseId,
            code = "A1",
            type = LocationType.BIN
        )

        assertThrows<BusinessRuleException> {
            service.createLocation(request, orgId)
        }
    }
}
