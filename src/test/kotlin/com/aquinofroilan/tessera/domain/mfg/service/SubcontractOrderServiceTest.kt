package com.aquinofroilan.tessera.domain.mfg.service

import com.aquinofroilan.tessera.domain.inventory.model.Product
import com.aquinofroilan.tessera.domain.inventory.model.StockMovement
import com.aquinofroilan.tessera.domain.inventory.model.StockMovementType
import com.aquinofroilan.tessera.domain.inventory.repository.ProductRepository
import com.aquinofroilan.tessera.domain.inventory.service.StockMovementService
import com.aquinofroilan.tessera.domain.mfg.dto.CreateSubcontractComponentRequest
import com.aquinofroilan.tessera.domain.mfg.dto.CreateSubcontractOrderRequest
import com.aquinofroilan.tessera.domain.mfg.dto.DispatchSubcontractOrderRequest
import com.aquinofroilan.tessera.domain.mfg.dto.ReceiveSubcontractGoodsRequest
import com.aquinofroilan.tessera.domain.mfg.model.SubcontractComponent
import com.aquinofroilan.tessera.domain.mfg.model.SubcontractOrder
import com.aquinofroilan.tessera.domain.mfg.model.SubcontractOrderStatus
import com.aquinofroilan.tessera.domain.mfg.model.WorkOrder
import com.aquinofroilan.tessera.domain.mfg.model.WorkOrderComponent
import com.aquinofroilan.tessera.domain.mfg.model.WorkOrderOperation
import com.aquinofroilan.tessera.domain.mfg.model.WorkOrderOperationStatus
import com.aquinofroilan.tessera.domain.mfg.model.WorkOrderStatus
import com.aquinofroilan.tessera.domain.mfg.repository.SubcontractOrderRepository
import com.aquinofroilan.tessera.domain.mfg.repository.WorkOrderRepository
import com.aquinofroilan.tessera.domain.procurement.model.Vendor
import com.aquinofroilan.tessera.domain.procurement.repository.VendorRepository
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID

class SubcontractOrderServiceTest {
    private lateinit var subcontractOrderRepository: SubcontractOrderRepository
    private lateinit var workOrderRepository: WorkOrderRepository
    private lateinit var vendorRepository: VendorRepository
    private lateinit var productRepository: ProductRepository
    private lateinit var stockMovementService: StockMovementService
    private lateinit var service: SubcontractOrderService

    private val orgId = UUID.fromString("4abe9f6d-6df3-6e5c-953e-3695db9a5216")
    private val userId = UUID.fromString("bc17c97c-3d89-7d43-b7e0-7ca0266eafa8")
    private val vendorId = UUID.fromString("11111111-2222-3333-4444-555555555555")
    private val workOrderId = UUID.fromString("22222222-3333-4444-5555-666666666666")
    private val productId = UUID.fromString("33333333-4444-5555-6666-777777777777")
    private val componentProductId = UUID.fromString("44444444-5555-6666-7777-888888888888")
    private val operationId = UUID.fromString("55555555-6666-7777-8888-999999999999")
    private val warehouseId = UUID.fromString("66666666-7777-8888-9999-000000000000")

    @BeforeEach
    fun setup() {
        subcontractOrderRepository = mock()
        workOrderRepository = mock()
        vendorRepository = mock()
        productRepository = mock()
        stockMovementService = mock()

        service =
            SubcontractOrderService(
                subcontractOrderRepository,
                workOrderRepository,
                vendorRepository,
                productRepository,
                stockMovementService,
            )
    }

    private fun createVendor() =
        Vendor(
            id = vendorId,
            organizationId = orgId,
            name = "Precision Plating Inc",
        )

    private fun createWorkOrder() =
        WorkOrder(
            id = workOrderId,
            organizationId = orgId,
            woNumber = "WO-0001",
            productId = productId,
            productSku = "WIDGET-01",
            productName = "Widget Assembly",
            bomId = UUID.randomUUID(),
            quantity = BigDecimal("100"),
            sourceWarehouseId = warehouseId,
            targetWarehouseId = warehouseId,
            status = WorkOrderStatus.RELEASED,
            createdBy = userId.toString(),
            components =
                listOf(
                    WorkOrderComponent(
                        lineNumber = 1,
                        componentProductId = componentProductId,
                        componentSku = "RAW-STEEL",
                        componentName = "Raw Steel Plate",
                        plannedQuantity = BigDecimal("100"),
                    ),
                ),
            operations =
                listOf(
                    WorkOrderOperation(
                        id = operationId,
                        operationNumber = 10,
                        workCenterId = UUID.randomUUID(),
                        workCenterCode = "SUBCONTRACT-01",
                        description = "Heat Treatment Service",
                        status = WorkOrderOperationStatus.PENDING,
                    ),
                ),
        )

