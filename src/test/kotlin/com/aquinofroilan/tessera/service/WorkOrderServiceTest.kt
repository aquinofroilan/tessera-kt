package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.CreateWorkOrderRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.model.BillOfMaterials
import com.aquinofroilan.tessera.model.BomLine
import com.aquinofroilan.tessera.model.BomStatus
import com.aquinofroilan.tessera.model.Product
import com.aquinofroilan.tessera.model.RoutingStatus
import com.aquinofroilan.tessera.model.Warehouse
import com.aquinofroilan.tessera.model.WorkOrder
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
import java.util.Optional

class WorkOrderServiceTest {
    private lateinit var repository: WorkOrderRepository
    private lateinit var productService: ProductService
    private lateinit var warehouseService: WarehouseService
    private lateinit var bomService: BillOfMaterialsService
    private lateinit var routingService: RoutingService
    private lateinit var service: WorkOrderService

    private val orgId = "550e8400-e29b-41d4-a716-446655440000"
    private val userId = "550e8400-e29b-41d4-a716-446655440001"
    private val productId = "550e8400-e29b-41d4-a716-446655440002"
    private val componentId = "550e8400-e29b-41d4-a716-446655440003"
    private val bomId = "550e8400-e29b-41d4-a716-446655440004"
    private val sourceWh = "550e8400-e29b-41d4-a716-446655440005"
    private val targetWh = "550e8400-e29b-41d4-a716-446655440006"

    @BeforeEach
    fun setup() {
        repository = mock(WorkOrderRepository::class.java)
        productService = mock(ProductService::class.java)
        warehouseService = mock(WarehouseService::class.java)
        bomService = mock(BillOfMaterialsService::class.java)
        routingService = mock(RoutingService::class.java)
        whenever(repository.save(any<WorkOrder>())).thenAnswer { it.arguments[0] }
        whenever(repository.countByOrganizationId(orgId)).thenReturn(0)
        whenever(
            productService.getProduct(java.util.UUID.fromString(productId), java.util.UUID.fromString(orgId)),
        ).thenReturn(product(productId, "PARENT"))
        whenever(
            warehouseService.getWarehouse(java.util.UUID.fromString(sourceWh), java.util.UUID.fromString(orgId)),
        ).thenReturn(warehouse(sourceWh, "SRC"))
        whenever(
            warehouseService.getWarehouse(java.util.UUID.fromString(targetWh), java.util.UUID.fromString(orgId)),
        ).thenReturn(warehouse(targetWh, "TGT"))
        whenever(
            bomService.listBoms(java.util.UUID.fromString(orgId), BomStatus.ACTIVE, java.util.UUID.fromString(productId)),
        ).thenReturn(listOf(activeBom(isDefault = true)))
        whenever(routingService.listRoutings(orgId, RoutingStatus.ACTIVE, productId)).thenReturn(emptyList())
        service = WorkOrderService(repository, productService, warehouseService, bomService, routingService)
    }

    @Test
    fun `create snapshots components with scrap-adjusted quantity and stamps WO number`() {
        val wo =
            service.createWorkOrder(
                CreateWorkOrderRequest(
                    productId = productId,
                    bomId = null,
                    routingId = null,
                    quantity = BigDecimal("10"),
                    sourceWarehouseId = sourceWh,
                    targetWarehouseId = targetWh,
                ),
                orgId,
                userId,
            )

        assertThat(wo.woNumber).isEqualTo("WO-0001")
        assertThat(wo.status).isEqualTo(WorkOrderStatus.DRAFT)
        assertThat(wo.components).hasSize(1)
        // BOM has 2.0 per unit with 5% scrap; 10 units * 2.0 * 1.05 = 21.0000
        assertThat(wo.components[0].plannedQuantity).isEqualByComparingTo(BigDecimal("21.0000"))
    }

