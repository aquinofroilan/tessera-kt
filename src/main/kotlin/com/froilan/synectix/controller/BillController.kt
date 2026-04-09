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
import com.froilan.synectix.model.User
import com.froilan.synectix.security.ApiKeyContext
import com.froilan.synectix.security.SessionContext
import com.froilan.synectix.service.BillService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.Locale

@RestController
@RequestMapping("/finance/ap/bills")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class BillController(
    private val billService: BillService,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('ap:create')")
    fun createBill(
        @Valid @RequestBody request: CreateBillRequest,
    ): ResponseEntity<Any> {
        val orgId = extractOrganizationId() ?: return unauthorized()
        val createdBy = extractUserId() ?: "api-key"

        return try {
            val bill = billService.createBill(request, orgId, createdBy)
            ResponseEntity.status(HttpStatus.CREATED).body(bill.toResponse())
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest()
                .body(mapOf("error" to (e.message ?: "Failed to create bill")))
        }
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ap:read')")
    fun listBills(
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) vendorId: String?,
    ): ResponseEntity<Any> {
        val orgId = extractOrganizationId() ?: return unauthorized()

        val billStatus =
            if (status != null) {
                try {
                    BillStatus.valueOf(status.uppercase(Locale.ROOT))
                } catch (e: IllegalArgumentException) {
                    return ResponseEntity.badRequest()
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
        val orgId = extractOrganizationId() ?: return unauthorized()

        return try {
            val bill = billService.getBill(id, orgId)
            ResponseEntity.ok(bill.toResponse())
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to (e.message ?: "Bill not found")))
        }
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('ap:approve')")
    fun approveBill(
        @PathVariable id: String,
    ): ResponseEntity<Any> {
        val orgId = extractOrganizationId() ?: return unauthorized()
        val userId = extractUserId() ?: "api-key"

        return try {
            val bill = billService.approveBill(id, orgId, userId)
            ResponseEntity.ok(bill.toResponse())
        } catch (e: IllegalArgumentException) {
            val status =
                if (e.message == "Bill not found") HttpStatus.NOT_FOUND else HttpStatus.BAD_REQUEST
            ResponseEntity.status(status)
                .body(mapOf("error" to (e.message ?: "Failed to approve bill")))
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(mapOf("error" to (e.message ?: "Failed to approve bill")))
        }
    }

    @PostMapping("/{id}/void")
    @PreAuthorize("hasAuthority('ap:void')")
    fun voidBill(
        @PathVariable id: String,
        @Valid @RequestBody request: VoidBillRequest,
    ): ResponseEntity<Any> {
        val orgId = extractOrganizationId() ?: return unauthorized()
        val userId = extractUserId() ?: "api-key"

        return try {
            val bill = billService.voidBill(id, orgId, request.reason, userId)
            ResponseEntity.ok(bill.toResponse())
        } catch (e: IllegalArgumentException) {
            val status =
                if (e.message == "Bill not found") HttpStatus.NOT_FOUND else HttpStatus.BAD_REQUEST
            ResponseEntity.status(status)
                .body(mapOf("error" to (e.message ?: "Failed to void bill")))
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(mapOf("error" to (e.message ?: "Failed to void bill")))
        }
    }

    @PostMapping("/{id}/payments")
    @PreAuthorize("hasAuthority('ap:pay')")
    fun recordPayment(
        @PathVariable id: String,
        @Valid @RequestBody request: RecordPaymentRequest,
    ): ResponseEntity<Any> {
        val orgId = extractOrganizationId() ?: return unauthorized()
        val createdBy = extractUserId() ?: "api-key"

        return try {
            val payment = billService.recordPayment(id, request, orgId, createdBy)
            ResponseEntity.status(HttpStatus.CREATED).body(payment.toResponse())
        } catch (e: IllegalArgumentException) {
            val status =
                if (e.message == "Bill not found") HttpStatus.NOT_FOUND else HttpStatus.BAD_REQUEST
            ResponseEntity.status(status)
                .body(mapOf("error" to (e.message ?: "Failed to record payment")))
        } catch (e: IllegalStateException) {
            ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(mapOf("error" to (e.message ?: "Failed to record payment")))
        }
    }

    @GetMapping("/{id}/payments")
    @PreAuthorize("hasAuthority('ap:read')")
    fun listPayments(
        @PathVariable id: String,
    ): ResponseEntity<Any> {
        val orgId = extractOrganizationId() ?: return unauthorized()

        return try {
            val payments = billService.getPayments(id, orgId)
            ResponseEntity.ok(payments.map { it.toResponse() })
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to (e.message ?: "Bill not found")))
        }
    }

    @GetMapping("/aging")
    @PreAuthorize("hasAuthority('ap:read')")
    fun getAgingReport(
        @RequestParam(required = false) asOfDate: LocalDate?,
    ): ResponseEntity<Any> {
        val orgId = extractOrganizationId() ?: return unauthorized()
        val report = billService.getAgingReport(orgId, asOfDate ?: LocalDate.now())
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
            amountPaid = amountPaid,
            journalEntryId = journalEntryId,
            createdBy = createdBy,
            approvedAt = approvedAt?.toString(),
            approvedBy = approvedBy,
            paidAt = paidAt?.toString(),
            voidedAt = voidedAt?.toString(),
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
            amountPaid = amountPaid,
        )

    private fun BillPayment.toResponse() =
        BillPaymentResponse(
            id = id,
            billId = billId,
            paymentDate = paymentDate.toString(),
            amount = amount,
            paymentMethod = paymentMethod.name,
            referenceNumber = referenceNumber,
            journalEntryId = journalEntryId,
            createdBy = createdBy,
            createdAt = createdAt?.toString(),
        )

    private fun extractOrganizationId(): String? {
        val authentication = SecurityContextHolder.getContext().authentication ?: return null
        return when (val details = authentication.details) {
            is SessionContext -> details.organizationId
            is ApiKeyContext -> details.organizationId
            else -> null
        }
    }

    private fun extractUserId(): String? {
        val authentication = SecurityContextHolder.getContext().authentication ?: return null
        return (authentication.principal as? User)?.uuid
    }

    private fun unauthorized(): ResponseEntity<Any> =
        ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(mapOf("error" to "Authentication required"))
}