    private fun createProduct() =
        Product(
            id = componentProductId,
            organizationId = orgId,
            sku = "RAW-STEEL",
            name = "Raw Steel Plate",
            listPrice = BigDecimal("10.00"),
            priceCurrency = "USD",
        )

    private fun createSubcontractOrder(status: SubcontractOrderStatus = SubcontractOrderStatus.DRAFT): SubcontractOrder {
        val comp =
            SubcontractComponent(
                productId = componentProductId,
                productSku = "RAW-STEEL",
                productName = "Raw Steel Plate",
                plannedQuantity = BigDecimal("100"),
            )
        return SubcontractOrder(
            organizationId = orgId,
            orderNumber = "SCO-00001",
            workOrderId = workOrderId,
            operationId = operationId,
            operationNumber = 10,
            vendorId = vendorId,
            serviceItemName = "Heat Treatment Service",
            quantity = BigDecimal("100"),
            receivedQuantity = BigDecimal.ZERO,
            unitServiceCost = BigDecimal("5.00"),
            totalCost = BigDecimal("500.00"),
            status = status,
            createdBy = userId,
            components = mutableListOf(comp),
        )
    }

    @Test
    fun `createSubcontractOrder creates draft order and auto-populates WO components`() {
        val wo = createWorkOrder()
        val vendor = createVendor()

        whenever(workOrderRepository.findById(workOrderId)).thenReturn(Optional.of(wo))
        whenever(vendorRepository.findByIdAndOrganizationId(vendorId, orgId)).thenReturn(Optional.of(vendor))
        whenever(subcontractOrderRepository.countByOrganizationId(orgId)).thenReturn(0)
        whenever(subcontractOrderRepository.save(any<SubcontractOrder>())).thenAnswer { it.arguments[0] }

        val request =
            CreateSubcontractOrderRequest(
                workOrderId = workOrderId,
                operationNumber = 10,
                vendorId = vendorId,
                serviceItemName = "Heat Treatment Service",
                quantity = BigDecimal("100"),
                unitServiceCost = BigDecimal("5.00"),
            )

        val response = service.createSubcontractOrder(orgId, userId, request)

        assertEquals("SCO-00001", response.orderNumber)
        assertEquals(SubcontractOrderStatus.DRAFT, response.status)
        assertEquals(BigDecimal("500.00"), response.totalCost)
        assertEquals("Precision Plating Inc", response.vendorName)
        assertEquals(1, response.components.size)
        assertEquals("RAW-STEEL", response.components[0].productSku)
    }

    @Test
    fun `createSubcontractOrder with explicit components`() {
        val wo = createWorkOrder()
        val vendor = createVendor()
        val product = createProduct()

        whenever(workOrderRepository.findById(workOrderId)).thenReturn(Optional.of(wo))
        whenever(vendorRepository.findByIdAndOrganizationId(vendorId, orgId)).thenReturn(Optional.of(vendor))
        whenever(productRepository.findById(componentProductId)).thenReturn(Optional.of(product))
        whenever(subcontractOrderRepository.countByOrganizationId(orgId)).thenReturn(1)
        whenever(subcontractOrderRepository.save(any<SubcontractOrder>())).thenAnswer { it.arguments[0] }

        val request =
            CreateSubcontractOrderRequest(
                workOrderId = workOrderId,
                operationNumber = 10,
                vendorId = vendorId,
                serviceItemName = "Heat Treatment Service",
                quantity = BigDecimal("100"),
                unitServiceCost = BigDecimal("5.00"),
                components =
                    listOf(
                        CreateSubcontractComponentRequest(
                            productId = componentProductId,
                            plannedQuantity = BigDecimal("100"),
                        ),
                    ),
            )

        val response = service.createSubcontractOrder(orgId, userId, request)

        assertEquals("SCO-00002", response.orderNumber)
        assertEquals(1, response.components.size)
    }

    @Test
    fun `createSubcontractOrder throws when work order not found`() {
        whenever(workOrderRepository.findById(workOrderId)).thenReturn(Optional.empty())

        val request =
            CreateSubcontractOrderRequest(
                workOrderId = workOrderId,
                operationNumber = 10,
                vendorId = vendorId,
                serviceItemName = "Heat Treatment Service",
                quantity = BigDecimal("100"),
            )

        assertThrows(ResourceNotFoundException::class.java) {
            service.createSubcontractOrder(orgId, userId, request)
        }
    }

