package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.CreateCountSessionRequest
import com.aquinofroilan.tessera.dto.CreateStockMovementRequest
import com.aquinofroilan.tessera.dto.RecordCountRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.model.InventoryCountLine
import com.aquinofroilan.tessera.model.InventoryCountSession
import com.aquinofroilan.tessera.model.InventoryCountStatus
import com.aquinofroilan.tessera.model.Product
import com.aquinofroilan.tessera.model.StockMovement
import com.aquinofroilan.tessera.model.StockMovementType
import com.aquinofroilan.tessera.model.StockOnHand
import com.aquinofroilan.tessera.model.Warehouse
import com.aquinofroilan.tessera.repository.InventoryCountSessionRepository
import com.aquinofroilan.tessera.repository.StockOnHandRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.Optional

class InventoryCountSessionServiceTest {
    private lateinit var repository: InventoryCountSessionRepository
    private lateinit var sohRepository: StockOnHandRepository
    private lateinit var warehouseService: WarehouseService
    private lateinit var productService: ProductService
    private lateinit var movementService: StockMovementService
    private lateinit var service: InventoryCountSessionService

    private val orgId = "org-1"
    private val userId = "user-1"
    private val whId = "wh-1"
    private val productAId = "prod-A"
    private val productBId = "prod-B"

    @BeforeEach
    fun setup() {
        repository = mock(InventoryCountSessionRepository::class.java)
        sohRepository = mock(StockOnHandRepository::class.java)
        warehouseService = mock(WarehouseService::class.java)
        productService = mock(ProductService::class.java)
        movementService = mock(StockMovementService::class.java)
        whenever(repository.save(any<InventoryCountSession>())).thenAnswer { it.arguments[0] }
        whenever(repository.findByOrganizationIdAndCode(any(), any())).thenReturn(Optional.empty())
        whenever(warehouseService.getWarehouse(whId, orgId)).thenReturn(
            Warehouse(id = whId, code = "MAIN", name = "Main", organizationId = orgId, isActive = true),
        )
        whenever(productService.getProduct(productAId, orgId)).thenReturn(product(productAId, "A"))
        whenever(productService.getProduct(productBId, orgId)).thenReturn(product(productBId, "B"))
        whenever(sohRepository.findByOrganizationId(orgId)).thenReturn(
            listOf(
                StockOnHand(organizationId = orgId, productId = productAId, warehouseId = whId, quantity = BigDecimal("100")),
                StockOnHand(organizationId = orgId, productId = productBId, warehouseId = whId, quantity = BigDecimal("50")),
                StockOnHand(organizationId = orgId, productId = "prod-other", warehouseId = "wh-other", quantity = BigDecimal("999")),
            ),
        )
        service = InventoryCountSessionService(repository, sohRepository, warehouseService, productService, movementService)
    }

    @Test
    fun `create snapshots on-hand for the chosen warehouse only`() {
        val s =
            service.createSession(
                CreateCountSessionRequest(code = "Q3-MAIN", warehouseId = whId),
                orgId,
                userId,
            )
        assertThat(s.status).isEqualTo(InventoryCountStatus.DRAFT)
        assertThat(s.lines).hasSize(2)
        assertThat(s.lines.map { it.productSku }).containsExactlyInAnyOrder("A", "B")
        assertThat(s.lines[0].expectedQuantity).isEqualByComparingTo(BigDecimal("100"))
    }

    @Test
    fun `recordCount transitions DRAFT to COUNTING and updates line`() {
        val draft =
            session(InventoryCountStatus.DRAFT)
                .copy(lines = listOf(line("l1", productAId, BigDecimal("100"))))
        whenever(repository.findById("s1")).thenReturn(Optional.of(draft))

        val updated = service.recordCount("s1", "l1", RecordCountRequest(countedQuantity = BigDecimal("95")), orgId)

        assertThat(updated.status).isEqualTo(InventoryCountStatus.COUNTING)
        assertThat(updated.lines[0].countedQuantity).isEqualByComparingTo(BigDecimal("95"))
        assertThat(updated.startedAt).isNotNull
    }

    @Test
    fun `recordCount rejects POSTED sessions`() {
        whenever(repository.findById("s1")).thenReturn(Optional.of(session(InventoryCountStatus.POSTED)))
        assertThatThrownBy {
            service.recordCount("s1", "l1", RecordCountRequest(countedQuantity = BigDecimal.ZERO), orgId)
        }.isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `postSession rejects when any line is unrecorded`() {
        val s =
            session(InventoryCountStatus.COUNTING).copy(
                lines =
                    listOf(
                        line("l1", productAId, BigDecimal("100")).copy(countedQuantity = BigDecimal("100")),
                        line("l2", productBId, BigDecimal("50")),
                    ),
            )
        whenever(repository.findById("s1")).thenReturn(Optional.of(s))
        assertThatThrownBy { service.postSession("s1", orgId, userId) }
            .isInstanceOf(BusinessRuleException::class.java)
            .hasMessageContaining("have not been counted")
    }

    @Test
    fun `postSession creates ADJUSTMENT movement only for variances`() {
        val s =
            session(InventoryCountStatus.COUNTING).copy(
                lines =
                    listOf(
                        line("l1", productAId, BigDecimal("100")).copy(countedQuantity = BigDecimal("95")),
                        line("l2", productBId, BigDecimal("50")).copy(countedQuantity = BigDecimal("50")),
                    ),
            )
        whenever(repository.findById("s1")).thenReturn(Optional.of(s))
        whenever(
            movementService.createMovement(any<CreateStockMovementRequest>(), any(), any()),
        ).thenAnswer {
            StockMovement(
                id = "mv1",
                organizationId = orgId,
                type = StockMovementType.ADJUSTMENT,
                productId = productAId,
                warehouseId = whId,
                quantity = BigDecimal("-5"),
                occurredAt = LocalDateTime.now(),
                createdBy = userId,
            )
        }

        val posted = service.postSession("s1", orgId, userId)

        assertThat(posted.status).isEqualTo(InventoryCountStatus.POSTED)
        assertThat(posted.lines[0].varianceQuantity).isEqualByComparingTo(BigDecimal("-5"))
        assertThat(posted.lines[0].adjustmentMovementId).isEqualTo("mv1")
        assertThat(posted.lines[1].varianceQuantity).isEqualByComparingTo(BigDecimal.ZERO)
        assertThat(posted.lines[1].adjustmentMovementId).isNull()
    }

    @Test
    fun `cancel rejects POSTED`() {
        whenever(repository.findById("s1")).thenReturn(Optional.of(session(InventoryCountStatus.POSTED)))
        assertThatThrownBy { service.cancelSession("s1", orgId) }
            .isInstanceOf(BusinessRuleException::class.java)
    }

    private fun session(status: InventoryCountStatus) =
        InventoryCountSession(
            id = "s1",
            organizationId = orgId,
            code = "Q3",
            warehouseId = whId,
            status = status,
            lines = emptyList(),
            createdBy = userId,
        )

    private fun line(
        id: String,
        productId: String,
        expected: BigDecimal,
    ) = InventoryCountLine(
        id = id,
        lineNumber = 1,
        productId = productId,
        productSku = productId,
        productName = productId,
        expectedQuantity = expected,
    )

    private fun product(
        id: String,
        sku: String,
    ) = Product(
        id = id,
        sku = sku,
        name = sku,
        listPrice = BigDecimal.ONE,
        priceCurrency = "USD",
        organizationId = orgId,
        isActive = true,
    )
}
