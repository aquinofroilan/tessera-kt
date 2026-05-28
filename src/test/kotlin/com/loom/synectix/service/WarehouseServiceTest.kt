package com.loom.synectix.service

import com.loom.synectix.dto.CreateWarehouseRequest
import com.loom.synectix.dto.UpdateWarehouseRequest
import com.loom.synectix.exception.BusinessRuleException
import com.loom.synectix.exception.ResourceNotFoundException
import com.loom.synectix.model.Warehouse
import com.loom.synectix.repository.WarehouseRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.springframework.dao.DuplicateKeyException
import java.util.Optional

class WarehouseServiceTest {
    private lateinit var warehouseService: WarehouseService
    private lateinit var warehouseRepository: WarehouseRepository

    private val orgId = "org-123"
    private val otherOrgId = "org-456"

    @BeforeEach
    fun setup() {
        warehouseRepository = mock(WarehouseRepository::class.java)
        warehouseService = WarehouseService(warehouseRepository)
    }

    private fun createMockWarehouse(
        code: String = "MAIN",
        organizationId: String = orgId,
        isActive: Boolean = true,
        allowNegativeStock: Boolean = false,
    ) = Warehouse(
        id = "wh-123",
        code = code,
        name = "Main Warehouse",
        description = "Primary warehouse",
        addressLine = "123 Main St",
        city = "Springfield",
        country = "US",
        allowNegativeStock = allowNegativeStock,
        organizationId = organizationId,
        isActive = isActive,
    )

    @Test
    fun `createWarehouse should save warehouse with provided fields`() {
        `when`(warehouseRepository.save(any<Warehouse>())).thenAnswer { it.arguments[0] }

        val request =
            CreateWarehouseRequest(
                code = "MAIN",
                name = "Main Warehouse",
            )

        val result = warehouseService.createWarehouse(request, orgId)

        assertThat(result.code).isEqualTo("MAIN")
        assertThat(result.organizationId).isEqualTo(orgId)
        assertThat(result.allowNegativeStock).isFalse()
        assertThat(result.isActive).isTrue()
    }

    @Test
    fun `createWarehouse should honour allowNegativeStock when set`() {
        `when`(warehouseRepository.save(any<Warehouse>())).thenAnswer { it.arguments[0] }

        val request =
            CreateWarehouseRequest(
                code = "OVERFLOW",
                name = "Overflow",
                allowNegativeStock = true,
            )

        val result = warehouseService.createWarehouse(request, orgId)

        assertThat(result.allowNegativeStock).isTrue()
    }

    @Test
    fun `createWarehouse should throw on duplicate code in organization`() {
        `when`(warehouseRepository.save(any<Warehouse>()))
            .thenThrow(DuplicateKeyException("duplicate key error"))

        val request = CreateWarehouseRequest(code = "MAIN", name = "Main Warehouse")

        val exception =
            assertThrows<BusinessRuleException> {
                warehouseService.createWarehouse(request, orgId)
            }
        assertThat(exception.message).contains("Warehouse with code 'MAIN' already exists in this organization")
    }

    @Test
    fun `getWarehouse should return warehouse when org matches`() {
        val warehouse = createMockWarehouse()
        `when`(warehouseRepository.findById("wh-123")).thenReturn(Optional.of(warehouse))

        val result = warehouseService.getWarehouse("wh-123", orgId)

        assertThat(result.id).isEqualTo("wh-123")
        assertThat(result.organizationId).isEqualTo(orgId)
    }

    @Test
    fun `getWarehouse should throw 404 when not found`() {
        `when`(warehouseRepository.findById("nonexistent")).thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> {
            warehouseService.getWarehouse("nonexistent", orgId)
        }
    }

    @Test
    fun `getWarehouse should enforce cross-org isolation`() {
        val warehouse = createMockWarehouse(organizationId = otherOrgId)
        `when`(warehouseRepository.findById("wh-123")).thenReturn(Optional.of(warehouse))

        assertThrows<ResourceNotFoundException> {
            warehouseService.getWarehouse("wh-123", orgId)
        }
    }

