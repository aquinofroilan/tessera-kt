package com.aquinofroilan.tessera.domain.finance.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.domain.finance.dto.CreateInvoiceRequest
import com.aquinofroilan.tessera.domain.finance.dto.InvoiceLineResponse
import com.aquinofroilan.tessera.domain.finance.dto.InvoiceReceiptResponse
import com.aquinofroilan.tessera.domain.finance.dto.InvoiceResponse
import com.aquinofroilan.tessera.domain.finance.dto.InvoiceSummaryResponse
import com.aquinofroilan.tessera.domain.finance.dto.RecordReceiptRequest
import com.aquinofroilan.tessera.domain.finance.dto.VoidInvoiceRequest
import com.aquinofroilan.tessera.domain.finance.model.Invoice
import com.aquinofroilan.tessera.domain.finance.model.InvoiceReceipt
import com.aquinofroilan.tessera.domain.finance.model.InvoiceStatus
import com.aquinofroilan.tessera.domain.finance.service.InvoiceService
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.security.CurrentOrganizationId
import com.aquinofroilan.tessera.security.CurrentUserId
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
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale
import java.util.UUID

@RestController
@RequestMapping("/api/v1/finance/ar/invoices")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class InvoiceController(
    private val invoiceService: InvoiceService,
    private val authContext: AuthenticationContext,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('ar:create')")
    fun createInvoice(
        @CurrentOrganizationId orgId: UUID,
        @CurrentUserId userId: UUID,
        @Valid @RequestBody request: CreateInvoiceRequest,
    ): ResponseEntity<Any> {
        val invoice = invoiceService.createInvoice(request, orgId, userId)
        return ResponseEntity.status(HttpStatus.CREATED).body(invoice.toResponse())
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ar:read')")
    fun listInvoices(
        @CurrentOrganizationId orgId: UUID,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) customerId: java.util.UUID?,
    ): ResponseEntity<Any> {
        val invoiceStatus =
            if (status != null) {
                try {
                    InvoiceStatus.valueOf(status.uppercase(Locale.ROOT))
                } catch (e: IllegalArgumentException) {
                    return ResponseEntity
                        .badRequest()
                        .body(mapOf("error" to "Invalid status '$status'"))
                }
            } else {
                null
            }

        val invoices = invoiceService.listInvoices(orgId, invoiceStatus, customerId)
        return ResponseEntity.ok(invoices.map { it.toSummaryResponse() })
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ar:read')")
    fun getInvoice(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> {
        val invoice = invoiceService.getInvoice(id, orgId)
        return ResponseEntity.ok(invoice.toResponse())
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('ar:approve')")
    fun approveInvoice(
        @CurrentUserId userId: UUID,
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> {
        val invoice = invoiceService.approveInvoice(id, orgId, userId)
        return ResponseEntity.ok(invoice.toResponse())
    }

    @PostMapping("/{id}/void")
    @PreAuthorize("hasAuthority('ar:void')")
    fun voidInvoice(
        @CurrentUserId userId: UUID,
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: java.util.UUID,
        @Valid @RequestBody request: VoidInvoiceRequest,
    ): ResponseEntity<Any> {
        val invoice = invoiceService.voidInvoice(id, orgId, request.reason, userId)
        return ResponseEntity.ok(invoice.toResponse())
    }

    @PostMapping("/{id}/receipts")
    @PreAuthorize("hasAuthority('ar:receive')")
    fun recordReceipt(
        @CurrentOrganizationId orgId: UUID,
        @CurrentUserId userId: UUID,
        @PathVariable id: java.util.UUID,
        @Valid @RequestBody request: RecordReceiptRequest,
    ): ResponseEntity<Any> {
        val receipt = invoiceService.recordReceipt(id, request, orgId, userId)
        return ResponseEntity.status(HttpStatus.CREATED).body(receipt.toResponse())
    }

    @GetMapping("/{id}/receipts")
    @PreAuthorize("hasAuthority('ar:read')")
    fun listReceipts(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> {
        val receipts = invoiceService.getReceipts(id, orgId)
        return ResponseEntity.ok(receipts.map { it.toResponse() })
    }

    @GetMapping("/aging")
    @PreAuthorize("hasAuthority('ar:read')")
    fun getAgingReport(
        @CurrentOrganizationId orgId: UUID,
        @RequestParam(required = false) asOfDate: LocalDate?,
    ): ResponseEntity<Any> {
        val report = invoiceService.getAgingReport(orgId, asOfDate ?: LocalDate.now(ZoneOffset.UTC))
        return ResponseEntity.ok(report)
    }

    private fun Invoice.toResponse() =
        InvoiceResponse(
            id = id,
            invoiceNumber = invoiceNumber,
            customerId = customerId,
            customerName = customerName,
            date = date.toString(),
            dueDate = dueDate.toString(),
            referenceNumber = referenceNumber,
            taxGroupId = taxGroupId,
            organizationId = organizationId,
            status = status.name,
            lines =
                lines.map { line ->
                    InvoiceLineResponse(
                        accountId = line.accountId,
                        accountCode = line.accountCode,
                        accountName = line.accountName,
                        amount = line.amount,
                        description = line.description,
                    )
                },
            totalAmount = totalAmount,
            taxAmount = taxAmount,
            amountReceived = amountReceived,
            currencyCode = currencyCode,
            exchangeRate = exchangeRate,
            baseCurrencyAmount = baseCurrencyAmount,
            baseCurrencyTaxAmount = baseCurrencyTaxAmount,
            baseCurrencyAmountReceived = baseCurrencyAmountReceived,
            journalEntryId = journalEntryId,
            createdBy = createdBy,
            approvedAt = approvedAt?.toString(),
            approvedBy = approvedBy,
            paidAt = paidAt?.toString(),
            voidedAt = voidedAt?.toString(),
            voidedBy = voidedBy,
            voidReason = voidReason,
            createdAt = createdAt?.toString(),
            updatedAt = updatedAt?.toString(),
        )

    private fun Invoice.toSummaryResponse() =
        InvoiceSummaryResponse(
            id = id,
            invoiceNumber = invoiceNumber,
            customerName = customerName,
            date = date.toString(),
            dueDate = dueDate.toString(),
            status = status.name,
            totalAmount = totalAmount,
            taxAmount = taxAmount,
            amountReceived = amountReceived,
            currencyCode = currencyCode,
        )

    private fun InvoiceReceipt.toResponse() =
        InvoiceReceiptResponse(
            id = id,
            invoiceId = invoiceId,
            receiptDate = receiptDate.toString(),
            amount = amount,
            baseCurrencyAmount = baseCurrencyAmount,
            exchangeRate = exchangeRate,
            paymentMethod = paymentMethod.name,
            referenceNumber = referenceNumber,
            journalEntryId = journalEntryId,
            createdBy = createdBy,
            createdAt = createdAt?.toString(),
        )
}
