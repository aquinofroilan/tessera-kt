package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.ConvertPurchaseRequestLineCost
import com.aquinofroilan.tessera.dto.ConvertPurchaseRequestRequest
import com.aquinofroilan.tessera.dto.CreatePurchaseOrderRequest
import com.aquinofroilan.tessera.dto.CreatePurchaseRequestLineRequest
import com.aquinofroilan.tessera.dto.CreatePurchaseRequestRequest
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

class PurchaseRequestServiceTest {
    private lateinit var repository: PurchaseRequestRepository
    private lateinit var productService: ProductService
    private lateinit var vendorService: VendorService
    private lateinit var warehouseService: WarehouseService
    private lateinit var purchaseOrderService: PurchaseOrderService
    private lateinit var service: PurchaseRequestService

    private val orgId = "org-1"
    private val userId = "user-1"

    @BeforeEach
    fun setup() {
        repository = mock(PurchaseRequestRepository::class.java)
        productService = mock(ProductService::class.java)
        vendorService = mock(VendorService::class.java)
        warehouseService = mock(WarehouseService::class.java)
        purchaseOrderService = mock(PurchaseOrderService::class.java)
        whenever(repository.countByOrganizationId(orgId)).thenReturn(0L)
        whenever(repository.save(any<PurchaseRequest>())).thenAnswer { it.arguments[0] }
        whenever(productService.getProduct("p-1", orgId)).thenReturn(
            Product(id = "p-1", sku = "SKU-1", name = "Widget", listPrice = BigDecimal("9"), priceCurrency = "USD", organizationId = orgId),
        )
        whenever(vendorService.getVendor("v-1", orgId)).thenReturn(Vendor(id = "v-1", name = "Acme", organizationId = orgId))
        whenever(warehouseService.getWarehouse("wh-1", orgId))
            .thenReturn(Warehouse(id = "wh-1", code = "MAIN", name = "Main", organizationId = orgId))
        service = PurchaseRequestService(repository, productService, vendorService, warehouseService, purchaseOrderService)
    }

    private fun lineReq(estimatedUnitCost: BigDecimal? = BigDecimal("5")) =
        CreatePurchaseRequestLineRequest(productId = "p-1", quantity = BigDecimal("3"), estimatedUnitCost = estimatedUnitCost)

