package com.aquinofroilan.tessera.domain.project.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.domain.finance.controller.InvoiceController
import com.aquinofroilan.tessera.domain.platform.dto.GenerateProjectInvoiceRequest
import com.aquinofroilan.tessera.domain.project.service.ProjectBillingService
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.security.CurrentOrganizationId
import com.aquinofroilan.tessera.security.CurrentUserId
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/projects/{projectId}/billing")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class ProjectBillingController(
    private val projectBillingService: ProjectBillingService,
    private val invoiceController: InvoiceController,
    private val authContext: AuthenticationContext,
) {
    @PostMapping("/invoice")
    @PreAuthorize("hasAuthority('projects:write')")
    fun generateInvoice(
        @CurrentOrganizationId orgId: UUID,
        @CurrentUserId userId: UUID,
        @PathVariable projectId: java.util.UUID,
        @Valid @RequestBody(required = false) request: GenerateProjectInvoiceRequest?,
    ): ResponseEntity<Any> {
        val invoice =
            projectBillingService.generateInvoice(
                projectId,
                request ?: GenerateProjectInvoiceRequest(),
                orgId,
                userId,
            )
        return ResponseEntity.status(HttpStatus.CREATED).body(invoiceController.getInvoice(orgId, invoice.id).body)
    }
}
