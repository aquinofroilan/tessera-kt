package com.aquinofroilan.tessera.service

import java.util.UUID

import com.aquinofroilan.tessera.dto.CreateReorderRuleRequest
import com.aquinofroilan.tessera.dto.UpdateReorderRuleRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import com.aquinofroilan.tessera.model.InventoryReorderRule
import com.aquinofroilan.tessera.repository.InventoryReorderRuleRepository
import com.aquinofroilan.tessera.repository.OnHandKey
import com.aquinofroilan.tessera.repository.StockMovementRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.springframework.dao.DuplicateKeyException
import java.math.BigDecimal
import java.util.Optional

class InventoryReorderRuleServiceTest {
    private lateinit var service: InventoryReorderRuleService
    private lateinit var reorderRuleRepository: InventoryReorderRuleRepository
    private lateinit var stockMovementRepository: StockMovementRepository

    private val orgId = java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d")
    private val otherOrgId = java.util.UUID.fromString("8576b8f7-dd04-3e57-b849-081b3776f223")

    @BeforeEach
    fun setup() {
        reorderRuleRepository = mock(InventoryReorderRuleRepository::class.java)
        stockMovementRepository = mock(StockMovementRepository::class.java)
        service = InventoryReorderRuleService(reorderRuleRepository, stockMovementRepository)
    }

    private fun rule(
        id: UUID = java.util.UUID.fromString("b6a30083-eb9b-37f7-b5f3-39643f6fbc62"),
        productId: UUID = java.util.UUID.fromString("c2cf5eda-4c7a-30a7-9e0b-be843869ca89"),
        warehouseId: UUID = java.util.UUID.fromString("c91d2c12-b2b4-3634-a3bb-d0ff561af4ff"),
        reorderPoint: BigDecimal = BigDecimal("10"),
        safetyStock: BigDecimal = BigDecimal("2"),
        organizationId: java.util.UUID = orgId,
    ) = InventoryReorderRule(
        id = id,
        organizationId = organizationId,
        productId = productId,
        warehouseId = warehouseId,
        reorderPoint = reorderPoint,
        safetyStock = safetyStock,
    )

    @Test
    fun `createRule saves rule with provided fields`() {
        `when`(reorderRuleRepository.save(any<InventoryReorderRule>())).thenAnswer { it.arguments[0] }
        val request =
            CreateReorderRuleRequest(
                productId = java.util.UUID.fromString("c2cf5eda-4c7a-30a7-9e0b-be843869ca89"),
                warehouseId = java.util.UUID.fromString("c91d2c12-b2b4-3634-a3bb-d0ff561af4ff"),
                reorderPoint = BigDecimal("10"),
                safetyStock = BigDecimal("2"),
            )
        val result = service.createRule(request, orgId)
        assertThat(result.productId).isEqualTo(java.util.UUID.fromString("c2cf5eda-4c7a-30a7-9e0b-be843869ca89"))
        assertThat(result.reorderPoint).isEqualByComparingTo("10")
        assertThat(result.safetyStock).isEqualByComparingTo("2")
        assertThat(result.organizationId).isEqualTo(orgId)
    }

    @Test
    fun `createRule defaults safetyStock to zero when omitted`() {
        `when`(reorderRuleRepository.save(any<InventoryReorderRule>())).thenAnswer { it.arguments[0] }
        val request =
            CreateReorderRuleRequest(
                productId = java.util.UUID.fromString("c2cf5eda-4c7a-30a7-9e0b-be843869ca89"),
                warehouseId = java.util.UUID.fromString("c91d2c12-b2b4-3634-a3bb-d0ff561af4ff"),
                reorderPoint = BigDecimal("10"),
                safetyStock = null,
            )
        val result = service.createRule(request, orgId)
        assertThat(result.safetyStock).isEqualByComparingTo("0")
    }

    @Test
    fun `createRule throws on duplicate (orgId, productId, warehouseId)`() {
        `when`(reorderRuleRepository.save(any<InventoryReorderRule>()))
            .thenThrow(DuplicateKeyException("dup"))
        val request =
            CreateReorderRuleRequest(
                productId = java.util.UUID.fromString("c2cf5eda-4c7a-30a7-9e0b-be843869ca89"),
                warehouseId = java.util.UUID.fromString("c91d2c12-b2b4-3634-a3bb-d0ff561af4ff"),
                reorderPoint = BigDecimal("10"),
            )
        val ex =
            assertThrows<BusinessRuleException> {
                service.createRule(request, orgId)
            }
        assertThat(ex.message).contains("already exists")
    }

    @Test
    fun `getRule enforces cross-org isolation`() {
        `when`(reorderRuleRepository.findById(java.util.UUID.fromString("b6a30083-eb9b-37f7-b5f3-39643f6fbc62"))).thenReturn(Optional.of(rule(organizationId = otherOrgId)))
        assertThrows<ResourceNotFoundException> { service.getRule(java.util.UUID.fromString("b6a30083-eb9b-37f7-b5f3-39643f6fbc62"), orgId) }
    }