    private fun approvedRequest(
        lines: List<PurchaseRequestLine> =
            listOf(
                PurchaseRequestLine(
                    id = "prl-1",
                    lineNumber = 1,
                    productId = "p-1",
                    productSku = "SKU-1",
                    productName = "Widget",
                    quantity = BigDecimal("3"),
                    estimatedUnitCost = BigDecimal("5"),
                ),
            ),
        suggestedVendorId: String? = "v-1",
        warehouseId: String? = "wh-1",
    ) = PurchaseRequest(
        id = "pr-1",
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
                CreatePurchaseRequestRequest(suggestedVendorId = "v-1", warehouseId = "wh-1", lines = listOf(lineReq())),
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
                CreatePurchaseRequestRequest(lines = listOf(lineReq().copy(quantity = BigDecimal.ZERO))),
                orgId,
                userId,
            )
        }.isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `submit moves a draft to submitted`() {
        whenever(repository.findById("pr-1"))
            .thenReturn(Optional.of(approvedRequest().copy(status = PurchaseRequestStatus.DRAFT)))

        assertThat(service.submitPurchaseRequest("pr-1", orgId).status).isEqualTo(PurchaseRequestStatus.SUBMITTED)
    }

    @Test
    fun `approve rejects a request that is not submitted`() {
        whenever(repository.findById("pr-1")).thenReturn(Optional.of(approvedRequest()))

        assertThatThrownBy { service.approvePurchaseRequest("pr-1", orgId, userId) }
            .isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `cancel rejects a converted request`() {
        whenever(repository.findById("pr-1"))
            .thenReturn(Optional.of(approvedRequest().copy(status = PurchaseRequestStatus.CONVERTED)))

        assertThatThrownBy { service.cancelPurchaseRequest("pr-1", orgId) }
            .isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `convert requires an approved request`() {
        whenever(repository.findById("pr-1"))
            .thenReturn(Optional.of(approvedRequest().copy(status = PurchaseRequestStatus.SUBMITTED)))

        assertThatThrownBy { service.convertToPurchaseOrder("pr-1", ConvertPurchaseRequestRequest(), orgId, userId) }
            .isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `convert builds a PO from estimated costs and marks the request converted`() {
        whenever(repository.findById("pr-1")).thenReturn(Optional.of(approvedRequest()))
        val createdPo =
            PurchaseOrder(
                id = "po-9",
                poNumber = "PO-0001",
                vendorId = "v-1",
                vendorName = "Acme",
                warehouseId = "wh-1",
                orderDate = LocalDate.of(2026, 5, 1),
                organizationId = orgId,
                lines = emptyList(),
                totalAmount = BigDecimal("15"),
                createdBy = userId,
            )
        whenever(purchaseOrderService.createPurchaseOrder(any(), eq(orgId), eq(userId))).thenReturn(createdPo)

        val po = service.convertToPurchaseOrder("pr-1", ConvertPurchaseRequestRequest(), orgId, userId)

        assertThat(po.id).isEqualTo("po-9")
        val captor = argumentCaptor<CreatePurchaseOrderRequest>()
        org.mockito.kotlin
            .verify(purchaseOrderService)
            .createPurchaseOrder(captor.capture(), eq(orgId), eq(userId))
        assertThat(captor.firstValue.vendorId).isEqualTo("v-1")
        assertThat(captor.firstValue.warehouseId).isEqualTo("wh-1")
        assertThat(captor.firstValue.lines[0].unitCost).isEqualByComparingTo("5")

        val saved = argumentCaptor<PurchaseRequest>()
        org.mockito.kotlin
            .verify(repository)
            .save(saved.capture())
        assertThat(saved.firstValue.status).isEqualTo(PurchaseRequestStatus.CONVERTED)
        assertThat(saved.firstValue.convertedPurchaseOrderId).isEqualTo("po-9")
    }

    @Test
    fun `convert fails when a line has no cost and no override`() {
        val noCostLine =
            PurchaseRequestLine(
                id = "prl-1",
                lineNumber = 1,
                productId = "p-1",
                productSku = "SKU-1",
                productName = "Widget",
                quantity = BigDecimal("3"),
                estimatedUnitCost = null,
            )
        whenever(repository.findById("pr-1")).thenReturn(Optional.of(approvedRequest(lines = listOf(noCostLine))))

        assertThatThrownBy { service.convertToPurchaseOrder("pr-1", ConvertPurchaseRequestRequest(), orgId, userId) }
            .isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `convert uses a per-line cost override`() {
        val noCostLine =
            PurchaseRequestLine(
                id = "prl-1",
                lineNumber = 1,
                productId = "p-1",
                productSku = "SKU-1",
                productName = "Widget",
                quantity = BigDecimal("3"),
                estimatedUnitCost = null,
            )
        whenever(repository.findById("pr-1")).thenReturn(Optional.of(approvedRequest(lines = listOf(noCostLine))))
        whenever(purchaseOrderService.createPurchaseOrder(any(), eq(orgId), eq(userId))).thenAnswer {
            val req = it.arguments[0] as CreatePurchaseOrderRequest
            PurchaseOrder(
                id = "po-9",
                poNumber = "PO-0001",
                vendorId = "v-1",
                vendorName = "Acme",
                warehouseId = "wh-1",
                orderDate = LocalDate.of(2026, 5, 1),
                organizationId = orgId,
                lines = emptyList(),
                totalAmount = req.lines[0].unitCost!!.multiply(req.lines[0].quantity),
                createdBy = userId,
            )
        }

        service.convertToPurchaseOrder(
            "pr-1",
            ConvertPurchaseRequestRequest(lineCosts = listOf(ConvertPurchaseRequestLineCost(lineId = "prl-1", unitCost = BigDecimal("7")))),
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
        whenever(repository.findById("pr-1"))
            .thenReturn(Optional.of(approvedRequest().copy(organizationId = "other")))

        assertThatThrownBy { service.getPurchaseRequest("pr-1", orgId) }
            .isInstanceOf(ResourceNotFoundException::class.java)
    }
}
