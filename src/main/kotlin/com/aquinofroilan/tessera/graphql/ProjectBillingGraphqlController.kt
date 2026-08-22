package com.aquinofroilan.tessera.graphql

import com.aquinofroilan.tessera.domain.platform.dto.GenerateProjectInvoiceRequest
import com.aquinofroilan.tessera.domain.project.controller.ProjectBillingController
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

/**
 * GraphQL bridge for project billing, delegating to the REST controller via the
 * shared JSON-scalar pass-through. Authorization mirrors the REST endpoint
 * (`projects:write`).
 */
@Controller
class ProjectBillingGraphqlController(
    private val projectBillingController: ProjectBillingController,
    private val support: GraphqlBridgeSupport,
) {
    @MutationMapping
    @PreAuthorize("hasAuthority('projects:write')")
    fun generateProjectInvoice(
        @Argument projectId: java.util.UUID,
        @Argument input: Any?,
    ): Any {
        val request = input?.let { support.toRequest<GenerateProjectInvoiceRequest>(it) }
        return support.unwrap(projectBillingController.generateInvoice(projectId, request))
    }
}
