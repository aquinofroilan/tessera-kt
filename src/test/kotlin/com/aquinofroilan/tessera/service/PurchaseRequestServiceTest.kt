package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.ConvertPurchaseRequestLineCost
import com.aquinofroilan.tessera.dto.ConvertPurchaseRequestRequest
import com.aquinofroilan.tessera.dto.CreatePurchaseOrderRequest
import com.aquinofroilan.tessera.dto.CreatePurchaseRequestLineRequest
import com.aquinofroilan.tessera.dto.CreatePurchaseRequestRequest
import com.aquinofroilan.tessera.event.DomainEventPublisher
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import com.aquinofroilan.tessera.model.Product
import com.aquinofroilan.tessera.model.PurchaseOrder
import com.aquinofroilan.tessera.model.PurchaseRequest
import com.aquinofroilan.tessera.model.PurchaseRequestLine
import com.aquinofroilan.tessera.model.PurchaseRequestStatus
import com.aquinofroilan.tessera.model.Vendor
import com.aquinofroilan.tessera.model.Warehouse
import com.aquinofroilan.tessera.repository.PurchaseRequestRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

class PurchaseRequestServiceTest {
    private lateinit var repository: PurchaseRequestRepository
    private lateinit var productService: ProductService
    private lateinit var vendorService: VendorService
    private lateinit var warehouseService: WarehouseService
    private lateinit var purchaseOrderService: PurchaseOrderService
    private lateinit var eventPublisher: DomainEventPublisher
    private lateinit var service: PurchaseRequestService

    private val orgId = java.util.UUID.fromString("e5628ca4-87a8-3e6f-8ae2-20213cc7ef92")
    private val userId = java.util.UUID.fromString("1db2395f-13ba-3d37-9d2b-f77d3eb3aa2e")

    @BeforeEach
    fun setup() {
        repository = mock(PurchaseRequestRepository::class.java)
        productService = mock(ProductService::class.java)
        vendorService = mock(VendorService::class.java)
        warehouseService = mock(WarehouseService::class.java)
        purchaseOrderService = mock(PurchaseOrderService::class.java)
        eventPublisher = mock(DomainEventPublisher::class.java)
        whenever(repository.countByOrganizationId(orgId)).thenReturn(0L)
        whenever(repository.save(any<PurchaseRequest>())).thenAnswer { it.arguments[0] }
        whenever(productService.getProduct(java.util.UUID.fromString("c2cf5eda-4c7a-30a7-9e0b-be843869ca89"), orgId)).thenReturn(
            Product(
                id = java.util.UUID.fromString("c2cf5eda-4c7a-30a7-9e0b-be843869ca89"),
                sku = "SKU-1",
                name = "Widget",
                listPrice = BigDecimal("9"),
                priceCurrency = "USD",
                organizationId = orgId,
            ),
        )
        whenever(
            vendorService.getVendor(java.util.UUID.fromString("718fa2b3-0eb7-3a9c-987f-a0cbe216ac6a"), orgId),
        ).thenReturn(Vendor(id = java.util.UUID.fromString("718fa2b3-0eb7-3a9c-987f-a0cbe216ac6a"), name = "Acme", organizationId = orgId))
        whenever(warehouseService.getWarehouse(java.util.UUID.fromString("c91d2c12-b2b4-3634-a3bb-d0ff561af4ff"), orgId))
            .thenReturn(
                Warehouse(
                    id = java.util.UUID.fromString("c91d2c12-b2b4-3634-a3bb-d0ff561af4ff"),
                    code = "MAIN",
                    name = "Main",
                    organizationId = orgId,
                ),
            )
        service =
            PurchaseRequestService(
                repository,
                productService,
                vendorService,
                warehouseService,
                purchaseOrderService,
                eventPublisher,
            )
    }

    private fun lineReq(
        quantity: BigDecimal = BigDecimal("3"),
        estimatedUnitCost: BigDecimal? = BigDecimal("5"),
    ) = CreatePurchaseRequestLineRequest(
        productId = java.util.UUID.fromString("c2cf5eda-4c7a-30a7-9e0b-be843869ca89"),
        quantity = quantity,
        estimatedUnitCost = estimatedUnitCost,
    )

    private fun approvedRequest(
        lines: List<PurchaseRequestLine> =
            listOf(
                PurchaseRequestLine(
                    id = java.util.UUID.fromString("778ad073-53c6-3e1d-9228-4882f83295f9"),
                    lineNumber = 1,
                    productId = java.util.UUID.fromString("c2cf5eda-4c7a-30a7-9e0b-be843869ca89"),
                    productSku = "SKU-1",
                    productName = "Widget",
                    quantity = BigDecimal("3"),
                    estimatedUnitCost = BigDecimal("5"),
                ),
            ),
        suggestedVendorId: UUID? = java.util.UUID.fromString("718fa2b3-0eb7-3a9c-987f-a0cbe216ac6a"),
        warehouseId: UUID? = java.util.UUID.fromString("c91d2c12-b2b4-3634-a3bb-d0ff561af4ff"),
    ) = PurchaseRequest(
        id = java.util.UUID.fromString("0cf84710-b013-3270-ad01-fbdd0de0af26"),
        prNumber = "PR-0001",
        organizationId = orgId,
        status = PurchaseRequestStatus.APPROVED,
        suggestedVendorId = suggestedVendorId,
        warehouseId = warehouseId,
        lines = lines,
        requestedBy = userId,
    )

