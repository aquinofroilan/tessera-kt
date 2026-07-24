package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.CreateStockMovementRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import com.aquinofroilan.tessera.model.StockMovement
import com.aquinofroilan.tessera.model.StockMovementType
import com.aquinofroilan.tessera.model.Warehouse
import com.aquinofroilan.tessera.repository.StockMovementRepository
import com.aquinofroilan.tessera.repository.StockOnHandRepository
import com.aquinofroilan.tessera.repository.WarehouseRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.Optional

class StockMovementServiceTest {
    private lateinit var stockMovementService: StockMovementService
    private lateinit var stockMovementRepository: StockMovementRepository
    private lateinit var warehouseRepository: WarehouseRepository
    private lateinit var stockOnHandRepository: StockOnHandRepository
    private lateinit var inventoryCostingService: InventoryCostingService
    private lateinit var inventoryPostingService: InventoryPostingService

    private val orgId = java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d")
    private val userId = java.util.UUID.fromString("3a01035d-c5db-3981-bf73-f18b3a0c1df9")
    private val productId = java.util.UUID.fromString("dbf2a095-ce0d-371a-bd21-a52d4a5a29c9")
    private val warehouseId = java.util.UUID.fromString("c91d2c12-b2b4-3634-a3bb-d0ff561af4ff")
    private val otherWarehouseId = java.util.UUID.fromString("1d0e273c-e8f6-3c81-aa83-af17bdd332f5")

    @BeforeEach
    fun setup() {
        stockMovementRepository = mock(StockMovementRepository::class.java)
        warehouseRepository = mock(WarehouseRepository::class.java)
        stockOnHandRepository = mock(StockOnHandRepository::class.java)
        inventoryCostingService = mock(InventoryCostingService::class.java)
        inventoryPostingService = mock(InventoryPostingService::class.java)
        whenever(stockOnHandRepository.applyDelta(any(), any(), any(), any(), any())).thenReturn(true)
        whenever(inventoryCostingService.apply(any())).thenReturn(BigDecimal.ZERO)
        stockMovementService =
            StockMovementService(
                stockMovementRepository,
                warehouseRepository,
                stockOnHandRepository,
                inventoryCostingService,
                inventoryPostingService,
            )
    }

    private fun mockWarehouse(
        id: java.util.UUID = warehouseId,
        code: String = "MAIN",
        allowNegativeStock: Boolean = false,
        isActive: Boolean = true,
        organizationId: java.util.UUID = orgId,
    ) {
        whenever(warehouseRepository.findById(id)).thenReturn(
            Optional.of(
                Warehouse(
                    id = id,
                    code = code,
                    name = "WH $code",
                    organizationId = organizationId,
                    allowNegativeStock = allowNegativeStock,
                    isActive = isActive,
                ),
            ),
        )
    }

    private fun answerSave() {
        whenever(stockMovementRepository.save(any<StockMovement>())).thenAnswer { it.arguments[0] }
    }

    @Test
    fun `createMovement RECEIPT requires unitCost`() {
        mockWarehouse()
        val request =
            CreateStockMovementRequest(
                type = StockMovementType.RECEIPT,
                productId = productId,
                warehouseId = warehouseId,
                quantity = BigDecimal("10"),
                unitCost = null,
            )
        val ex =
            assertThrows<BusinessRuleException> {
                stockMovementService.createMovement(request, orgId, userId)
            }
        assertThat(ex.message).contains("unitCost")
    }

    @Test
    fun `createMovement RECEIPT saves with provided unitCost`() {
        mockWarehouse()
        answerSave()
        val request =
            CreateStockMovementRequest(
                type = StockMovementType.RECEIPT,
                productId = productId,
                warehouseId = warehouseId,
                quantity = BigDecimal("10"),
                unitCost = BigDecimal("5.50"),
            )
        val result = stockMovementService.createMovement(request, orgId, userId)
        assertThat(result.type).isEqualTo(StockMovementType.RECEIPT)
        assertThat(result.quantity).isEqualByComparingTo("10")
        assertThat(result.unitCost).isEqualByComparingTo("5.50")
        assertThat(result.createdBy).isEqualTo(userId)
    }