    @Test
    fun `updateRule applies partial changes`() {
        val existing = rule()
        val updated = existing.apply { reorderPoint = BigDecimal("20") }
        `when`(reorderRuleRepository.findById(java.util.UUID.fromString("b6a30083-eb9b-37f7-b5f3-39643f6fbc62"))).thenReturn(Optional.of(existing))
        `when`(reorderRuleRepository.save(any<InventoryReorderRule>())).thenReturn(updated)
        val result =
            service.updateRule(java.util.UUID.fromString("b6a30083-eb9b-37f7-b5f3-39643f6fbc62"), UpdateReorderRuleRequest(reorderPoint = BigDecimal("20")), orgId)
        assertThat(result.reorderPoint).isEqualByComparingTo("20")
    }

    @Test
    fun `deleteRule removes rule when org matches`() {
        val existing = rule()
        `when`(reorderRuleRepository.findById(java.util.UUID.fromString("b6a30083-eb9b-37f7-b5f3-39643f6fbc62"))).thenReturn(Optional.of(existing))
        service.deleteRule(java.util.UUID.fromString("b6a30083-eb9b-37f7-b5f3-39643f6fbc62"), orgId)
        org.mockito.Mockito
            .verify(reorderRuleRepository)
            .delete(existing)
    }

    @Test
    fun `lowStockReport returns only pairs below reorder point`() {
        `when`(reorderRuleRepository.findByOrganizationId(orgId)).thenReturn(
            listOf(
                rule(id = java.util.UUID.fromString("b6a30083-eb9b-37f7-b5f3-39643f6fbc62"), productId = java.util.UUID.fromString("c2cf5eda-4c7a-30a7-9e0b-be843869ca89")),
                rule(id = java.util.UUID.fromString("a6458940-ef5f-365c-b738-9572bb4253e0"), productId = java.util.UUID.fromString("85439c0c-f7b0-3e68-92c0-6195141662c1")),
                rule(id = java.util.UUID.fromString("f26b40bb-3a45-34b7-9fb4-74e8b6576e9f"), productId = java.util.UUID.fromString("a4bd2b8b-0864-3a85-b209-0cabb1f6f16a")),
            ),
        )
        `when`(stockMovementRepository.onHandByProductWarehouse(orgId)).thenReturn(
            mapOf(
                OnHandKey(java.util.UUID.fromString("c2cf5eda-4c7a-30a7-9e0b-be843869ca89"), java.util.UUID.fromString("c91d2c12-b2b4-3634-a3bb-d0ff561af4ff")) to BigDecimal("4"), // below
                OnHandKey(java.util.UUID.fromString("85439c0c-f7b0-3e68-92c0-6195141662c1"), java.util.UUID.fromString("c91d2c12-b2b4-3634-a3bb-d0ff561af4ff")) to BigDecimal("12"), // above
                OnHandKey(java.util.UUID.fromString("a4bd2b8b-0864-3a85-b209-0cabb1f6f16a"), java.util.UUID.fromString("c91d2c12-b2b4-3634-a3bb-d0ff561af4ff")) to BigDecimal("2"), // below
            ),
        )
        val report = service.lowStockReport(orgId)
        assertThat(report.lines.map { it.productId }).containsExactlyInAnyOrder(java.util.UUID.fromString("c2cf5eda-4c7a-30a7-9e0b-be843869ca89"), java.util.UUID.fromString("a4bd2b8b-0864-3a85-b209-0cabb1f6f16a"))
        assertThat(report.lines.first { it.productId == java.util.UUID.fromString("c2cf5eda-4c7a-30a7-9e0b-be843869ca89") }.shortfall).isEqualByComparingTo("6")
        assertThat(report.lines.first { it.productId == java.util.UUID.fromString("a4bd2b8b-0864-3a85-b209-0cabb1f6f16a") }.shortfall).isEqualByComparingTo("8")
    }

    @Test
    fun `lowStockReport treats missing on-hand as zero`() {
        `when`(reorderRuleRepository.findByOrganizationId(orgId)).thenReturn(
            listOf(rule(productId = java.util.UUID.fromString("c2cf5eda-4c7a-30a7-9e0b-be843869ca89"), warehouseId = java.util.UUID.fromString("c91d2c12-b2b4-3634-a3bb-d0ff561af4ff"), reorderPoint = BigDecimal("5"))),
        )
        `when`(stockMovementRepository.onHandByProductWarehouse(orgId)).thenReturn(emptyMap())
        val report = service.lowStockReport(orgId)
        assertThat(report.lines).hasSize(1)
        assertThat(report.lines[0].onHand).isEqualByComparingTo("0")
        assertThat(report.lines[0].shortfall).isEqualByComparingTo("5")
    }
}
