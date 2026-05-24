package com.froilan.synectix.service

import com.froilan.synectix.model.InventoryCostLayer
import com.froilan.synectix.model.InventoryCostingMethod
import com.froilan.synectix.model.InventoryWaSnapshot
import com.froilan.synectix.model.Organizations
import com.froilan.synectix.model.StockMovement
import com.froilan.synectix.model.StockMovementType
import com.froilan.synectix.repository.InventoryCostLayerRepository
import com.froilan.synectix.repository.InventoryWaSnapshotRepository
import com.froilan.synectix.repository.OrganizationRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.Optional

class InventoryCostingServiceTest {
    private lateinit var service: InventoryCostingService
    private lateinit var layerRepository: InventoryCostLayerRepository
    private lateinit var waRepository: InventoryWaSnapshotRepository
    private lateinit var organizationRepository: OrganizationRepository

    private val orgId = "org-123"
    private val productId = "prod-1"
    private val warehouseId = "wh-1"
    private val otherWarehouseId = "wh-2"

    @BeforeEach
    fun setup() {
        layerRepository = mock(InventoryCostLayerRepository::class.java)
        waRepository = mock(InventoryWaSnapshotRepository::class.java)
        organizationRepository = mock(OrganizationRepository::class.java)
        service = InventoryCostingService(layerRepository, waRepository, organizationRepository)
    }

    private fun mockOrg(method: InventoryCostingMethod) {
        `when`(organizationRepository.findById(orgId)).thenReturn(
            Optional.of(
                Organizations(
                    uuid = orgId,
                    orgSlug = "org",
                    name = "Org",
                    legalName = "Org LLC",
                    tradeName = "Org",
                    fiscalYearStart = LocalDateTime.now(),
                    timezone = "UTC",
                    inventoryCostingMethod = method,
                ),
            ),
        )
    }

    private fun movement(
        type: StockMovementType,
        quantity: BigDecimal,
        unitCost: BigDecimal? = null,
        warehouse: String = warehouseId,
        transferTo: String? = null,
        id: String = "mov-1",
        occurredAt: LocalDateTime = LocalDateTime.now(),
    ) = StockMovement(
        id = id,
        organizationId = orgId,
        type = type,
        productId = productId,
        warehouseId = warehouse,
        transferToWarehouseId = transferTo,
        quantity = quantity,
        unitCost = unitCost,
        occurredAt = occurredAt,
        createdBy = "user-1",
    )

    // ─── FIFO ──────────────────────────────────────────────────────────────

    @Test
    fun `FIFO RECEIPT adds a new cost layer`() {
        mockOrg(InventoryCostingMethod.FIFO)
        `when`(layerRepository.save(any<InventoryCostLayer>())).thenAnswer { it.arguments[0] }
        service.apply(movement(StockMovementType.RECEIPT, BigDecimal("10"), BigDecimal("5")))

        // Now mock that the new layer exists, and valuation reads it back
        `when`(
            layerRepository.findByOrganizationIdAndProductIdAndWarehouseIdOrderByOccurredAtAsc(
                orgId,
                productId,
                warehouseId,
            ),
        ).thenReturn(
            listOf(makeLayer(BigDecimal("10"), BigDecimal("10"), BigDecimal("5"))),
        )
        assertThat(service.valuationCost(orgId, productId, warehouseId)).isEqualByComparingTo("50")
    }

    @Test
    fun `FIFO ISSUE consumes oldest layers first`() {
        mockOrg(InventoryCostingMethod.FIFO)
        val older = makeLayer(BigDecimal("4"), BigDecimal("4"), BigDecimal("2"), id = "l1", occurredOffsetSec = -1000)
        val newer = makeLayer(BigDecimal("6"), BigDecimal("6"), BigDecimal("3"), id = "l2", occurredOffsetSec = 0)
        `when`(
            layerRepository.findByOrganizationIdAndProductIdAndWarehouseIdOrderByOccurredAtAsc(
                orgId,
                productId,
                warehouseId,
            ),
        ).thenReturn(listOf(older, newer))
        `when`(layerRepository.save(any<InventoryCostLayer>())).thenAnswer { it.arguments[0] }

        service.apply(movement(StockMovementType.ISSUE, BigDecimal("5")))

        val captor = org.mockito.ArgumentCaptor.forClass(InventoryCostLayer::class.java)
        org.mockito.Mockito
            .verify(layerRepository, org.mockito.Mockito.atLeast(2))
            .save(captor.capture())
        val saved = captor.allValues
        assertThat(saved.first { it.id == "l1" }.remainingQuantity).isEqualByComparingTo("0")
        assertThat(saved.first { it.id == "l2" }.remainingQuantity).isEqualByComparingTo("5")
    }