    @Test
    fun `createMovement RECEIPT increments counter with positive delta`() {
        mockWarehouse(allowNegativeStock = false)
        answerSave()
        val request =
            CreateStockMovementRequest(
                type = StockMovementType.RECEIPT,
                productId = productId,
                warehouseId = warehouseId,
                quantity = BigDecimal("10"),
                unitCost = BigDecimal("1"),
            )
        stockMovementService.createMovement(request, orgId, userId)

        val deltaCaptor = argumentCaptor<BigDecimal>()
        val allowCaptor = argumentCaptor<Boolean>()
        verify(stockOnHandRepository).applyDelta(
            eq(orgId),
            eq(productId),
            eq(warehouseId),
            deltaCaptor.capture(),
            allowCaptor.capture(),
        )
        assertThat(deltaCaptor.firstValue).isEqualByComparingTo("10")
        assertThat(allowCaptor.firstValue).isFalse()
    }

    @Test
    fun `createMovement OPENING_BALANCE requires unitCost`() {
        mockWarehouse()
        val request =
            CreateStockMovementRequest(
                type = StockMovementType.OPENING_BALANCE,
                productId = productId,
                warehouseId = warehouseId,
                quantity = BigDecimal("5"),
                unitCost = null,
            )
        assertThrows<BusinessRuleException> {
            stockMovementService.createMovement(request, orgId, userId)
        }
    }

    @Test
    fun `createMovement ISSUE rejects when applyDelta reports insufficient stock`() {
        mockWarehouse(allowNegativeStock = false)
        whenever(
            stockOnHandRepository.applyDelta(eq(orgId), eq(productId), eq(warehouseId), any(), eq(false)),
        ).thenReturn(false)
        whenever(stockOnHandRepository.get(orgId, productId, warehouseId)).thenReturn(BigDecimal("3"))
        val request =
            CreateStockMovementRequest(
                type = StockMovementType.ISSUE,
                productId = productId,
                warehouseId = warehouseId,
                quantity = BigDecimal("5"),
            )
        val ex =
            assertThrows<BusinessRuleException> {
                stockMovementService.createMovement(request, orgId, userId)
            }
        assertThat(ex.message).contains("below zero")
        assertThat(ex.message).contains("current 3")
    }

    @Test
    fun `createMovement ISSUE passes allowNegative=true through to counter when warehouse permits`() {
        mockWarehouse(allowNegativeStock = true)
        answerSave()
        val request =
            CreateStockMovementRequest(
                type = StockMovementType.ISSUE,
                productId = productId,
                warehouseId = warehouseId,
                quantity = BigDecimal("5"),
            )
        stockMovementService.createMovement(request, orgId, userId)

        val allowCaptor = argumentCaptor<Boolean>()
        verify(stockOnHandRepository).applyDelta(any(), any(), any(), any(), allowCaptor.capture())
        assertThat(allowCaptor.firstValue).isTrue()
    }

    @Test
    fun `createMovement ISSUE decrements counter with negative delta`() {
        mockWarehouse(allowNegativeStock = false)
        answerSave()
        val request =
            CreateStockMovementRequest(
                type = StockMovementType.ISSUE,
                productId = productId,
                warehouseId = warehouseId,
                quantity = BigDecimal("4"),
            )
        stockMovementService.createMovement(request, orgId, userId)

        val deltaCaptor = argumentCaptor<BigDecimal>()
        verify(stockOnHandRepository).applyDelta(any(), any(), any(), deltaCaptor.capture(), any())
        assertThat(deltaCaptor.firstValue).isEqualByComparingTo("-4")
    }

    @Test
    fun `createMovement TRANSFER requires destination warehouse`() {
        mockWarehouse()
        val request =
            CreateStockMovementRequest(
                type = StockMovementType.TRANSFER,
                productId = productId,
                warehouseId = warehouseId,
                quantity = BigDecimal("2"),
            )
        val ex =
            assertThrows<BusinessRuleException> {
                stockMovementService.createMovement(request, orgId, userId)
            }
        assertThat(ex.message).contains("transferToWarehouseId")
    }

    @Test
    fun `createMovement TRANSFER rejects same source and destination`() {
        mockWarehouse()
        val request =
            CreateStockMovementRequest(
                type = StockMovementType.TRANSFER,
                productId = productId,
                warehouseId = warehouseId,
                transferToWarehouseId = warehouseId,
                quantity = BigDecimal("2"),
            )
        assertThrows<BusinessRuleException> {
            stockMovementService.createMovement(request, orgId, userId)
        }
    }

