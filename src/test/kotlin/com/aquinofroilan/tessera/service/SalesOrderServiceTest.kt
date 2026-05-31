package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.CreateSalesOrderLineRequest
import com.aquinofroilan.tessera.dto.CreateSalesOrderRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.model.Customer
import com.aquinofroilan.tessera.model.Product
import com.aquinofroilan.tessera.model.SalesOrder
import com.aquinofroilan.tessera.model.SalesOrderStatus
import com.aquinofroilan.tessera.model.StockMovementType
import com.aquinofroilan.tessera.model.Warehouse
import com.aquinofroilan.tessera.repository.SalesOrderRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional

class SalesOrderServiceTest {
    private lateinit var repository: SalesOrderRepository
    private lateinit var customerService: CustomerService
    private lateinit var warehouseService: WarehouseService
    private lateinit var productService: ProductService
    private lateinit var stockMovementService: StockMovementService
    private lateinit var service: SalesOrderService

    private val orgId = "org-1"
    private val userId = "user-1"

    @BeforeEach
    fun setup() {
        repository = mock(SalesOrderRepository::class.java)
        customerService = mock(CustomerService::class.java)
        warehouseService = mock(WarehouseService::class.java)
        productService = mock(ProductService::class.java)
        stockMovementService = mock(StockMovementService::class.java)
        whenever(repository.countByOrganizationId(orgId)).thenReturn(0L)
        whenever(repository.save(any<SalesOrder>())).thenAnswer { it.arguments[0] }
        whenever(customerService.getCustomer("c-1", orgId)).thenReturn(Customer(id = "c-1", name = "Buyer", organizationId = orgId))
        whenever(warehouseService.getWarehouse("wh-1", orgId))
            .thenReturn(Warehouse(id = "wh-1", code = "MAIN", name = "Main", organizationId = orgId))
        whenever(productService.getProduct("p-1", orgId)).thenReturn(
            Product(id = "p-1", sku = "SKU-1", name = "Widget", listPrice = BigDecimal("9"), priceCurrency = "USD", organizationId = orgId),
        )
        service = SalesOrderService(repository, customerService, warehouseService, productService, stockMovementService)
    }

    private fun createRequest() =
        CreateSalesOrderRequest(
            customerId = "c-1",
            warehouseId = "wh-1",
            orderDate = LocalDate.of(2026, 5, 1),
            lines = listOf(CreateSalesOrderLineRequest(productId = "p-1", quantity = BigDecimal("4"), unitPrice = BigDecimal("9"))),
        )

    @Test
    fun `create computes totals and starts in DRAFT`() {
        val so = service.createSalesOrder(createRequest(), orgId, userId)

        assertThat(so.soNumber).isEqualTo("SO-0001")
        assertThat(so.status).isEqualTo(SalesOrderStatus.DRAFT)
        assertThat(so.lines.first().lineTotal).isEqualByComparingTo("36")
        assertThat(so.totalAmount).isEqualByComparingTo("36")
    }

    @Test
    fun `fulfill posts an ISSUE stock movement per line and marks FULFILLED`() {
        val approved = service.createSalesOrder(createRequest(), orgId, userId).copy(status = SalesOrderStatus.APPROVED)
        whenever(repository.findById(approved.id)).thenReturn(Optional.of(approved))

        val fulfilled = service.fulfillSalesOrder(approved.id, orgId, userId)

        assertThat(fulfilled.status).isEqualTo(SalesOrderStatus.FULFILLED)
        verify(stockMovementService, times(1)).createMovement(
            argThat { type == StockMovementType.ISSUE && warehouseId == "wh-1" && productId == "p-1" },
            eq(orgId),
            eq(userId),
        )
    }

    @Test
    fun `fulfill is rejected unless APPROVED`() {
        val draft = service.createSalesOrder(createRequest(), orgId, userId)
        whenever(repository.findById(draft.id)).thenReturn(Optional.of(draft))

        assertThatThrownBy { service.fulfillSalesOrder(draft.id, orgId, userId) }
            .isInstanceOf(BusinessRuleException::class.java)
        verify(stockMovementService, never()).createMovement(any(), any(), any())
    }
}
