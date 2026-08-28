package com.aquinofroilan.tessera.domain.inventory.service

import com.aquinofroilan.tessera.domain.inventory.repository.OnHandKey
import com.aquinofroilan.tessera.domain.inventory.repository.StockMovementRepository
import com.aquinofroilan.tessera.domain.organization.model.InventoryCostingMethod
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal

class InventoryValuationServiceTest {
    private lateinit var service: InventoryValuationService
    private lateinit var stockMovementRepository: StockMovementRepository
    private lateinit var costingService: InventoryCostingService

    private val orgId = java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d")

    @BeforeEach
    fun setup() {
        stockMovementRepository = mock(StockMovementRepository::class.java)
        costingService = mock(InventoryCostingService::class.java)
        service = InventoryValuationService(stockMovementRepository, costingService)
    }

    @Test
    fun `valuation aggregates per-pair quantities and costs`() {
        `when`(costingService.costingMethodFor(orgId)).thenReturn(InventoryCostingMethod.WEIGHTED_AVERAGE)
        `when`(stockMovementRepository.onHandByProductWarehouse(orgId)).thenReturn(
            mapOf(
                OnHandKey(
                    java.util.UUID.fromString("c2cf5eda-4c7a-30a7-9e0b-be843869ca89"),
                    java.util.UUID.fromString("c91d2c12-b2b4-3634-a3bb-d0ff561af4ff"),
                ) to
                    BigDecimal("10"),
                OnHandKey(
                    java.util.UUID.fromString("85439c0c-f7b0-3e68-92c0-6195141662c1"),
                    java.util.UUID.fromString("c91d2c12-b2b4-3634-a3bb-d0ff561af4ff"),
                ) to
                    BigDecimal("5"),
            ),
        )
        `when`(
            costingService.valuationCost(
                orgId,
                java.util.UUID.fromString("c2cf5eda-4c7a-30a7-9e0b-be843869ca89"),
                java.util.UUID.fromString("c91d2c12-b2b4-3634-a3bb-d0ff561af4ff"),
            ),
        ).thenReturn(BigDecimal("100"))
        `when`(
            costingService.valuationCost(
                orgId,
                java.util.UUID.fromString("85439c0c-f7b0-3e68-92c0-6195141662c1"),
                java.util.UUID.fromString("c91d2c12-b2b4-3634-a3bb-d0ff561af4ff"),
            ),
        ).thenReturn(BigDecimal("25"))

        val report = service.valuation(orgId)

        assertThat(report.costingMethod).isEqualTo(InventoryCostingMethod.WEIGHTED_AVERAGE)
        assertThat(report.lines).hasSize(2)
        assertThat(report.totalValue).isEqualByComparingTo("125")
    }

    @Test
    fun `valuation omits zero-quantity pairs`() {
        `when`(costingService.costingMethodFor(orgId)).thenReturn(InventoryCostingMethod.FIFO)
        `when`(stockMovementRepository.onHandByProductWarehouse(orgId)).thenReturn(
            mapOf(
                OnHandKey(
                    java.util.UUID.fromString("c2cf5eda-4c7a-30a7-9e0b-be843869ca89"),
                    java.util.UUID.fromString("c91d2c12-b2b4-3634-a3bb-d0ff561af4ff"),
                ) to
                    BigDecimal("0"),
                OnHandKey(
                    java.util.UUID.fromString("85439c0c-f7b0-3e68-92c0-6195141662c1"),
                    java.util.UUID.fromString("c91d2c12-b2b4-3634-a3bb-d0ff561af4ff"),
                ) to
                    BigDecimal("3"),
            ),
        )
        `when`(
            costingService.valuationCost(
                orgId,
                java.util.UUID.fromString("85439c0c-f7b0-3e68-92c0-6195141662c1"),
                java.util.UUID.fromString("c91d2c12-b2b4-3634-a3bb-d0ff561af4ff"),
            ),
        ).thenReturn(BigDecimal("9"))

        val report = service.valuation(orgId)

        assertThat(report.lines).hasSize(1)
        assertThat(report.lines[0].productId).isEqualTo(java.util.UUID.fromString("85439c0c-f7b0-3e68-92c0-6195141662c1"))
        assertThat(report.totalValue).isEqualByComparingTo("9")
    }

    @Test
    fun `valuation returns empty report for org with no stock`() {
        `when`(costingService.costingMethodFor(orgId)).thenReturn(InventoryCostingMethod.WEIGHTED_AVERAGE)
        `when`(stockMovementRepository.onHandByProductWarehouse(orgId)).thenReturn(emptyMap())

        val report = service.valuation(orgId)

        assertThat(report.lines).isEmpty()
        assertThat(report.totalValue).isEqualByComparingTo("0")
    }
}
