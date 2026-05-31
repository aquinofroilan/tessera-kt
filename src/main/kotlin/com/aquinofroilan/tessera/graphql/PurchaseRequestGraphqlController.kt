package com.aquinofroilan.tessera.graphql

import com.aquinofroilan.tessera.controller.PurchaseRequestController
import com.aquinofroilan.tessera.dto.ConvertPurchaseRequestRequest
import com.aquinofroilan.tessera.dto.CreatePurchaseRequestRequest
import com.aquinofroilan.tessera.dto.RejectPurchaseRequestRequest
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

/**
 * GraphQL bridge for the procurement purchase-request (requisition) routes,
 * delegating to the REST controller via the shared JSON-scalar pass-through.
 * Authorization mirrors the REST endpoints (`procurement:read`/`:write`/`:approve`).
 */
@Controller
class PurchaseRequestGraphqlController(
    private val purchaseRequestController: PurchaseRequestController,
    private val support: GraphqlBridgeSupport,
) {
    @QueryMapping
    @PreAuthorize("hasAuthority('procurement:read')")
    fun purchaseRequests(
        @Argument status: String?,
        @Argument requestedBy: String?,
    ): Any = support.unwrap(purchaseRequestController.listPurchaseRequests(status, requestedBy))

    @QueryMapping
    @PreAuthorize("hasAuthority('procurement:read')")
    fun purchaseRequest(
        @Argument id: String,
    ): Any = support.unwrap(purchaseRequestController.getPurchaseRequest(id))

    @MutationMapping
    @PreAuthorize("hasAuthority('procurement:write')")
    fun createPurchaseRequest(
        @Argument input: Any,
    ): Any = support.unwrap(purchaseRequestController.createPurchaseRequest(support.toRequest<CreatePurchaseRequestRequest>(input)))

    @MutationMapping
    @PreAuthorize("hasAuthority('procurement:write')")
    fun submitPurchaseRequest(
        @Argument id: String,
    ): Any = support.unwrap(purchaseRequestController.submitPurchaseRequest(id))

    @MutationMapping
    @PreAuthorize("hasAuthority('procurement:approve')")
    fun approvePurchaseRequest(
        @Argument id: String,
    ): Any = support.unwrap(purchaseRequestController.approvePurchaseRequest(id))

    @MutationMapping
    @PreAuthorize("hasAuthority('procurement:approve')")
    fun rejectPurchaseRequest(
        @Argument id: String,
        @Argument input: Any?,
    ): Any {
        val request = input?.let { support.toRequest<RejectPurchaseRequestRequest>(it) }
        return support.unwrap(purchaseRequestController.rejectPurchaseRequest(id, request))
    }

    @MutationMapping
    @PreAuthorize("hasAuthority('procurement:write')")
    fun cancelPurchaseRequest(
        @Argument id: String,
    ): Any = support.unwrap(purchaseRequestController.cancelPurchaseRequest(id))

    @MutationMapping
    @PreAuthorize("hasAuthority('procurement:write')")
    fun convertPurchaseRequest(
        @Argument id: String,
        @Argument input: Any?,
    ): Any {
        val request = input?.let { support.toRequest<ConvertPurchaseRequestRequest>(it) }
        return support.unwrap(purchaseRequestController.convertPurchaseRequest(id, request))
    }
}
