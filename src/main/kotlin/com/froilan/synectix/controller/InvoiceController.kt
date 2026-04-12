package com.froilan.synectix.controller

import com.froilan.synectix.annotation.LogLevel
import com.froilan.synectix.annotation.Loggable
import com.froilan.synectix.dto.CreateInvoiceRequest
import com.froilan.synectix.dto.InvoiceLineResponse
import com.froilan.synectix.dto.InvoiceReceiptResponse
import com.froilan.synectix.dto.InvoiceResponse
import com.froilan.synectix.dto.InvoiceSummaryResponse
import com.froilan.synectix.dto.RecordReceiptRequest
import com.froilan.synectix.dto.VoidInvoiceRequest
import com.froilan.synectix.model.Invoice
import com.froilan.synectix.model.InvoiceReceipt
import com.froilan.synectix.model.InvoiceStatus
import com.froilan.synectix.security.AuthenticationContext
import com.froilan.synectix.service.InvoiceService
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

@RestController
@RequestMapping("/finance/ar/invoices")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class InvoiceController(
    private val invoiceService: InvoiceService,
    private val authContext: AuthenticationContext,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('ar:create')")
    fun createInvoice(
        @Valid @RequestBody request: CreateInvoiceRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return unauthorized()
        val createdBy = authContext.userId() ?: "api-key"

        return try {
            val invoice = invoiceService.createInvoice(request, orgId, createdBy)
            ResponseEntity.status(HttpStatus.CREATED).body(invoice.toResponse())
        } catch (e: IllegalArgumentException) {
            ResponseEntity
                .badRequest()
                .body(mapOf("error" to (e.message ?: "Failed to create invoice")))
        }
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ar:read')")
    fun listInvoices(
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) customerId: String?,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return unauthorized()

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
        @PathVariable id: String,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return unauthorized()

        return try {
            val invoice = invoiceService.getInvoice(id, orgId)
            ResponseEntity.ok(invoice.toResponse())
        } catch (e: IllegalArgumentException) {
            ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to (e.message ?: "Invoice not found")))
        }
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('ar:approve')")
    fun approveInvoice(
        @PathVariable id: String,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return unauthorized()
        val userId = authContext.userId() ?: "api-key"

        return try {
            val invoice = invoiceService.approveInvoice(id, orgId, userId)
            ResponseEntity.ok(invoice.toResponse())
        } catch (e: IllegalArgumentException) {
            val status =
                if (e.message == "Invoice not found") HttpStatus.NOT_FOUND else HttpStatus.BAD_REQUEST
            ResponseEntity
                .status(status)
                .body(mapOf("error" to (e.message ?: "Failed to approve invoice")))
        } catch (e: IllegalStateException) {
            ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(mapOf("error" to (e.message ?: "Failed to approve invoice")))
        }
    }

    @PostMapping("/{id}/void")
    @PreAuthorize("hasAuthority('ar:void')")
    fun voidInvoice(
        @PathVariable id: String,
        @Valid @RequestBody request: VoidInvoiceRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return unauthorized()
        val userId = authContext.userId() ?: "api-key"

        return try {
            val invoice = invoiceService.voidInvoice(id, orgId, request.reason, userId)
            ResponseEntity.ok(invoice.toResponse())
        } catch (e: IllegalArgumentException) {
            val status =
                if (e.message == "Invoice not found") HttpStatus.NOT_FOUND else HttpStatus.BAD_REQUEST
            ResponseEntity
                .status(status)
                .body(mapOf("error" to (e.message ?: "Failed to void invoice")))
        } catch (e: IllegalStateException) {
            ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(mapOf("error" to (e.message ?: "Failed to void invoice")))
        }
    }

    @PostMapping("/{id}/receipts")
    @PreAuthorize("hasAuthority('ar:receive')")
    fun recordReceipt(
        @PathVariable id: String,
        @Valid @RequestBody request: RecordReceiptRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return unauthorized()
        val createdBy = authContext.userId() ?: "api-key"

        return try {
            val receipt = invoiceService.recordReceipt(id, request, orgId, createdBy)
            ResponseEntity.status(HttpStatus.CREATED).body(receipt.toResponse())
        } catch (e: IllegalArgumentException) {
            val status =
                if (e.message == "Invoice not found") HttpStatus.NOT_FOUND else HttpStatus.BAD_REQUEST
            ResponseEntity
                .status(status)
                .body(mapOf("error" to (e.message ?: "Failed to record receipt")))
        } catch (e: IllegalStateException) {
            ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(mapOf("error" to (e.message ?: "Failed to record receipt")))
        }
    }

    @GetMapping("/{id}/receipts")
    @PreAuthorize("hasAuthority('ar:read')")
    fun listReceipts(
        @PathVariable id: String,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return unauthorized()

        return try {
            val receipts = invoiceService.getReceipts(id, orgId)
            ResponseEntity.ok(receipts.map { it.toResponse() })
        } catch (e: IllegalArgumentException) {
            ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to (e.message ?: "Invoice not found")))
        }
    }

    @GetMapping("/aging")
    @PreAuthorize("hasAuthority('ar:read')")
    fun getAgingReport(
        @RequestParam(required = false) asOfDate: LocalDate?,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return unauthorized()
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
            amountReceived = amountReceived,
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
            amountReceived = amountReceived,
        )

    private fun InvoiceReceipt.toResponse() =
        InvoiceReceiptResponse(
            id = id,
            invoiceId = invoiceId,
            receiptDate = receiptDate.toString(),
            amount = amount,
            paymentMethod = paymentMethod.name,
            referenceNumber = referenceNumber,
            journalEntryId = journalEntryId,
            createdBy = createdBy,
            createdAt = createdAt?.toString(),
        )

    private fun unauthorized(): ResponseEntity<Any> =
        ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(mapOf("error" to "Authentication required"))
}
