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
import java.util.UUID

class InventoryReportsServiceTest {
    private lateinit var service: InventoryReportsService
    private lateinit var stockMovementRepository: StockMovementRepository

    private val orgId = java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d")

    @BeforeEach
    fun setup() {
        stockMovementRepository = mock(StockMovementRepository::class.java)
        service = InventoryReportsService(stockMovementRepository)
    }

    @Test
    fun `stockOnHand without asOfDate delegates to live aggregator`() {
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
                    BigDecimal("0"),
            ),
        )
        val report = service.stockOnHand(orgId, null, null, null)
        assertThat(report.lines).hasSize(1)
        assertThat(report.lines[0].productId).isEqualTo(java.util.UUID.fromString("c2cf5eda-4c7a-30a7-9e0b-be843869ca89"))
    }

    @Test
    fun `stockOnHand with asOfDate replays movements`() {
        val asOf = LocalDateTime.of(2026, 5, 1, 0, 0)
        `when`(
            stockMovementRepository.listMovements(eq(orgId), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), eq(asOf)),
        ).thenReturn(
            listOf(
                movement(
                    StockMovementType.RECEIPT,
                    BigDecimal("10"),
                    id = java.util.UUID.fromString("9e0b57e6-ae56-3955-bb0a-78763cf4171e"),
                ),
                movement(StockMovementType.ISSUE, BigDecimal("3"), id = java.util.UUID.fromString("8d955049-1fdf-3f79-9590-0a50fa1e16cb")),
                movement(
                    StockMovementType.TRANSFER,
                    BigDecimal("2"),
                    warehouseId = java.util.UUID.fromString("c91d2c12-b2b4-3634-a3bb-d0ff561af4ff"),
                    transferTo = java.util.UUID.fromString("1d0e273c-e8f6-3c81-aa83-af17bdd332f5"),
                    id = java.util.UUID.fromString("c576d44c-0ef7-311d-9658-92317a6a2c03"),
                ),
            ),
        )
        val report = service.stockOnHand(orgId, null, null, asOf)
        // wh-1: +10 -3 -2 = 5;  wh-2: +2 (from transfer)
        val byKey = report.lines.associate { (it.productId to it.warehouseId) to it.quantity }
        assertThat(byKey).containsEntry(
            (
                java.util.UUID.fromString("c2cf5eda-4c7a-30a7-9e0b-be843869ca89") to
                    java.util.UUID.fromString("c91d2c12-b2b4-3634-a3bb-d0ff561af4ff")
            ),
            BigDecimal("5"),
        )
        assertThat(byKey).containsEntry(
            (
                java.util.UUID.fromString("c2cf5eda-4c7a-30a7-9e0b-be843869ca89") to
                    java.util.UUID.fromString("1d0e273c-e8f6-3c81-aa83-af17bdd332f5")
            ),
            BigDecimal("2"),
        )
    }

    @Test
    fun `stockOnHand applies productId and warehouseId filters`() {
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
                OnHandKey(
                    java.util.UUID.fromString("c2cf5eda-4c7a-30a7-9e0b-be843869ca89"),
                    java.util.UUID.fromString("1d0e273c-e8f6-3c81-aa83-af17bdd332f5"),
                ) to
                    BigDecimal("7"),
            ),
        )
        val report =
            service.stockOnHand(
                orgId,
                productId = java.util.UUID.fromString("c2cf5eda-4c7a-30a7-9e0b-be843869ca89"),
                warehouseId = null,
                asOfDate = null,
            )
        assertThat(
            report.lines.map {
                it.warehouseId
            },
        ).containsExactlyInAnyOrder(
            java.util.UUID.fromString("c91d2c12-b2b4-3634-a3bb-d0ff561af4ff"),
            java.util.UUID.fromString("1d0e273c-e8f6-3c81-aa83-af17bdd332f5"),
        )
    }

    @Test
    fun `movementHistory returns running balance per pair`() {
        `when`(
            stockMovementRepository.listMovements(
                eq(orgId),
                eq(java.util.UUID.fromString("c2cf5eda-4c7a-30a7-9e0b-be843869ca89")),
                eq(java.util.UUID.fromString("c91d2c12-b2b4-3634-a3bb-d0ff561af4ff")),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
            ),
        ).thenReturn(
            listOf(
                movement(
                    StockMovementType.RECEIPT,
                    BigDecimal("10"),
                    id = java.util.UUID.fromString("9e0b57e6-ae56-3955-bb0a-78763cf4171e"),
                    occurredOffsetSec = 0,
                ),
                movement(
                    StockMovementType.ISSUE,
                    BigDecimal("3"),
                    id = java.util.UUID.fromString("8d955049-1fdf-3f79-9590-0a50fa1e16cb"),
                    occurredOffsetSec = 10,
                ),
                movement(
                    StockMovementType.RECEIPT,
                    BigDecimal("5"),
                    id = java.util.UUID.fromString("c576d44c-0ef7-311d-9658-92317a6a2c03"),
                    occurredOffsetSec = 20,
                ),
            ),
        )
        val history =
            service.movementHistory(
                orgId,
                productId = java.util.UUID.fromString("c2cf5eda-4c7a-30a7-9e0b-be843869ca89"),
                warehouseId = java.util.UUID.fromString("c91d2c12-b2b4-3634-a3bb-d0ff561af4ff"),
                from = null,
                to = null,
            )
        assertThat(history.lines.map { it.runningBalance.toPlainString() }).containsExactly("10", "7", "12")
    }

    private fun movement(
        type: StockMovementType,
        quantity: BigDecimal,
        warehouseId: UUID = java.util.UUID.fromString("c91d2c12-b2b4-3634-a3bb-d0ff561af4ff"),
        transferTo: java.util.UUID? = null,
        id: java.util.UUID = java.util.UUID.randomUUID(),
        occurredOffsetSec: Long = 0,
    ) = StockMovement(
        id = id,
        organizationId = orgId,
        type = type,
        productId = java.util.UUID.fromString("c2cf5eda-4c7a-30a7-9e0b-be843869ca89"),
        warehouseId = warehouseId,
        transferToWarehouseId = transferTo,
        quantity = quantity,
        unitCost = if (type == StockMovementType.RECEIPT) BigDecimal("1") else null,
        occurredAt = LocalDateTime.now().plusSeconds(occurredOffsetSec),
        createdBy = java.util.UUID.fromString("1db2395f-13ba-3d37-9d2b-f77d3eb3aa2e"),
    )
}
