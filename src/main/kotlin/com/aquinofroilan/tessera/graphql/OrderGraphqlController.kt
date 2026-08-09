package com.aquinofroilan.tessera.graphql

import com.aquinofroilan.tessera.controller.PurchaseOrderController
import com.aquinofroilan.tessera.controller.SalesOrderController
import com.aquinofroilan.tessera.dto.BillMatchRequest
import com.aquinofroilan.tessera.dto.CreatePurchaseOrderRequest
import com.aquinofroilan.tessera.dto.CreateSalesOrderRequest
import com.aquinofroilan.tessera.dto.FulfillSalesOrderRequest
import com.aquinofroilan.tessera.dto.GenerateBillRequest
import com.aquinofroilan.tessera.dto.GenerateInvoiceRequest
import com.aquinofroilan.tessera.dto.ReceivePurchaseOrderRequest
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

/**
 * GraphQL bridge for the procurement (purchase order) and sales order routes,
 * delegating to the existing REST controllers via the shared JSON-scalar
 * pass-through. Authorization mirrors the REST endpoints.
 */
@Controller
class OrderGraphqlController(
    private val purchaseOrderController: PurchaseOrderController,
    private val salesOrderController: SalesOrderController,
    private val support: GraphqlBridgeSupport,
) {
    @QueryMapping
    @PreAuthorize("hasAuthority('procurement:read')")
    fun purchaseOrders(
        @Argument status: String?,
        @Argument vendorId: java.util.UUID?,
    ): Any = support.unwrap(purchaseOrderController.listPurchaseOrders(status, vendorId))

    @QueryMapping
    @PreAuthorize("hasAuthority('procurement:read')")
    fun purchaseOrder(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(purchaseOrderController.getPurchaseOrder(id))

    @MutationMapping
    @PreAuthorize("hasAuthority('procurement:write')")
    fun createPurchaseOrder(
        @Argument input: Any,
    ): Any = support.unwrap(purchaseOrderController.createPurchaseOrder(support.toRequest<CreatePurchaseOrderRequest>(input)))

    @MutationMapping
    @PreAuthorize("hasAuthority('procurement:approve')")
    fun approvePurchaseOrder(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(purchaseOrderController.approvePurchaseOrder(id))

    @MutationMapping
    @PreAuthorize("hasAuthority('procurement:receive')")
    fun receivePurchaseOrder(
        @Argument id: java.util.UUID,
        @Argument input: Any?,
    ): Any =
        support.unwrap(purchaseOrderController.receivePurchaseOrder(id, input?.let { support.toRequest<ReceivePurchaseOrderRequest>(it) }))

    @MutationMapping
    @PreAuthorize("hasAuthority('procurement:read')")
    fun matchPurchaseOrderBill(
        @Argument id: java.util.UUID,
        @Argument input: Any,
    ): Any = support.unwrap(purchaseOrderController.matchBill(id, support.toRequest<BillMatchRequest>(input)))

    @MutationMapping
    @PreAuthorize("hasAuthority('procurement:receive')")
    fun generatePurchaseOrderBill(
        @Argument id: java.util.UUID,
        @Argument input: Any?,
    ): Any = support.unwrap(purchaseOrderController.generateBill(id, input?.let { support.toRequest<GenerateBillRequest>(it) }))

    @MutationMapping
    @PreAuthorize("hasAuthority('procurement:write')")
    fun closePurchaseOrder(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(purchaseOrderController.closePurchaseOrder(id))

    @MutationMapping
    @PreAuthorize("hasAuthority('procurement:write')")
    fun cancelPurchaseOrder(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(purchaseOrderController.cancelPurchaseOrder(id))

    @QueryMapping
    @PreAuthorize("hasAuthority('sales:read')")
    fun salesOrders(
        @Argument status: String?,
        @Argument customerId: java.util.UUID?,
    ): Any = support.unwrap(salesOrderController.listSalesOrders(status, customerId))

    @QueryMapping
    @PreAuthorize("hasAuthority('sales:read')")
    fun salesOrder(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(salesOrderController.getSalesOrder(id))

    @MutationMapping
    @PreAuthorize("hasAuthority('sales:write')")
    fun createSalesOrder(
        @Argument input: Any,
    ): Any = support.unwrap(salesOrderController.createSalesOrder(support.toRequest<CreateSalesOrderRequest>(input)))

    @MutationMapping
    @PreAuthorize("hasAuthority('sales:approve')")
    fun approveSalesOrder(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(salesOrderController.approveSalesOrder(id))

    @MutationMapping
    @PreAuthorize("hasAuthority('sales:fulfill')")
    fun fulfillSalesOrder(
        @Argument id: java.util.UUID,
        @Argument input: Any?,
    ): Any = support.unwrap(salesOrderController.fulfillSalesOrder(id, input?.let { support.toRequest<FulfillSalesOrderRequest>(it) }))

    @MutationMapping
    @PreAuthorize("hasAuthority('sales:fulfill')")
    fun generateSalesOrderInvoice(
        @Argument id: java.util.UUID,
        @Argument input: Any?,
    ): Any = support.unwrap(salesOrderController.generateInvoice(id, input?.let { support.toRequest<GenerateInvoiceRequest>(it) }))

    @MutationMapping
    @PreAuthorize("hasAuthority('sales:write')")
    fun closeSalesOrder(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(salesOrderController.closeSalesOrder(id))

    @MutationMapping
    @PreAuthorize("hasAuthority('sales:write')")
    fun cancelSalesOrder(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(salesOrderController.cancelSalesOrder(id))
}
