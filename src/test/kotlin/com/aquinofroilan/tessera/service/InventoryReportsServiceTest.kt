package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.model.StockMovement
import com.aquinofroilan.tessera.model.StockMovementType
import com.aquinofroilan.tessera.repository.OnHandKey
import com.aquinofroilan.tessera.repository.StockMovementRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import java.math.BigDecimal
import java.time.LocalDateTime

class InventoryReportsServiceTest {
    private lateinit var service: InventoryReportsService
    private lateinit var stockMovementRepository: StockMovementRepository

    private val orgId = "org-123"

    @BeforeEach
    fun setup() {
        stockMovementRepository = mock(StockMovementRepository::class.java)
        service = InventoryReportsService(stockMovementRepository)
    }

    @Test
    fun `stockOnHand without asOfDate delegates to live aggregator`() {
        `when`(stockMovementRepository.onHandByProductWarehouse(orgId)).thenReturn(
            mapOf(
                OnHandKey("p-1", "wh-1") to BigDecimal("10"),
                OnHandKey("p-2", "wh-1") to BigDecimal("0"),
            ),
        )
        val report = service.stockOnHand(orgId, null, null, null)
        assertThat(report.lines).hasSize(1)
        assertThat(report.lines[0].productId).isEqualTo("p-1")
    }

    @Test
    fun `stockOnHand with asOfDate replays movements`() {
        val asOf = LocalDateTime.of(2026, 5, 1, 0, 0)
        `when`(
            stockMovementRepository.listMovements(eq(orgId), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), eq(asOf)),
        ).thenReturn(
            listOf(
                movement(StockMovementType.RECEIPT, BigDecimal("10"), warehouseId = "wh-1", id = "m1"),
                movement(StockMovementType.ISSUE, BigDecimal("3"), warehouseId = "wh-1", id = "m2"),
                movement(
                    StockMovementType.TRANSFER,
                    BigDecimal("2"),
                    warehouseId = "wh-1",
                    transferTo = "wh-2",
                    id = "m3",
                ),
            ),
        )
        val report = service.stockOnHand(orgId, null, null, asOf)
        // wh-1: +10 -3 -2 = 5;  wh-2: +2 (from transfer)
        val byKey = report.lines.associate { (it.productId to it.warehouseId) to it.quantity }
        assertThat(byKey).containsEntry(("p-1" to "wh-1"), BigDecimal("5"))
        assertThat(byKey).containsEntry(("p-1" to "wh-2"), BigDecimal("2"))
    }

    @Test
    fun `stockOnHand applies productId and warehouseId filters`() {
        `when`(stockMovementRepository.onHandByProductWarehouse(orgId)).thenReturn(
            mapOf(
                OnHandKey("p-1", "wh-1") to BigDecimal("10"),
                OnHandKey("p-2", "wh-1") to BigDecimal("5"),
                OnHandKey("p-1", "wh-2") to BigDecimal("7"),
            ),
        )
        val report = service.stockOnHand(orgId, productId = "p-1", warehouseId = null, asOfDate = null)
        assertThat(report.lines.map { it.warehouseId }).containsExactly("wh-1", "wh-2")
    }

    @Test
    fun `movementHistory returns running balance per pair`() {
        `when`(
            stockMovementRepository.listMovements(eq(orgId), eq("p-1"), eq("wh-1"), anyOrNull(), anyOrNull(), anyOrNull()),
        ).thenReturn(
            listOf(
                movement(StockMovementType.RECEIPT, BigDecimal("10"), id = "m1", occurredOffsetSec = 0),
                movement(StockMovementType.ISSUE, BigDecimal("3"), id = "m2", occurredOffsetSec = 10),
                movement(StockMovementType.RECEIPT, BigDecimal("5"), id = "m3", occurredOffsetSec = 20),
            ),
        )
        val history = service.movementHistory(orgId, productId = "p-1", warehouseId = "wh-1", from = null, to = null)
        assertThat(history.lines.map { it.runningBalance.toPlainString() }).containsExactly("10", "7", "12")
    }

    private fun movement(
        type: StockMovementType,
        quantity: BigDecimal,
        warehouseId: String = "wh-1",
        transferTo: String? = null,
        id: String = "mov",
        occurredOffsetSec: Long = 0,
    ) = StockMovement(
        id = id,
        organizationId = orgId,
        type = type,
        productId = "p-1",
        warehouseId = warehouseId,
        transferToWarehouseId = transferTo,
        quantity = quantity,
        unitCost = if (type == StockMovementType.RECEIPT) BigDecimal("1") else null,
        occurredAt = LocalDateTime.now().plusSeconds(occurredOffsetSec),
        createdBy = "user-1",
    )
}