    @Test
    fun `create persists a draft with a generated number`() {
        val pr =
            service.createPurchaseRequest(
                CreatePurchaseRequestRequest(
                    suggestedVendorId = java.util.UUID.fromString("718fa2b3-0eb7-3a9c-987f-a0cbe216ac6a"),
                    warehouseId = java.util.UUID.fromString("c91d2c12-b2b4-3634-a3bb-d0ff561af4ff"),
                    lines = listOf(lineReq()),
                ),
                orgId,
                userId,
            )

        assertThat(pr.status).isEqualTo(PurchaseRequestStatus.DRAFT)
        assertThat(pr.prNumber).isEqualTo("PR-0001")
        assertThat(pr.lines).hasSize(1)
        assertThat(pr.lines[0].productSku).isEqualTo("SKU-1")
    }

    @Test
    fun `create rejects a non-positive quantity`() {
        assertThatThrownBy {
            service.createPurchaseRequest(
                CreatePurchaseRequestRequest(lines = listOf(lineReq(quantity = BigDecimal.ZERO))),
                orgId,
                userId,
            )
        }.isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `submit moves a draft to submitted`() {
        whenever(repository.findById(java.util.UUID.fromString("0cf84710-b013-3270-ad01-fbdd0de0af26")))
            .thenReturn(Optional.of(approvedRequest().apply { status = PurchaseRequestStatus.DRAFT }))

        assertThat(
            service.submitPurchaseRequest(java.util.UUID.fromString("0cf84710-b013-3270-ad01-fbdd0de0af26"), orgId).status,
        ).isEqualTo(PurchaseRequestStatus.SUBMITTED)
    }

    @Test
    fun `approve rejects a request that is not submitted`() {
        whenever(
            repository.findById(java.util.UUID.fromString("0cf84710-b013-3270-ad01-fbdd0de0af26")),
        ).thenReturn(Optional.of(approvedRequest()))

        assertThatThrownBy {
            service.approvePurchaseRequest(
                java.util.UUID.fromString("0cf84710-b013-3270-ad01-fbdd0de0af26"),
                orgId,
                userId,
            )
        }.isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `cancel rejects a converted request`() {
        whenever(repository.findById(java.util.UUID.fromString("0cf84710-b013-3270-ad01-fbdd0de0af26")))
            .thenReturn(Optional.of(approvedRequest().apply { status = PurchaseRequestStatus.CONVERTED }))

        assertThatThrownBy { service.cancelPurchaseRequest(java.util.UUID.fromString("0cf84710-b013-3270-ad01-fbdd0de0af26"), orgId) }
            .isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `convert requires an approved request`() {
        whenever(repository.findById(java.util.UUID.fromString("0cf84710-b013-3270-ad01-fbdd0de0af26")))
            .thenReturn(Optional.of(approvedRequest().apply { status = PurchaseRequestStatus.SUBMITTED }))

        assertThatThrownBy {
            service.convertToPurchaseOrder(
                java.util.UUID.fromString("0cf84710-b013-3270-ad01-fbdd0de0af26"),
                ConvertPurchaseRequestRequest(),
                orgId,
                userId,
            )
        }.isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `convert builds a PO from estimated costs and marks the request converted`() {
        whenever(
            repository.findById(java.util.UUID.fromString("0cf84710-b013-3270-ad01-fbdd0de0af26")),
        ).thenReturn(Optional.of(approvedRequest()))
        val createdPo =
            PurchaseOrder(
                id = java.util.UUID.fromString("6dab366f-6306-3179-9dde-41e1ddd2888e"),
                poNumber = "PO-0001",
                vendorId = java.util.UUID.fromString("718fa2b3-0eb7-3a9c-987f-a0cbe216ac6a"),
                vendorName = "Acme",
                warehouseId = java.util.UUID.fromString("c91d2c12-b2b4-3634-a3bb-d0ff561af4ff"),
                orderDate = LocalDate.of(2026, 5, 1),
                organizationId = orgId,
                lines = emptyList(),
                totalAmount = BigDecimal("15"),
                createdBy = userId,
            )
        whenever(purchaseOrderService.createPurchaseOrder(any(), eq(orgId), eq(userId))).thenReturn(createdPo)

        val po =
            service.convertToPurchaseOrder(
                java.util.UUID.fromString("0cf84710-b013-3270-ad01-fbdd0de0af26"),
                ConvertPurchaseRequestRequest(),
                orgId,
                userId,
            )

        assertThat(po.id).isEqualTo(java.util.UUID.fromString("6dab366f-6306-3179-9dde-41e1ddd2888e"))
        val captor = argumentCaptor<CreatePurchaseOrderRequest>()
        org.mockito.kotlin
            .verify(purchaseOrderService)
            .createPurchaseOrder(captor.capture(), eq(orgId), eq(userId))
        assertThat(captor.firstValue.vendorId).isEqualTo(java.util.UUID.fromString("718fa2b3-0eb7-3a9c-987f-a0cbe216ac6a"))
        assertThat(captor.firstValue.warehouseId).isEqualTo(java.util.UUID.fromString("c91d2c12-b2b4-3634-a3bb-d0ff561af4ff"))
        assertThat(captor.firstValue.lines[0].unitCost).isEqualByComparingTo("5")

        val saved = argumentCaptor<PurchaseRequest>()
        org.mockito.kotlin
            .verify(repository)
            .save(saved.capture())
        assertThat(saved.firstValue.status).isEqualTo(PurchaseRequestStatus.CONVERTED)
        assertThat(saved.firstValue.convertedPurchaseOrderId).isEqualTo(java.util.UUID.fromString("6dab366f-6306-3179-9dde-41e1ddd2888e"))
    }

    @Test
    fun `convert fails when a line has no cost and no override`() {
        val noCostLine =
            PurchaseRequestLine(
                id = java.util.UUID.fromString("778ad073-53c6-3e1d-9228-4882f83295f9"),
                lineNumber = 1,
                productId = java.util.UUID.fromString("c2cf5eda-4c7a-30a7-9e0b-be843869ca89"),
                productSku = "SKU-1",
                productName = "Widget",
                quantity = BigDecimal("3"),
                estimatedUnitCost = null,
            )
        whenever(
            repository.findById(java.util.UUID.fromString("0cf84710-b013-3270-ad01-fbdd0de0af26")),
        ).thenReturn(Optional.of(approvedRequest(lines = listOf(noCostLine))))

        assertThatThrownBy {
            service.convertToPurchaseOrder(
                java.util.UUID.fromString("0cf84710-b013-3270-ad01-fbdd0de0af26"),
                ConvertPurchaseRequestRequest(),
                orgId,
                userId,
            )
        }.isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `convert uses a per-line cost override`() {
        val noCostLine =
            PurchaseRequestLine(
                id = java.util.UUID.fromString("778ad073-53c6-3e1d-9228-4882f83295f9"),
                lineNumber = 1,
                productId = java.util.UUID.fromString("c2cf5eda-4c7a-30a7-9e0b-be843869ca89"),
                productSku = "SKU-1",
                productName = "Widget",
                quantity = BigDecimal("3"),
                estimatedUnitCost = null,
            )
        whenever(
            repository.findById(java.util.UUID.fromString("0cf84710-b013-3270-ad01-fbdd0de0af26")),
        ).thenReturn(Optional.of(approvedRequest(lines = listOf(noCostLine))))
        whenever(purchaseOrderService.createPurchaseOrder(any(), eq(orgId), eq(userId))).thenAnswer {
            val req = it.arguments[0] as CreatePurchaseOrderRequest
            PurchaseOrder(
                id = java.util.UUID.fromString("6dab366f-6306-3179-9dde-41e1ddd2888e"),
                poNumber = "PO-0001",
                vendorId = java.util.UUID.fromString("718fa2b3-0eb7-3a9c-987f-a0cbe216ac6a"),
                vendorName = "Acme",
                warehouseId = java.util.UUID.fromString("c91d2c12-b2b4-3634-a3bb-d0ff561af4ff"),
                orderDate = LocalDate.of(2026, 5, 1),
                organizationId = orgId,
                lines = emptyList(),
                totalAmount = req.lines[0].unitCost!!.multiply(req.lines[0].quantity),
                createdBy = userId,
            )
        }

        service.convertToPurchaseOrder(
            java.util.UUID.fromString("0cf84710-b013-3270-ad01-fbdd0de0af26"),
            ConvertPurchaseRequestRequest(
                lineCosts =
                    listOf(
                        ConvertPurchaseRequestLineCost(
                            lineId = java.util.UUID.fromString("778ad073-53c6-3e1d-9228-4882f83295f9"),
                            unitCost = BigDecimal("7"),
                        ),
                    ),
            ),
            orgId,
            userId,
        )

        val captor = argumentCaptor<CreatePurchaseOrderRequest>()
        org.mockito.kotlin
            .verify(purchaseOrderService)
            .createPurchaseOrder(captor.capture(), eq(orgId), eq(userId))
        assertThat(captor.firstValue.lines[0].unitCost).isEqualByComparingTo("7")
    }

    @Test
    fun `get rejects cross-org access`() {
        whenever(repository.findById(java.util.UUID.fromString("0cf84710-b013-3270-ad01-fbdd0de0af26")))
            .thenReturn(
                Optional.of(
                    approvedRequest().apply {
                        organizationId =
                            java.util.UUID.fromString("f022a845-ae01-3e07-ae04-7fc0ffb096a8")
                    },
                ),
            )

        assertThatThrownBy { service.getPurchaseRequest(java.util.UUID.fromString("0cf84710-b013-3270-ad01-fbdd0de0af26"), orgId) }
            .isInstanceOf(ResourceNotFoundException::class.java)
    }
}
