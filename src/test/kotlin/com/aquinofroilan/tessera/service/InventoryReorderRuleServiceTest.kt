package com.aquinofroilan.tessera.service

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

    private val orgId = "org-123"
    private val otherOrgId = "org-456"

    @BeforeEach
    fun setup() {
        reorderRuleRepository = mock(InventoryReorderRuleRepository::class.java)
        stockMovementRepository = mock(StockMovementRepository::class.java)
        service = InventoryReorderRuleService(reorderRuleRepository, stockMovementRepository)
    }

    private fun rule(
        id: String = "rr-1",
        productId: String = "p-1",
        warehouseId: String = "wh-1",
        reorderPoint: BigDecimal = BigDecimal("10"),
        safetyStock: BigDecimal = BigDecimal("2"),
        organizationId: String = orgId,
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
                productId = "p-1",
                warehouseId = "wh-1",
                reorderPoint = BigDecimal("10"),
                safetyStock = BigDecimal("2"),
            )
        val result = service.createRule(request, orgId)
        assertThat(result.productId).isEqualTo("p-1")
        assertThat(result.reorderPoint).isEqualByComparingTo("10")
        assertThat(result.safetyStock).isEqualByComparingTo("2")
        assertThat(result.organizationId).isEqualTo(orgId)
    }

    @Test
    fun `createRule defaults safetyStock to zero when omitted`() {
        `when`(reorderRuleRepository.save(any<InventoryReorderRule>())).thenAnswer { it.arguments[0] }
        val request =
            CreateReorderRuleRequest(
                productId = "p-1",
                warehouseId = "wh-1",
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
                productId = "p-1",
                warehouseId = "wh-1",
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
        `when`(reorderRuleRepository.findById("rr-1")).thenReturn(Optional.of(rule(organizationId = otherOrgId)))
        assertThrows<ResourceNotFoundException> { service.getRule("rr-1", orgId) }
    }

    @Test
    fun `updateRule applies partial changes`() {
        val existing = rule()
        val updated = existing.copy(reorderPoint = BigDecimal("20"))
        `when`(reorderRuleRepository.findById("rr-1")).thenReturn(Optional.of(existing))
        `when`(reorderRuleRepository.save(any<InventoryReorderRule>())).thenReturn(updated)
        val result =
            service.updateRule("rr-1", UpdateReorderRuleRequest(reorderPoint = BigDecimal("20")), orgId)
        assertThat(result.reorderPoint).isEqualByComparingTo("20")
    }

    @Test
    fun `deleteRule removes rule when org matches`() {
        val existing = rule()
        `when`(reorderRuleRepository.findById("rr-1")).thenReturn(Optional.of(existing))
        service.deleteRule("rr-1", orgId)
        org.mockito.Mockito
            .verify(reorderRuleRepository)
            .delete(existing)
    }

    @Test
    fun `lowStockReport returns only pairs below reorder point`() {
        `when`(reorderRuleRepository.findByOrganizationId(orgId)).thenReturn(
            listOf(
                rule(id = "rr-1", productId = "p-1", warehouseId = "wh-1", reorderPoint = BigDecimal("10")),
                rule(id = "rr-2", productId = "p-2", warehouseId = "wh-1", reorderPoint = BigDecimal("5")),
                rule(id = "rr-3", productId = "p-3", warehouseId = "wh-1", reorderPoint = BigDecimal("3")),
            ),
        )
        `when`(stockMovementRepository.onHandByProductWarehouse(orgId)).thenReturn(
            mapOf(
                OnHandKey("p-1", "wh-1") to BigDecimal("4"), // below
                OnHandKey("p-2", "wh-1") to BigDecimal("8"), // above
                OnHandKey("p-3", "wh-1") to BigDecimal("2"), // below
            ),
        )
        val report = service.lowStockReport(orgId)
        assertThat(report.lines.map { it.productId }).containsExactly("p-1", "p-3")
        assertThat(report.lines.first { it.productId == "p-1" }.shortfall).isEqualByComparingTo("6")
        assertThat(report.lines.first { it.productId == "p-3" }.shortfall).isEqualByComparingTo("1")
    }

    @Test
    fun `lowStockReport treats missing on-hand as zero`() {
        `when`(reorderRuleRepository.findByOrganizationId(orgId)).thenReturn(
            listOf(rule(productId = "p-1", warehouseId = "wh-1", reorderPoint = BigDecimal("5"))),
        )
        `when`(stockMovementRepository.onHandByProductWarehouse(orgId)).thenReturn(emptyMap())
        val report = service.lowStockReport(orgId)
        assertThat(report.lines).hasSize(1)
        assertThat(report.lines[0].onHand).isEqualByComparingTo("0")
        assertThat(report.lines[0].shortfall).isEqualByComparingTo("5")
    }
}