    @Test
    fun `dispatchComponents creates stock movements and marks order DISPATCHED`() {
        val order = createSubcontractOrder(SubcontractOrderStatus.DRAFT)
        val wo = createWorkOrder()
        val vendor = createVendor()

        whenever(subcontractOrderRepository.findByIdAndOrganizationId(order.id, orgId)).thenReturn(Optional.of(order))
        whenever(workOrderRepository.findById(workOrderId)).thenReturn(Optional.of(wo))
        whenever(vendorRepository.findById(vendorId)).thenReturn(Optional.of(vendor))
        whenever(subcontractOrderRepository.save(any<SubcontractOrder>())).thenAnswer { it.arguments[0] }

        val dummyMovement =
            StockMovement(
                productId = componentProductId,
                warehouseId = warehouseId,
                type = StockMovementType.WIP_ISSUE,
                quantity = BigDecimal("100"),
                organizationId = orgId,
                occurredAt = LocalDateTime.now(),
                createdBy = userId,
            )
        whenever(stockMovementService.createMovementCapturingCost(any(), any(), any()))
            .thenReturn(Pair(dummyMovement, BigDecimal("1000.00")))

        val response = service.dispatchComponents(order.id, orgId, userId, DispatchSubcontractOrderRequest())

        assertEquals(SubcontractOrderStatus.DISPATCHED, response.status)
        assertNotNull(response.dispatchedAt)
        assertEquals(BigDecimal("100"), response.components[0].dispatchedQuantity)
        verify(stockMovementService).createMovementCapturingCost(any(), any(), any())
        verify(workOrderRepository).save(any())
    }

    @Test
    fun `receiveProcessedGoods updates received quantity, WO operation status and costs`() {
        val order = createSubcontractOrder(SubcontractOrderStatus.DISPATCHED)
        val wo = createWorkOrder().copy(status = WorkOrderStatus.IN_PROGRESS)
        val vendor = createVendor()

        whenever(subcontractOrderRepository.findByIdAndOrganizationId(order.id, orgId)).thenReturn(Optional.of(order))
        whenever(workOrderRepository.findById(workOrderId)).thenReturn(Optional.of(wo))
        whenever(vendorRepository.findById(vendorId)).thenReturn(Optional.of(vendor))
        whenever(subcontractOrderRepository.save(any<SubcontractOrder>())).thenAnswer { it.arguments[0] }

        val request =
            ReceiveSubcontractGoodsRequest(
                quantityReceived = BigDecimal("100"),
                unitServiceCostOverride = BigDecimal("5.00"),
            )

        val response = service.receiveProcessedGoods(order.id, orgId, userId, request)

        assertEquals(SubcontractOrderStatus.COMPLETED, response.status)
        assertEquals(BigDecimal("100"), response.receivedQuantity)
        assertNotNull(response.completedAt)
        verify(workOrderRepository).save(any())
    }

    @Test
    fun `receiveProcessedGoods handles partial receipt`() {
        val order = createSubcontractOrder(SubcontractOrderStatus.DISPATCHED)
        val wo = createWorkOrder().copy(status = WorkOrderStatus.IN_PROGRESS)
        val vendor = createVendor()

        whenever(subcontractOrderRepository.findByIdAndOrganizationId(order.id, orgId)).thenReturn(Optional.of(order))
        whenever(workOrderRepository.findById(workOrderId)).thenReturn(Optional.of(wo))
        whenever(vendorRepository.findById(vendorId)).thenReturn(Optional.of(vendor))
        whenever(subcontractOrderRepository.save(any<SubcontractOrder>())).thenAnswer { it.arguments[0] }

        val request =
            ReceiveSubcontractGoodsRequest(
                quantityReceived = BigDecimal("40"),
            )

        val response = service.receiveProcessedGoods(order.id, orgId, userId, request)

        assertEquals(SubcontractOrderStatus.PARTIALLY_RECEIVED, response.status)
        assertEquals(BigDecimal("40"), response.receivedQuantity)
    }

    @Test
    fun `cancelSubcontractOrder cancels draft order`() {
        val order = createSubcontractOrder(SubcontractOrderStatus.DRAFT)
        val vendor = createVendor()

        whenever(subcontractOrderRepository.findByIdAndOrganizationId(order.id, orgId)).thenReturn(Optional.of(order))
        whenever(vendorRepository.findById(vendorId)).thenReturn(Optional.of(vendor))
        whenever(subcontractOrderRepository.save(any<SubcontractOrder>())).thenAnswer { it.arguments[0] }

        val response = service.cancelSubcontractOrder(order.id, orgId, userId)

        assertEquals(SubcontractOrderStatus.CANCELLED, response.status)
        assertNotNull(response.cancelledAt)
    }

    @Test
    fun `cancelSubcontractOrder throws on completed order`() {
        val order = createSubcontractOrder(SubcontractOrderStatus.COMPLETED)

        whenever(subcontractOrderRepository.findByIdAndOrganizationId(order.id, orgId)).thenReturn(Optional.of(order))

        assertThrows(BusinessRuleException::class.java) {
            service.cancelSubcontractOrder(order.id, orgId, userId)
        }
    }
}