    @Test
    fun `listWarehouses defaults to active-only`() {
        val a = createMockWarehouse(code = "MAIN")
        val b = createMockWarehouse(code = "EAST")
        `when`(warehouseRepository.search(orgId, true, null)).thenReturn(listOf(b, a))

        val result = warehouseService.listWarehouses(orgId)

        assertThat(result.map { it.code }).containsExactly("EAST", "MAIN")
    }

    @Test
    fun `listWarehouses passes isActive false through to repository`() {
        val inactive = createMockWarehouse(code = "OLD", isActive = false)
        `when`(warehouseRepository.search(orgId, false, null)).thenReturn(listOf(inactive))

        val result = warehouseService.listWarehouses(orgId, isActive = false)

        assertThat(result).hasSize(1)
        assertThat(result[0].isActive).isFalse()
    }

    @Test
    fun `listWarehouses passes search term through to repository`() {
        val warehouse = createMockWarehouse(code = "MAIN")
        `when`(warehouseRepository.search(orgId, true, "main")).thenReturn(listOf(warehouse))

        val result = warehouseService.listWarehouses(orgId, search = "main")

        assertThat(result).hasSize(1)
        assertThat(result[0].code).isEqualTo("MAIN")
    }

    @Test
    fun `updateWarehouse should perform partial update`() {
        val existing = createMockWarehouse()
        val updated = existing.copy(name = "Renamed Warehouse")
        `when`(warehouseRepository.findById("wh-123")).thenReturn(Optional.of(existing))
        `when`(warehouseRepository.save(any<Warehouse>())).thenReturn(updated)

        val request = UpdateWarehouseRequest(name = "Renamed Warehouse")
        val result = warehouseService.updateWarehouse("wh-123", request, orgId)

        assertThat(result.name).isEqualTo("Renamed Warehouse")
        assertThat(result.code).isEqualTo("MAIN")
    }

    @Test
    fun `updateWarehouse should allow toggling allowNegativeStock`() {
        val existing = createMockWarehouse(allowNegativeStock = false)
        val updated = existing.copy(allowNegativeStock = true)
        `when`(warehouseRepository.findById("wh-123")).thenReturn(Optional.of(existing))
        `when`(warehouseRepository.save(any<Warehouse>())).thenReturn(updated)

        val request = UpdateWarehouseRequest(allowNegativeStock = true)
        val result = warehouseService.updateWarehouse("wh-123", request, orgId)

        assertThat(result.allowNegativeStock).isTrue()
    }

    @Test
    fun `updateWarehouse should throw when warehouse inactive`() {
        val existing = createMockWarehouse(isActive = false)
        `when`(warehouseRepository.findById("wh-123")).thenReturn(Optional.of(existing))

        assertThrows<BusinessRuleException> {
            warehouseService.updateWarehouse("wh-123", UpdateWarehouseRequest(name = "X"), orgId)
        }
    }

    @Test
    fun `deleteWarehouse should set isActive to false (soft delete)`() {
        val existing = createMockWarehouse(isActive = true)
        val deleted = existing.copy(isActive = false)
        `when`(warehouseRepository.findById("wh-123")).thenReturn(Optional.of(existing))
        `when`(warehouseRepository.save(any<Warehouse>())).thenReturn(deleted)

        val result = warehouseService.deleteWarehouse("wh-123", orgId)

        assertThat(result.isActive).isFalse()
    }

    @Test
    fun `deleteWarehouse should throw when already inactive`() {
        val existing = createMockWarehouse(isActive = false)
        `when`(warehouseRepository.findById("wh-123")).thenReturn(Optional.of(existing))

        assertThrows<BusinessRuleException> {
            warehouseService.deleteWarehouse("wh-123", orgId)
        }
    }

    @Test
    fun `deleteWarehouse should enforce cross-org isolation`() {
        val warehouse = createMockWarehouse(organizationId = otherOrgId)
        `when`(warehouseRepository.findById("wh-123")).thenReturn(Optional.of(warehouse))

        assertThrows<ResourceNotFoundException> {
            warehouseService.deleteWarehouse("wh-123", orgId)
        }
    }
}