    @Test
    fun `create rejects when no default ACTIVE bom and none specified`() {
        whenever(
            bomService.listBoms(java.util.UUID.fromString(orgId), BomStatus.ACTIVE, java.util.UUID.fromString(productId)),
        ).thenReturn(emptyList())
        val req =
            CreateWorkOrderRequest(
                productId = productId,
                bomId = null,
                routingId = null,
                quantity = BigDecimal("1"),
                sourceWarehouseId = sourceWh,
                targetWarehouseId = targetWh,
            )
        assertThatThrownBy { service.createWorkOrder(req, orgId, userId) }
            .isInstanceOf(BusinessRuleException::class.java)
            .hasMessageContaining("default ACTIVE BOM")
    }

    @Test
    fun `release flips DRAFT to RELEASED and stamps audit fields`() {
        val draft = draftWO()
        whenever(repository.findById("wo1")).thenReturn(Optional.of(draft))
        val released = service.releaseWorkOrder("wo1", orgId, userId)
        assertThat(released.status).isEqualTo(WorkOrderStatus.RELEASED)
        assertThat(released.releasedBy).isEqualTo(userId)
    }

    @Test
    fun `release rejects non-DRAFT work orders`() {
        val released = draftWO().copy(status = WorkOrderStatus.RELEASED)
        whenever(repository.findById("wo1")).thenReturn(Optional.of(released))
        assertThatThrownBy { service.releaseWorkOrder("wo1", orgId, userId) }
            .isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `cancel rejects COMPLETED work orders`() {
        val done = draftWO().copy(status = WorkOrderStatus.COMPLETED)
        whenever(repository.findById("wo1")).thenReturn(Optional.of(done))
        assertThatThrownBy { service.cancelWorkOrder("wo1", orgId, userId) }
            .isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `cancel is idempotent for already-cancelled`() {
        val cancelled = draftWO().copy(status = WorkOrderStatus.CANCELLED)
        whenever(repository.findById("wo1")).thenReturn(Optional.of(cancelled))
        val result = service.cancelWorkOrder("wo1", orgId, userId)
        assertThat(result.status).isEqualTo(WorkOrderStatus.CANCELLED)
    }

    private fun activeBom(isDefault: Boolean) =
        BillOfMaterials(
            id = java.util.UUID.fromString(bomId),
            organizationId = java.util.UUID.fromString(orgId),
            productId = java.util.UUID.fromString(productId),
            code = "BOM",
            name = "BOM",
            status = BomStatus.ACTIVE,
            isDefault = isDefault,
            lines =
                listOf(
                    BomLine(
                        lineNumber = 1,
                        componentProductId = java.util.UUID.fromString(componentId),
                        componentSku = "COMP",
                        componentName = "Component",
                        quantity = BigDecimal("2.0"),
                        scrapPct = BigDecimal("5"),
                    ),
                ),
            createdBy = java.util.UUID.fromString(userId),
        )

    private fun draftWO() =
        WorkOrder(
            id = "wo1",
            organizationId = orgId,
            woNumber = "WO-0001",
            productId = productId,
            productSku = "PARENT",
            productName = "PARENT",
            bomId = bomId,
            routingId = null,
            quantity = BigDecimal("10"),
            sourceWarehouseId = sourceWh,
            targetWarehouseId = targetWh,
            status = WorkOrderStatus.DRAFT,
            components = emptyList(),
            operations = emptyList(),
            createdBy = userId,
        )

    private fun product(
        id: String,
        sku: String,
    ) = Product(
        id = java.util.UUID.fromString(id),
        sku = sku,
        name = sku,
        listPrice = BigDecimal.ONE,
        priceCurrency = "USD",
        organizationId = java.util.UUID.fromString(orgId),
        isActive = true,
    )

    private fun warehouse(
        id: String,
        code: String,
    ) = Warehouse(
        id = java.util.UUID.fromString(id),
        code = code,
        name = code,
        organizationId = java.util.UUID.fromString(orgId),
        isActive = true,
    )
}
