package com.aquinofroilan.tessera.domain.procurement.service

import com.aquinofroilan.tessera.domain.procurement.dto.CreateVendorEvaluationRequest
import com.aquinofroilan.tessera.domain.procurement.model.PurchaseOrder
import com.aquinofroilan.tessera.domain.procurement.model.PurchaseOrderLine
import com.aquinofroilan.tessera.domain.procurement.model.PurchaseOrderStatus
import com.aquinofroilan.tessera.domain.procurement.model.Vendor
import com.aquinofroilan.tessera.domain.procurement.model.VendorEvaluation
import com.aquinofroilan.tessera.domain.procurement.repository.PurchaseOrderRepository
import com.aquinofroilan.tessera.domain.procurement.repository.VendorEvaluationRepository
import com.aquinofroilan.tessera.domain.procurement.repository.VendorRepository
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID

class VendorPerformanceServiceTest {
    private lateinit var vendorRepository: VendorRepository
    private lateinit var purchaseOrderRepository: PurchaseOrderRepository
    private lateinit var vendorEvaluationRepository: VendorEvaluationRepository
    private lateinit var service: VendorPerformanceService

    private val orgId = UUID.fromString("4abe9f6d-6df3-6e5c-953e-3695db9a5216")
    private val vendorId = UUID.fromString("11111111-2222-3333-4444-555555555555")
    private val userId = UUID.fromString("bc17c97c-3d89-7d43-b7e0-7ca0266eafa8")

    @BeforeEach
    fun setUp() {
        vendorRepository = mock(VendorRepository::class.java)
        purchaseOrderRepository = mock(PurchaseOrderRepository::class.java)
        vendorEvaluationRepository = mock(VendorEvaluationRepository::class.java)
        service = VendorPerformanceService(vendorRepository, purchaseOrderRepository, vendorEvaluationRepository)
    }

    private fun createVendor() =
        Vendor(
            id = vendorId,
            name = "Acme Supplies",
            organizationId = orgId,
        )

    private fun createPurchaseOrder(
        orderDate: LocalDate,
        expectedDate: LocalDate?,
        receivedAt: LocalDateTime?,
        quantity: BigDecimal,
        receivedQuantity: BigDecimal,
        status: PurchaseOrderStatus = PurchaseOrderStatus.RECEIVED,
    ): PurchaseOrder {
        val line =
            PurchaseOrderLine(
                id = UUID.randomUUID(),
                productId = UUID.randomUUID(),
                productSku = "SKU-1",
                productName = "Product 1",
                quantity = quantity,
                unitCost = BigDecimal("10.00"),
                lineTotal = quantity.multiply(BigDecimal("10.00")),
                receivedQuantity = receivedQuantity,
                billedQuantity = receivedQuantity,
            )
        return PurchaseOrder(
            id = UUID.randomUUID(),
            poNumber = "PO-001",
            vendorId = vendorId,
            vendorName = "Acme Supplies",
            warehouseId = UUID.randomUUID(),
            orderDate = orderDate,
            expectedDate = expectedDate,
            organizationId = orgId,
            status = status,
            lines = listOf(line),
            totalAmount = line.lineTotal,
            createdBy = userId,
            receivedAt = receivedAt,
        )
    }

    @Test
    fun `getVendorPerformance returns default score when vendor has no orders`() {
        `when`(vendorRepository.findByIdAndOrganizationId(vendorId, orgId)).thenReturn(Optional.of(createVendor()))
        `when`(purchaseOrderRepository.findByOrganizationIdAndVendorId(orgId, vendorId)).thenReturn(emptyList())
        `when`(vendorEvaluationRepository.findByOrganizationIdAndVendorIdOrderByEvaluationDateDesc(orgId, vendorId))
            .thenReturn(emptyList())

        val summary = service.getVendorPerformance(vendorId, orgId)

        assertEquals("Acme Supplies", summary.vendorName)
        assertEquals(0, summary.totalOrders)
        assertEquals(100.0, summary.onTimeDeliveryRate)
        assertEquals(100.0, summary.qualityFulfillmentRate)
        assertEquals(100.0, summary.priceAccuracyRate)
        assertEquals(100.0, summary.overallScore)
        assertEquals("EXCELLENT", summary.ratingTier)
        assertNull(summary.evaluationAverageScore)
    }

    @Test
    fun `getVendorPerformance calculates accurate on-time delivery rate and fulfillment`() {
        val po1 =
            createPurchaseOrder(
                orderDate = LocalDate.of(2026, 1, 1),
                expectedDate = LocalDate.of(2026, 1, 10),
                receivedAt = LocalDateTime.of(2026, 1, 9, 10, 0), // On-time
                quantity = BigDecimal("100"),
                receivedQuantity = BigDecimal("100"),
            )
        val po2 =
            createPurchaseOrder(
                orderDate = LocalDate.of(2026, 1, 1),
                expectedDate = LocalDate.of(2026, 1, 10),
                receivedAt = LocalDateTime.of(2026, 1, 15, 10, 0), // 5 days late
                quantity = BigDecimal("50"),
                receivedQuantity = BigDecimal("40"), // 80% received
            )

        `when`(vendorRepository.findByIdAndOrganizationId(vendorId, orgId)).thenReturn(Optional.of(createVendor()))
        `when`(purchaseOrderRepository.findByOrganizationIdAndVendorId(orgId, vendorId)).thenReturn(listOf(po1, po2))
        `when`(vendorEvaluationRepository.findByOrganizationIdAndVendorIdOrderByEvaluationDateDesc(orgId, vendorId))
            .thenReturn(emptyList())

        val summary = service.getVendorPerformance(vendorId, orgId)

        assertEquals(2, summary.totalOrders)
        assertEquals(2, summary.completedOrders)
        assertEquals(50.0, summary.onTimeDeliveryRate)
        assertEquals(5.0, summary.averageDeliveryDelayDays)
        assertEquals(93.33, summary.qualityFulfillmentRate)
    }

    @Test
    fun `recordEvaluation creates and calculates overall score`() {
        `when`(vendorRepository.findByIdAndOrganizationId(vendorId, orgId)).thenReturn(Optional.of(createVendor()))
        `when`(vendorEvaluationRepository.save(any<VendorEvaluation>())).thenAnswer { it.arguments[0] }

        val request =
            CreateVendorEvaluationRequest(
                evaluationDate = LocalDate.of(2026, 2, 1),
                deliveryScore = BigDecimal("90.00"),
                qualityScore = BigDecimal("85.00"),
                priceAccuracyScore = BigDecimal("95.00"),
                comments = "Great vendor",
            )

        val response = service.recordEvaluation(vendorId, orgId, userId, request)

        assertNotNull(response.id)
        assertEquals(vendorId, response.vendorId)
        assertEquals(BigDecimal("90.00"), response.deliveryScore)
        assertEquals(BigDecimal("85.00"), response.qualityScore)
        assertEquals(BigDecimal("95.00"), response.priceAccuracyScore)
        // 90*0.4 + 85*0.35 + 95*0.25 = 36 + 29.75 + 23.75 = 89.50
        assertEquals(BigDecimal("89.50"), response.overallScore)
        assertEquals("Great vendor", response.comments)
    }

    @Test
    fun `getVendorPerformance throws ResourceNotFoundException when vendor does not exist`() {
        `when`(vendorRepository.findByIdAndOrganizationId(vendorId, orgId)).thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> {
            service.getVendorPerformance(vendorId, orgId)
        }
    }
}
