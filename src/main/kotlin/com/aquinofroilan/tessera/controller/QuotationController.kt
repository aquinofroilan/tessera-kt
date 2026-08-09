package com.aquinofroilan.tessera.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.dto.ConvertQuotationRequest
import com.aquinofroilan.tessera.dto.CreateQuotationRequest
import com.aquinofroilan.tessera.dto.QuotationResponse
import com.aquinofroilan.tessera.dto.RejectQuotationRequest
import com.aquinofroilan.tessera.dto.SalesOrderResponse
import com.aquinofroilan.tessera.model.QuotationStatus
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.service.QuotationService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.Locale

@RestController
@RequestMapping("/sales/quotations")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class QuotationController(
    private val quotationService: QuotationService,
    private val authContext: AuthenticationContext,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('sales:write')")
    fun createQuotation(
        @Valid @RequestBody request: CreateQuotationRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val createdBy = authContext.userId() ?: java.util.UUID.fromString("00000000-0000-0000-0000-000000000000")
        val created = quotationService.createQuotation(request, orgId, createdBy)
        return ResponseEntity.status(HttpStatus.CREATED).body(QuotationResponse.from(created))
    }

    @GetMapping
    @PreAuthorize("hasAuthority('sales:read')")
    fun listQuotations(
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) customerId: java.util.UUID?,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val quoteStatus =
            if (status != null) {
                try {
                    QuotationStatus.valueOf(status.uppercase(Locale.ROOT))
                } catch (e: IllegalArgumentException) {
                    return ResponseEntity.badRequest().body(mapOf("error" to "Invalid status '$status'"))
                }
            } else {
                null
            }
        return ResponseEntity.ok(
            quotationService.listQuotations(orgId, quoteStatus, customerId).map { QuotationResponse.from(it) },
        )
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('sales:read')")
    fun getQuotation(
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(QuotationResponse.from(quotationService.getQuotation(id, orgId)))
    }

    @PostMapping("/{id}/send")
    @PreAuthorize("hasAuthority('sales:write')")
    fun sendQuotation(
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(QuotationResponse.from(quotationService.sendQuotation(id, orgId)))
    }

    @PostMapping("/{id}/accept")
    @PreAuthorize("hasAuthority('sales:write')")
    fun acceptQuotation(
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(QuotationResponse.from(quotationService.acceptQuotation(id, orgId)))
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('sales:write')")
    fun rejectQuotation(
        @PathVariable id: java.util.UUID,
        @RequestBody(required = false) request: RejectQuotationRequest?,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(QuotationResponse.from(quotationService.rejectQuotation(id, request?.reason, orgId)))
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('sales:write')")
    fun cancelQuotation(
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(QuotationResponse.from(quotationService.cancelQuotation(id, orgId)))
    }

    @PostMapping("/{id}/convert")
    @PreAuthorize("hasAuthority('sales:write')")
    fun convertQuotation(
        @PathVariable id: java.util.UUID,
        @Valid @RequestBody(required = false) request: ConvertQuotationRequest?,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val createdBy = authContext.userId() ?: java.util.UUID.fromString("00000000-0000-0000-0000-000000000000")
        val salesOrder = quotationService.convertToSalesOrder(id, request ?: ConvertQuotationRequest(), orgId, createdBy)
        return ResponseEntity.status(HttpStatus.CREATED).body(SalesOrderResponse.from(salesOrder))
    }
}
