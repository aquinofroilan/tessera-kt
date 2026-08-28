package com.aquinofroilan.tessera.graphql

import com.aquinofroilan.tessera.domain.sales.controller.QuotationController
import com.aquinofroilan.tessera.domain.sales.dto.ConvertQuotationRequest
import com.aquinofroilan.tessera.domain.sales.dto.CreateQuotationRequest
import com.aquinofroilan.tessera.domain.sales.dto.RejectQuotationRequest
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
        @Argument customerId: java.util.UUID?,
    ): Any = support.unwrap(quotationController.listQuotations(support.orgId(), status, customerId))

    @QueryMapping
    @PreAuthorize("hasAuthority('sales:read')")
    fun quotation(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(quotationController.getQuotation(support.orgId(), id))

    @MutationMapping
    @PreAuthorize("hasAuthority('sales:write')")
    fun createQuotation(
        @Argument input: Any,
    ): Any =
        support.unwrap(
            quotationController.createQuotation(support.orgId(), support.userId(), support.toRequest<CreateQuotationRequest>(input)),
        )

    @MutationMapping
    @PreAuthorize("hasAuthority('sales:write')")
    fun sendQuotation(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(quotationController.sendQuotation(support.orgId(), id))

    @MutationMapping
    @PreAuthorize("hasAuthority('sales:write')")
    fun acceptQuotation(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(quotationController.acceptQuotation(support.orgId(), id))

    @MutationMapping
    @PreAuthorize("hasAuthority('sales:write')")
    fun rejectQuotation(
        @Argument id: java.util.UUID,
        @Argument input: Any?,
    ): Any {
        val request = input?.let { support.toRequest<RejectQuotationRequest>(it) }
        return support.unwrap(quotationController.rejectQuotation(support.orgId(), id, request))
    }

    @MutationMapping
    @PreAuthorize("hasAuthority('sales:write')")
    fun cancelQuotation(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(quotationController.cancelQuotation(support.orgId(), id))

    @MutationMapping
    @PreAuthorize("hasAuthority('sales:write')")
    fun convertQuotation(
        @Argument id: java.util.UUID,
        @Argument input: Any?,
    ): Any {
        val request = input?.let { support.toRequest<ConvertQuotationRequest>(it) }
        return support.unwrap(quotationController.convertQuotation(support.orgId(), support.userId(), id, request))
    }
}