    @Test
    fun `createMovement TRANSFER applies delta to both source and destination`() {
        mockWarehouse(id = warehouseId, code = "MAIN", allowNegativeStock = true)
        mockWarehouse(id = otherWarehouseId, code = "EAST")
        answerSave()
        val request =
            CreateStockMovementRequest(
                type = StockMovementType.TRANSFER,
                productId = productId,
                warehouseId = warehouseId,
                transferToWarehouseId = otherWarehouseId,
                quantity = BigDecimal("2"),
            )
        val result = stockMovementService.createMovement(request, orgId, userId)
        assertThat(result.warehouseId).isEqualTo(warehouseId)
        assertThat(result.transferToWarehouseId).isEqualTo(otherWarehouseId)

        verify(stockOnHandRepository).applyDelta(
            eq(orgId),
            eq(productId),
            eq(warehouseId),
            argThat<BigDecimal> { compareTo(BigDecimal("-2")) == 0 },
            any(),
        )
        verify(stockOnHandRepository).applyDelta(
            eq(orgId),
            eq(productId),
            eq(otherWarehouseId),
            argThat<BigDecimal> { compareTo(BigDecimal("2")) == 0 },
            eq(true),
        )
    }

    @Test
    fun `createMovement ADJUSTMENT positive credits counter`() {
        mockWarehouse()
        answerSave()
        val request =
            CreateStockMovementRequest(
                type = StockMovementType.ADJUSTMENT,
                productId = productId,
                warehouseId = warehouseId,
                quantity = BigDecimal("3"),
            )
        val result = stockMovementService.createMovement(request, orgId, userId)
        assertThat(result.quantity).isEqualByComparingTo("3")

        val deltaCaptor = argumentCaptor<BigDecimal>()
        verify(stockOnHandRepository).applyDelta(any(), any(), any(), deltaCaptor.capture(), any())
        assertThat(deltaCaptor.firstValue).isEqualByComparingTo("3")
    }

    @Test
    fun `createMovement ADJUSTMENT negative rejects when applyDelta reports insufficient`() {
        mockWarehouse(allowNegativeStock = false)
        whenever(
            stockOnHandRepository.applyDelta(any(), any(), any(), any(), any()),
        ).thenReturn(false)
        whenever(stockOnHandRepository.get(orgId, productId, warehouseId)).thenReturn(BigDecimal("2"))
        val request =
            CreateStockMovementRequest(
                type = StockMovementType.ADJUSTMENT,
                productId = productId,
                warehouseId = warehouseId,
                quantity = BigDecimal("-5"),
            )
        assertThrows<BusinessRuleException> {
            stockMovementService.createMovement(request, orgId, userId)
        }
    }

    @Test
    fun `createMovement ADJUSTMENT zero is rejected`() {
        mockWarehouse()
        val request =
            CreateStockMovementRequest(
                type = StockMovementType.ADJUSTMENT,
                productId = productId,
                warehouseId = warehouseId,
                quantity = BigDecimal.ZERO,
            )
        assertThrows<BusinessRuleException> {
            stockMovementService.createMovement(request, orgId, userId)
        }
    }

    @Test
    fun `createMovement rejects when warehouse from other org`() {
        mockWarehouse(organizationId = java.util.UUID.fromString("fbede99a-0bef-3bf9-ba0b-8d28f050479d"))
        val request =
            CreateStockMovementRequest(
                type = StockMovementType.RECEIPT,
                productId = productId,
                warehouseId = warehouseId,
                quantity = BigDecimal("1"),
                unitCost = BigDecimal("1"),
            )
        assertThrows<ResourceNotFoundException> {
            stockMovementService.createMovement(request, orgId, userId)
        }
    }

    @Test
    fun `createMovement rejects when warehouse inactive`() {
        mockWarehouse(isActive = false)
        val request =
            CreateStockMovementRequest(
                type = StockMovementType.RECEIPT,
                productId = productId,
                warehouseId = warehouseId,
                quantity = BigDecimal("1"),
                unitCost = BigDecimal("1"),
            )
        val ex =
            assertThrows<BusinessRuleException> {
                stockMovementService.createMovement(request, orgId, userId)
            }
        assertThat(ex.message).contains("inactive")
    }

