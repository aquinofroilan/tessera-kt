package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.CompleteWorkOrderRequest
import com.aquinofroilan.tessera.dto.CreateStockMovementRequest
import com.aquinofroilan.tessera.dto.IssueMaterialLineRequest
import com.aquinofroilan.tessera.dto.IssueMaterialRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.model.StockMovement
import com.aquinofroilan.tessera.model.StockMovementType
import com.aquinofroilan.tessera.model.WorkOrder
import com.aquinofroilan.tessera.model.WorkOrderComponent
import com.aquinofroilan.tessera.model.WorkOrderStatus
import com.aquinofroilan.tessera.repository.WorkOrderRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.LocalDateTime

class WorkOrderExecutionServiceTest {
    private lateinit var repository: WorkOrderRepository
    private lateinit var workOrderService: WorkOrderService
    private lateinit var stockMovementService: StockMovementService
    private lateinit var service: WorkOrderExecutionService

    private val orgId = "org-1"
    private val userId = "user-1"

    @BeforeEach
    fun setup() {
        repository = mock(WorkOrderRepository::class.java)
        workOrderService = mock(WorkOrderService::class.java)
        stockMovementService = mock(StockMovementService::class.java)
        whenever(repository.save(any<WorkOrder>())).thenAnswer { it.arguments[0] }
        service = WorkOrderExecutionService(repository, workOrderService, stockMovementService)
    }

    @Test
    fun `issueMaterial transitions RELEASED to IN_PROGRESS and stamps issued cost`() {
        val wo = released()
        whenever(workOrderService.getWorkOrder("wo1", orgId)).thenReturn(wo)
        whenever(stockMovementService.createMovementCapturingCost(any<CreateStockMovementRequest>(), any(), any()))
            .thenReturn(dummyMovement() to BigDecimal("50.00"))

        val updated =
            service.issueMaterial(
                "wo1",
                IssueMaterialRequest(lines = listOf(IssueMaterialLineRequest("c1", BigDecimal("5")))),
                orgId,
                userId,
            )

        assertThat(updated.status).isEqualTo(WorkOrderStatus.IN_PROGRESS)
        assertThat(updated.totalIssuedCost).isEqualByComparingTo(BigDecimal("50.00"))
        assertThat(updated.components[0].issuedQuantity).isEqualByComparingTo(BigDecimal("5"))
        assertThat(updated.components[0].issuedCost).isEqualByComparingTo(BigDecimal("50.00"))
    }

    @Test
    fun `issueMaterial rejects over-issue beyond remaining planned quantity`() {
        val wo =
            released().copy(
                components =
                    listOf(
                        component("c1", planned = BigDecimal("10"), issued = BigDecimal("8")),
                    ),
            )
        whenever(workOrderService.getWorkOrder("wo1", orgId)).thenReturn(wo)

        assertThatThrownBy {
            service.issueMaterial(
                "wo1",
                IssueMaterialRequest(lines = listOf(IssueMaterialLineRequest("c1", BigDecimal("3")))),
                orgId,
                userId,
            )
        }.isInstanceOf(BusinessRuleException::class.java)
            .hasMessageContaining("only")
    }

    @Test
    fun `complete with full quantity flips status to COMPLETED and stamps audit`() {
        val wo = released().copy(status = WorkOrderStatus.IN_PROGRESS, totalIssuedCost = BigDecimal("100.00"))
        whenever(workOrderService.getWorkOrder("wo1", orgId)).thenReturn(wo)
        whenever(stockMovementService.createMovementCapturingCost(any<CreateStockMovementRequest>(), any(), any()))
            .thenReturn(dummyMovement() to BigDecimal("100.00"))

        val updated =
            service.completeProduction(
                "wo1",
                CompleteWorkOrderRequest(quantityCompleted = BigDecimal("10")),
                orgId,
                userId,
            )

        assertThat(updated.status).isEqualTo(WorkOrderStatus.COMPLETED)
        assertThat(updated.quantityCompleted).isEqualByComparingTo(BigDecimal("10"))
        assertThat(updated.completedBy).isEqualTo(userId)
        assertThat(updated.totalCompletedCost).isEqualByComparingTo(BigDecimal("100.00"))
    }

    @Test
    fun `complete rejects when completed + scrap exceeds planned`() {
        val wo = released().copy(quantityCompleted = BigDecimal("8"))
        whenever(workOrderService.getWorkOrder("wo1", orgId)).thenReturn(wo)

        assertThatThrownBy {
            service.completeProduction(
                "wo1",
                CompleteWorkOrderRequest(quantityCompleted = BigDecimal("3")),
                orgId,
                userId,
            )
        }.isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `complete with both 0 quantityCompleted and 0 scrapped is rejected`() {
        val wo = released()
        whenever(workOrderService.getWorkOrder("wo1", orgId)).thenReturn(wo)

        assertThatThrownBy {
            service.completeProduction(
                "wo1",
                CompleteWorkOrderRequest(quantityCompleted = BigDecimal.ZERO, quantityScrapped = BigDecimal.ZERO),
                orgId,
                userId,
            )
        }.isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `cannot issue material on a COMPLETED work order`() {
        val wo = released().copy(status = WorkOrderStatus.COMPLETED)
        whenever(workOrderService.getWorkOrder("wo1", orgId)).thenReturn(wo)
        assertThatThrownBy {
            service.issueMaterial(
                "wo1",
                IssueMaterialRequest(lines = listOf(IssueMaterialLineRequest("c1", BigDecimal.ONE))),
                orgId,
                userId,
            )
        }.isInstanceOf(BusinessRuleException::class.java)
    }

    private fun released() =
        WorkOrder(
            id = "wo1",
            organizationId = orgId,
            woNumber = "WO-0001",
            productId = "prod-1",
            productSku = "SKU",
            productName = "Widget",
            bomId = "bom-1",
            routingId = null,
            quantity = BigDecimal("10"),
            sourceWarehouseId = "wh-src",
            targetWarehouseId = "wh-tgt",
            status = WorkOrderStatus.RELEASED,
            components = listOf(component("c1", planned = BigDecimal("10"), issued = BigDecimal.ZERO)),
            operations = emptyList(),
            createdBy = userId,
        )

    private fun component(
        id: String,
        planned: BigDecimal,
        issued: BigDecimal,
    ) = WorkOrderComponent(
        id = id,
        lineNumber = 1,
        componentProductId = "comp-prod",
        componentSku = "COMP",
        componentName = "Component",
        plannedQuantity = planned,
        issuedQuantity = issued,
    )

    private fun dummyMovement() =
        StockMovement(
            organizationId = orgId,
            type = StockMovementType.WIP_ISSUE,
            productId = "comp-prod",
            warehouseId = "wh-src",
            quantity = BigDecimal.ONE,
            occurredAt = LocalDateTime.now(),
            createdBy = userId,
        )
}
