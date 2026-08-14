package com.aquinofroilan.tessera.domain.finance.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.domain.finance.dto.BillLineResponse
import com.aquinofroilan.tessera.domain.finance.dto.BillPaymentResponse
import com.aquinofroilan.tessera.domain.finance.dto.BillResponse
import com.aquinofroilan.tessera.domain.finance.dto.BillSummaryResponse
import com.aquinofroilan.tessera.domain.finance.dto.CreateBillRequest
import com.aquinofroilan.tessera.domain.finance.dto.RecordPaymentRequest
import com.aquinofroilan.tessera.domain.finance.dto.VoidBillRequest
import com.aquinofroilan.tessera.domain.finance.model.Bill
import com.aquinofroilan.tessera.domain.finance.model.BillPayment
import com.aquinofroilan.tessera.domain.finance.model.BillStatus
import com.aquinofroilan.tessera.domain.finance.service.BillService
import com.aquinofroilan.tessera.security.AuthenticationContext
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
@RequestMapping("/api/v1/finance/ap/bills")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class BillController(
    private val billService: BillService,
    private val authContext: AuthenticationContext,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('ap:create')")
    fun createBill(
        @Valid @RequestBody request: CreateBillRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val createdBy = authContext.userId() ?: java.util.UUID.fromString("00000000-0000-0000-0000-000000000000")

        val bill = billService.createBill(request, orgId, createdBy)
        return ResponseEntity.status(HttpStatus.CREATED).body(bill.toResponse())
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ap:read')")
    fun listBills(
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) vendorId: java.util.UUID?,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()

        val billStatus =
            if (status != null) {
                try {
                    BillStatus.valueOf(status.uppercase(Locale.ROOT))
                } catch (e: IllegalArgumentException) {
                    return ResponseEntity
                        .badRequest()
                        .body(mapOf("error" to "Invalid status '$status'"))
                }
            } else {
                null
            }

        val bills = billService.listBills(orgId, billStatus, vendorId)
        return ResponseEntity.ok(bills.map { it.toSummaryResponse() })
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ap:read')")
    fun getBill(
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()

        val bill = billService.getBill(id, orgId)
        return ResponseEntity.ok(bill.toResponse())
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('ap:approve')")
    fun approveBill(
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val userId = authContext.userId() ?: java.util.UUID.fromString("00000000-0000-0000-0000-000000000000")

        val bill = billService.approveBill(id, orgId, userId)
        return ResponseEntity.ok(bill.toResponse())
    }

    @PostMapping("/{id}/void")
    @PreAuthorize("hasAuthority('ap:void')")
    fun voidBill(
        @PathVariable id: java.util.UUID,
        @Valid @RequestBody request: VoidBillRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val userId = authContext.userId() ?: java.util.UUID.fromString("00000000-0000-0000-0000-000000000000")

        val bill = billService.voidBill(id, orgId, request.reason, userId)
        return ResponseEntity.ok(bill.toResponse())
    }

    @PostMapping("/{id}/payments")
    @PreAuthorize("hasAuthority('ap:pay')")
    fun recordPayment(
        @PathVariable id: java.util.UUID,
        @Valid @RequestBody request: RecordPaymentRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val createdBy = authContext.userId() ?: java.util.UUID.fromString("00000000-0000-0000-0000-000000000000")

        val payment = billService.recordPayment(id, request, orgId, createdBy)
        return ResponseEntity.status(HttpStatus.CREATED).body(payment.toResponse())
    }

    @GetMapping("/{id}/payments")
    @PreAuthorize("hasAuthority('ap:read')")
    fun listPayments(
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()

        val payments = billService.getPayments(id, orgId)
        return ResponseEntity.ok(payments.map { it.toResponse() })
    }

    @GetMapping("/aging")
    @PreAuthorize("hasAuthority('ap:read')")
    fun getAgingReport(
        @RequestParam(required = false) asOfDate: LocalDate?,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val report = billService.getAgingReport(orgId, asOfDate ?: LocalDate.now(ZoneOffset.UTC))
        return ResponseEntity.ok(report)
    }

    private fun Bill.toResponse() =
        BillResponse(
            id = id,
            billNumber = billNumber,
            vendorId = vendorId,
            vendorName = vendorName,
            date = date.toString(),
            dueDate = dueDate.toString(),
            referenceNumber = referenceNumber,
            taxGroupId = taxGroupId,
            organizationId = organizationId,
            status = status.name,
            lines =
                lines.map { line ->
                    BillLineResponse(
                        accountId = line.accountId,
                        accountCode = line.accountCode,
                        accountName = line.accountName,
                        amount = line.amount,
                        description = line.description,
                    )
                },
            totalAmount = totalAmount,
            taxAmount = taxAmount,
            amountPaid = amountPaid,
            currencyCode = currencyCode,
            exchangeRate = exchangeRate,
            baseCurrencyAmount = baseCurrencyAmount,
            baseCurrencyTaxAmount = baseCurrencyTaxAmount,
            baseCurrencyAmountPaid = baseCurrencyAmountPaid,
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

    private fun Bill.toSummaryResponse() =
        BillSummaryResponse(
            id = id,
            billNumber = billNumber,
            vendorName = vendorName,
            date = date.toString(),
            dueDate = dueDate.toString(),
            status = status.name,
            totalAmount = totalAmount,
            taxAmount = taxAmount,
            amountPaid = amountPaid,
            currencyCode = currencyCode,
        )

    private fun BillPayment.toResponse() =
        BillPaymentResponse(
            id = id,
            billId = billId,
            paymentDate = paymentDate.toString(),
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
