package com.aquinofroilan.tessera.graphql

import com.aquinofroilan.tessera.controller.QuotationController
import com.aquinofroilan.tessera.dto.ConvertQuotationRequest
import com.aquinofroilan.tessera.dto.CreateQuotationRequest
import com.aquinofroilan.tessera.dto.RejectQuotationRequest
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

/**
 * GraphQL bridge for the sales quotation routes, delegating to the REST
 * controller via the shared JSON-scalar pass-through. Authorization mirrors the
 * REST endpoints (`sales:read`/`sales:write`).
 */
@Controller
class QuotationGraphqlController(
    private val quotationController: QuotationController,
    private val support: GraphqlBridgeSupport,
) {
    @QueryMapping
    @PreAuthorize("hasAuthority('sales:read')")
    fun quotations(
        @Argument status: String?,
        @Argument customerId: String?,
    ): Any = support.unwrap(quotationController.listQuotations(status, customerId))

    @QueryMapping
    @PreAuthorize("hasAuthority('sales:read')")
    fun quotation(
        @Argument id: String,
    ): Any = support.unwrap(quotationController.getQuotation(id))

    @MutationMapping
    @PreAuthorize("hasAuthority('sales:write')")
    fun createQuotation(
        @Argument input: Any,
    ): Any = support.unwrap(quotationController.createQuotation(support.toRequest<CreateQuotationRequest>(input)))

    @MutationMapping
    @PreAuthorize("hasAuthority('sales:write')")
    fun sendQuotation(
        @Argument id: String,
    ): Any = support.unwrap(quotationController.sendQuotation(id))

    @MutationMapping
    @PreAuthorize("hasAuthority('sales:write')")
    fun acceptQuotation(
        @Argument id: String,
    ): Any = support.unwrap(quotationController.acceptQuotation(id))

    @MutationMapping
    @PreAuthorize("hasAuthority('sales:write')")
    fun rejectQuotation(
        @Argument id: String,
        @Argument input: Any?,
    ): Any {
        val request = input?.let { support.toRequest<RejectQuotationRequest>(it) }
        return support.unwrap(quotationController.rejectQuotation(id, request))
    }

    @MutationMapping
    @PreAuthorize("hasAuthority('sales:write')")
    fun cancelQuotation(
        @Argument id: String,
    ): Any = support.unwrap(quotationController.cancelQuotation(id))

    @MutationMapping
    @PreAuthorize("hasAuthority('sales:write')")
    fun convertQuotation(
        @Argument id: String,
        @Argument input: Any?,
    ): Any {
        val request = input?.let { support.toRequest<ConvertQuotationRequest>(it) }
        return support.unwrap(quotationController.convertQuotation(id, request))
    }
}
