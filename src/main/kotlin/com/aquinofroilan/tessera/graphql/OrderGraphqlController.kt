package com.aquinofroilan.tessera.graphql

import com.aquinofroilan.tessera.domain.procurement.controller.PurchaseOrderController
import com.aquinofroilan.tessera.domain.procurement.dto.BillMatchRequest
import com.aquinofroilan.tessera.domain.procurement.dto.CreatePurchaseOrderRequest
import com.aquinofroilan.tessera.domain.procurement.dto.GenerateBillRequest
import com.aquinofroilan.tessera.domain.procurement.dto.ReceivePurchaseOrderRequest
import com.aquinofroilan.tessera.domain.sales.controller.SalesOrderController
import com.aquinofroilan.tessera.domain.sales.dto.CreateSalesOrderRequest
import com.aquinofroilan.tessera.domain.sales.dto.FulfillSalesOrderRequest
import com.aquinofroilan.tessera.domain.sales.dto.GenerateInvoiceRequest
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
    ): Any = support.unwrap(purchaseOrderController.listPurchaseOrders(support.orgId(), status, vendorId))

    @QueryMapping
    @PreAuthorize("hasAuthority('procurement:read')")
    fun purchaseOrder(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(purchaseOrderController.getPurchaseOrder(support.orgId(), id))

    @MutationMapping
    @PreAuthorize("hasAuthority('procurement:write')")
    fun createPurchaseOrder(
        @Argument input: Any,
    ): Any =
        support.unwrap(
            purchaseOrderController.createPurchaseOrder(
                support.orgId(),
                support.userId(),
                support.toRequest<CreatePurchaseOrderRequest>(input),
            ),
        )

    @MutationMapping
    @PreAuthorize("hasAuthority('procurement:approve')")
    fun approvePurchaseOrder(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(purchaseOrderController.approvePurchaseOrder(support.orgId(), support.userId(), id))

    @MutationMapping
    @PreAuthorize("hasAuthority('procurement:receive')")
    fun receivePurchaseOrder(
        @Argument id: java.util.UUID,
        @Argument input: Any?,
    ): Any =
        support.unwrap(
            purchaseOrderController.receivePurchaseOrder(
                support.userId(),
                support.orgId(),
                id,
                input?.let {
                    support.toRequest<ReceivePurchaseOrderRequest>(it)
                },
            ),
        )

    @MutationMapping
    @PreAuthorize("hasAuthority('procurement:read')")
    fun matchPurchaseOrderBill(
        @Argument id: java.util.UUID,
        @Argument input: Any,
    ): Any = support.unwrap(purchaseOrderController.matchBill(support.orgId(), id, support.toRequest<BillMatchRequest>(input)))

    @MutationMapping
    @PreAuthorize("hasAuthority('procurement:receive')")
    fun generatePurchaseOrderBill(
        @Argument id: java.util.UUID,
        @Argument input: Any?,
    ): Any =
        support.unwrap(
            purchaseOrderController.generateBill(
                support.orgId(),
                support.userId(),
                id,
                input?.let {
                    support.toRequest<GenerateBillRequest>(it)
                },
            ),
        )

    @MutationMapping
    @PreAuthorize("hasAuthority('procurement:write')")
    fun closePurchaseOrder(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(purchaseOrderController.closePurchaseOrder(support.orgId(), id))

    @MutationMapping
    @PreAuthorize("hasAuthority('procurement:write')")
    fun cancelPurchaseOrder(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(purchaseOrderController.cancelPurchaseOrder(support.userId(), support.orgId(), id))

    @QueryMapping
    @PreAuthorize("hasAuthority('sales:read')")
    fun salesOrders(
        @Argument status: String?,
        @Argument customerId: java.util.UUID?,
    ): Any = support.unwrap(salesOrderController.listSalesOrders(support.orgId(), status, customerId))

    @QueryMapping
    @PreAuthorize("hasAuthority('sales:read')")
    fun salesOrder(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(salesOrderController.getSalesOrder(support.orgId(), id))

    @MutationMapping
    @PreAuthorize("hasAuthority('sales:write')")
    fun createSalesOrder(
        @Argument input: Any,
    ): Any =
        support.unwrap(
            salesOrderController.createSalesOrder(support.orgId(), support.userId(), support.toRequest<CreateSalesOrderRequest>(input)),
        )

    @MutationMapping
    @PreAuthorize("hasAuthority('sales:approve')")
    fun approveSalesOrder(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(salesOrderController.approveSalesOrder(support.orgId(), support.userId(), id))

    @MutationMapping
    @PreAuthorize("hasAuthority('sales:fulfill')")
    fun fulfillSalesOrder(
        @Argument id: java.util.UUID,
        @Argument input: Any?,
    ): Any =
        support.unwrap(
            salesOrderController.fulfillSalesOrder(
                support.userId(),
                support.orgId(),
                id,
                input?.let {
                    support.toRequest<FulfillSalesOrderRequest>(it)
                },
            ),
        )

    @MutationMapping
    @PreAuthorize("hasAuthority('sales:fulfill')")
    fun generateSalesOrderInvoice(
        @Argument id: java.util.UUID,
        @Argument input: Any?,
    ): Any =
        support.unwrap(
            salesOrderController.generateInvoice(
                support.orgId(),
                support.userId(),
                id,
                input?.let {
                    support.toRequest<GenerateInvoiceRequest>(it)
                },
            ),
        )

    @MutationMapping
    @PreAuthorize("hasAuthority('sales:write')")
    fun closeSalesOrder(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(salesOrderController.closeSalesOrder(support.orgId(), id))

    @MutationMapping
    @PreAuthorize("hasAuthority('sales:write')")
    fun cancelSalesOrder(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(salesOrderController.cancelSalesOrder(support.userId(), support.orgId(), id))
}
