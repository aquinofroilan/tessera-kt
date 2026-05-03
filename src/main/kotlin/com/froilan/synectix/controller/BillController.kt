package com.froilan.synectix.controller

import com.froilan.synectix.annotation.LogLevel
import com.froilan.synectix.annotation.Loggable
import com.froilan.synectix.dto.BillLineResponse
import com.froilan.synectix.dto.BillPaymentResponse
import com.froilan.synectix.dto.BillResponse
import com.froilan.synectix.dto.BillSummaryResponse
import com.froilan.synectix.dto.CreateBillRequest
import com.froilan.synectix.dto.RecordPaymentRequest
import com.froilan.synectix.dto.VoidBillRequest
import com.froilan.synectix.model.Bill
import com.froilan.synectix.model.BillPayment
import com.froilan.synectix.model.BillStatus
import com.froilan.synectix.security.AuthenticationContext
import com.froilan.synectix.service.BillService
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
@RequestMapping("/finance/ap/bills")
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
        val createdBy = authContext.userId() ?: "api-key"

        val bill = billService.createBill(request, orgId, createdBy)
        return ResponseEntity.status(HttpStatus.CREATED).body(bill.toResponse())
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ap:read')")
    fun listBills(
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) vendorId: String?,
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
        @PathVariable id: String,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()

        val bill = billService.getBill(id, orgId)
        return ResponseEntity.ok(bill.toResponse())
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('ap:approve')")
    fun approveBill(
        @PathVariable id: String,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val userId = authContext.userId() ?: "api-key"

        val bill = billService.approveBill(id, orgId, userId)
        return ResponseEntity.ok(bill.toResponse())
    }

    @PostMapping("/{id}/void")
    @PreAuthorize("hasAuthority('ap:void')")
    fun voidBill(
        @PathVariable id: String,
        @Valid @RequestBody request: VoidBillRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val userId = authContext.userId() ?: "api-key"

        val bill = billService.voidBill(id, orgId, request.reason, userId)
        return ResponseEntity.ok(bill.toResponse())
    }

    @PostMapping("/{id}/payments")
    @PreAuthorize("hasAuthority('ap:pay')")
    fun recordPayment(
        @PathVariable id: String,
        @Valid @RequestBody request: RecordPaymentRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val createdBy = authContext.userId() ?: "api-key"

        val payment = billService.recordPayment(id, request, orgId, createdBy)
        return ResponseEntity.status(HttpStatus.CREATED).body(payment.toResponse())
    }

    @GetMapping("/{id}/payments")
    @PreAuthorize("hasAuthority('ap:read')")
    fun listPayments(
        @PathVariable id: String,
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