    @Test
    fun `FIFO TRANSFER consumes source layers and creates destination layer at weighted cost`() {
        mockOrg(InventoryCostingMethod.FIFO)
        val layer = makeLayer(BigDecimal("10"), BigDecimal("10"), BigDecimal("4"))
        `when`(
            layerRepository.findByOrganizationIdAndProductIdAndWarehouseIdOrderByOccurredAtAsc(
                orgId,
                productId,
                warehouseId,
            ),
        ).thenReturn(listOf(layer))
        `when`(layerRepository.save(any<InventoryCostLayer>())).thenAnswer { it.arguments[0] }

        service.apply(movement(StockMovementType.TRANSFER, BigDecimal("3"), transferTo = otherWarehouseId))

        val captor = org.mockito.ArgumentCaptor.forClass(InventoryCostLayer::class.java)
        org.mockito.Mockito
            .verify(layerRepository, org.mockito.Mockito.atLeast(2))
            .save(captor.capture())
        val destLayer = captor.allValues.first { it.warehouseId == otherWarehouseId }
        assertThat(destLayer.unitCost).isEqualByComparingTo("4")
        assertThat(destLayer.originalQuantity).isEqualByComparingTo("3")
    }

    // ─── Weighted Average ─────────────────────────────────────────────────

    @Test
    fun `WA RECEIPT updates snapshot quantity and total cost`() {
        mockOrg(InventoryCostingMethod.WEIGHTED_AVERAGE)
        `when`(
            waRepository.findByOrganizationIdAndProductIdAndWarehouseId(orgId, productId, warehouseId),
        ).thenReturn(Optional.empty())
        `when`(waRepository.save(any<InventoryWaSnapshot>())).thenAnswer { it.arguments[0] }

        service.apply(movement(StockMovementType.RECEIPT, BigDecimal("10"), BigDecimal("5")))

        val captor = org.mockito.ArgumentCaptor.forClass(InventoryWaSnapshot::class.java)
        org.mockito.Mockito
            .verify(waRepository)
            .save(captor.capture())
        assertThat(captor.value.quantity).isEqualByComparingTo("10")
        assertThat(captor.value.totalCost).isEqualByComparingTo("50")
    }

    @Test
    fun `WA second RECEIPT blends avg cost across batches`() {
        mockOrg(InventoryCostingMethod.WEIGHTED_AVERAGE)
        val existing =
            InventoryWaSnapshot(
                organizationId = orgId,
                productId = productId,
                warehouseId = warehouseId,
                quantity = BigDecimal("10"),
                totalCost = BigDecimal("50"),
            )
        `when`(
            waRepository.findByOrganizationIdAndProductIdAndWarehouseId(orgId, productId, warehouseId),
        ).thenReturn(Optional.of(existing))
        `when`(waRepository.save(any<InventoryWaSnapshot>())).thenAnswer { it.arguments[0] }

        // Add 10 units at 7 → total 20 units, total cost 50 + 70 = 120, avg = 6
        service.apply(movement(StockMovementType.RECEIPT, BigDecimal("10"), BigDecimal("7")))

        val captor = org.mockito.ArgumentCaptor.forClass(InventoryWaSnapshot::class.java)
        org.mockito.Mockito
            .verify(waRepository)
            .save(captor.capture())
        assertThat(captor.value.quantity).isEqualByComparingTo("20")
        assertThat(captor.value.totalCost).isEqualByComparingTo("120")
    }

    @Test
    fun `WA ISSUE consumes at current avg cost`() {
        mockOrg(InventoryCostingMethod.WEIGHTED_AVERAGE)
        val existing =
            InventoryWaSnapshot(
                organizationId = orgId,
                productId = productId,
                warehouseId = warehouseId,
                quantity = BigDecimal("20"),
                totalCost = BigDecimal("120"),
            )
        `when`(
            waRepository.findByOrganizationIdAndProductIdAndWarehouseId(orgId, productId, warehouseId),
        ).thenReturn(Optional.of(existing))
        `when`(waRepository.save(any<InventoryWaSnapshot>())).thenAnswer { it.arguments[0] }

        // Issue 5 units; avg = 6; consumed cost = 30; new qty 15, new total 90
        service.apply(movement(StockMovementType.ISSUE, BigDecimal("5")))

        val captor = org.mockito.ArgumentCaptor.forClass(InventoryWaSnapshot::class.java)
        org.mockito.Mockito
            .verify(waRepository)
            .save(captor.capture())
        assertThat(captor.value.quantity).isEqualByComparingTo("15")
        assertThat(captor.value.totalCost).isEqualByComparingTo("90")
    }

    @Test
    fun `WA valuation returns snapshot totalCost`() {
        mockOrg(InventoryCostingMethod.WEIGHTED_AVERAGE)
        `when`(
            waRepository.findByOrganizationIdAndProductIdAndWarehouseId(orgId, productId, warehouseId),
        ).thenReturn(
            Optional.of(
                InventoryWaSnapshot(
                    organizationId = orgId,
                    productId = productId,
                    warehouseId = warehouseId,
                    quantity = BigDecimal("15"),
                    totalCost = BigDecimal("90"),
                ),
            ),
        )
        assertThat(service.valuationCost(orgId, productId, warehouseId)).isEqualByComparingTo("90")
    }

    private fun makeLayer(
        original: BigDecimal,
        remaining: BigDecimal,
        unitCost: BigDecimal,
        id: String = "layer-1",
        occurredOffsetSec: Long = 0,
    ) = InventoryCostLayer(
        id = id,
        organizationId = orgId,
        productId = productId,
        warehouseId = warehouseId,
        originalQuantity = original,
        remainingQuantity = remaining,
        unitCost = unitCost,
        sourceMovementId = "mov-x",
        occurredAt = LocalDateTime.now().plusSeconds(occurredOffsetSec),
    )
}