    @Test
    fun `createMovement uses occurredAt now when omitted`() {
        mockWarehouse()
        answerSave()
        val before = LocalDateTime.now()
        val request =
            CreateStockMovementRequest(
                type = StockMovementType.RECEIPT,
                productId = productId,
                warehouseId = warehouseId,
                quantity = BigDecimal("1"),
                unitCost = BigDecimal("1"),
            )
        val result = stockMovementService.createMovement(request, orgId, userId)
        assertThat(result.occurredAt).isAfterOrEqualTo(before)
    }

    @Test
    fun `listMovements delegates to repository`() {
        val now = LocalDateTime.now()
        whenever(stockMovementRepository.listMovements(orgId, productId, warehouseId, null, null, null))
            .thenReturn(
                listOf(
                    StockMovement(
                        organizationId = orgId,
                        type = StockMovementType.RECEIPT,
                        productId = productId,
                        warehouseId = warehouseId,
                        quantity = BigDecimal("1"),
                        unitCost = BigDecimal("1"),
                        occurredAt = now,
                        createdBy = userId,
                    ),
                ),
            )
        val result = stockMovementService.listMovements(orgId, productId, warehouseId)
        assertThat(result).hasSize(1)
    }

    @Test
    fun `onHand reads from counter repository`() {
        whenever(stockOnHandRepository.get(orgId, productId, warehouseId)).thenReturn(BigDecimal("7"))
        val result = stockMovementService.onHand(orgId, productId, warehouseId)
        assertThat(result).isEqualByComparingTo("7")
    }

    @Test
    fun `reverseMovement compensates a RECEIPT and marks the original reversed`() {
        mockWarehouse()
        answerSave()
        val original =
            StockMovement(
                id = java.util.UUID.fromString("9e0b57e6-ae56-3955-bb0a-78763cf4171e"),
                organizationId = orgId,
                type = StockMovementType.RECEIPT,
                productId = productId,
                warehouseId = warehouseId,
                quantity = BigDecimal("10"),
                unitCost = BigDecimal("5"),
                reference = "PO-PO-0001",
                occurredAt = LocalDateTime.now(),
                createdBy = userId,
            )
        whenever(stockMovementRepository.findById(java.util.UUID.fromString("9e0b57e6-ae56-3955-bb0a-78763cf4171e"))).thenReturn(Optional.of(original))

        val reversal = stockMovementService.reverseMovement(java.util.UUID.fromString("9e0b57e6-ae56-3955-bb0a-78763cf4171e"), orgId, userId)

        assertThat(reversal.type).isEqualTo(StockMovementType.ADJUSTMENT)
        assertThat(reversal.quantity).isEqualByComparingTo("-10")
        assertThat(reversal.reversalOfMovementId).isEqualTo(java.util.UUID.fromString("9e0b57e6-ae56-3955-bb0a-78763cf4171e"))

        val captor = argumentCaptor<StockMovement>()
        verify(stockMovementRepository, org.mockito.kotlin.atLeastOnce()).save(captor.capture())
        assertThat(captor.allValues).anyMatch { it.id == java.util.UUID.fromString("9e0b57e6-ae56-3955-bb0a-78763cf4171e") && it.reversed }
    }

    @Test
    fun `reverseMovement rejects an already-reversed movement`() {
        val original =
            StockMovement(
                id = java.util.UUID.fromString("9e0b57e6-ae56-3955-bb0a-78763cf4171e"),
                organizationId = orgId,
                type = StockMovementType.RECEIPT,
                productId = productId,
                warehouseId = warehouseId,
                quantity = BigDecimal("10"),
                unitCost = BigDecimal("5"),
                reversed = true,
                occurredAt = LocalDateTime.now(),
                createdBy = userId,
            )
        whenever(stockMovementRepository.findById(java.util.UUID.fromString("9e0b57e6-ae56-3955-bb0a-78763cf4171e"))).thenReturn(Optional.of(original))

        assertThrows<BusinessRuleException> { stockMovementService.reverseMovement(java.util.UUID.fromString("9e0b57e6-ae56-3955-bb0a-78763cf4171e"), orgId, userId) }
        verify(stockMovementRepository, never()).save(any<StockMovement>())
    }
}
