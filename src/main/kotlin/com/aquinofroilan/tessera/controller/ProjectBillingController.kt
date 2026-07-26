package com.aquinofroilan.tessera.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.dto.GenerateProjectInvoiceRequest
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.service.ProjectBillingService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/projects/{projectId}/billing")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class ProjectBillingController(
    private val projectBillingService: ProjectBillingService,
    private val invoiceController: InvoiceController,
    private val authContext: AuthenticationContext,
) {
    @PostMapping("/invoice")
    @PreAuthorize("hasAuthority('projects:write')")
    fun generateInvoice(
        @PathVariable projectId: java.util.UUID,
        @Valid @RequestBody(required = false) request: GenerateProjectInvoiceRequest?,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val createdBy = authContext.userId() ?: java.util.UUID.fromString("00000000-0000-0000-0000-000000000000")
        val invoice =
            projectBillingService.generateInvoice(
                projectId,
                request ?: GenerateProjectInvoiceRequest(),
                orgId,
                createdBy,
            )
        // Reuse the invoice controller's mapping for a consistent InvoiceResponse.
        return ResponseEntity.status(HttpStatus.CREATED).body(invoiceController.getInvoice(invoice.id).body)
    }
}
