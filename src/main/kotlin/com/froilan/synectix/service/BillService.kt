package com.froilan.synectix.service

import com.froilan.synectix.exception.BusinessRuleException
import com.froilan.synectix.exception.ResourceNotFoundException

import com.froilan.synectix.dto.AgingBucket
import com.froilan.synectix.dto.ApAgingReportResponse
import com.froilan.synectix.dto.CreateBillRequest
import com.froilan.synectix.dto.RecordPaymentRequest
import com.froilan.synectix.dto.VendorAgingResponse
import com.froilan.synectix.model.Account
import com.froilan.synectix.model.Bill
import com.froilan.synectix.model.BillLine
import com.froilan.synectix.model.BillPayment
import com.froilan.synectix.model.BillStatus
import com.froilan.synectix.model.JournalEntryLine
import com.froilan.synectix.repository.AccountRepository
import com.froilan.synectix.repository.BillPaymentRepository
import com.froilan.synectix.repository.BillRepository
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
class BillService(
    private val billRepository: BillRepository,
    private val billPaymentRepository: BillPaymentRepository,
    private val accountRepository: AccountRepository,
    private val vendorService: VendorService,
    private val journalEntryService: JournalEntryService,
) {
    @Transactional
    fun createBill(
        request: CreateBillRequest,
        organizationId: String,
        createdBy: String,
    ): Bill {
        val vendor = vendorService.getVendor(request.vendorId, organizationId)
        if (!vendor.isActive) {
            throw BusinessRuleException("Cannot create bill for inactive vendor")
        }

        if (request.date.isAfter(request.dueDate)) {
            throw BusinessRuleException("Due date must be on or after bill date")
        }

        if (request.lines.isEmpty()) {
            throw BusinessRuleException("At least one line item is required")
        }

        request.lines.forEach { line ->
            if (line.amount <= BigDecimal.ZERO) {
                throw BusinessRuleException("Line item amounts must be positive")
            }
        }

        val accountIds = request.lines.map { it.accountId }.distinct()
        val accounts = accountRepository.findAllById(accountIds).associateBy { it.id }

        val missingAccounts = accountIds.filter { it !in accounts }
        if (missingAccounts.isNotEmpty()) {
            throw BusinessRuleException("Accounts not found: ${missingAccounts.joinToString(", ")}")
        }

        accounts.values.forEach { account ->
            if (account.organizationId != organizationId) {
                throw ResourceNotFoundException("Account '${account.id}' not found")
            }
            if (!account.isActive) {
                throw BusinessRuleException("Account '${account.code}' is inactive")
            }
        }

        val lines =
            request.lines.map { lineReq ->
                val account = accounts.getValue(lineReq.accountId)
                BillLine(
                    accountId = account.id,
                    accountCode = account.code,
                    accountName = account.name,
                    amount = lineReq.amount,
                    description = lineReq.description,
                )
            }

        val totalAmount = lines.fold(BigDecimal.ZERO) { sum, line -> sum.add(line.amount) }

        return saveBillWithRetry(organizationId) { billNumber ->
            Bill(
                billNumber = billNumber,
                vendorId = vendor.id,
                vendorName = vendor.name,
                date = request.date,
                dueDate = request.dueDate,
                referenceNumber = request.referenceNumber,
                organizationId = organizationId,
                lines = lines,
                totalAmount = totalAmount,
                createdBy = createdBy,
            )
        }
    }

    fun getBill(
        billId: String,
        organizationId: String,
    ): Bill {
        val bill =
            billRepository.findById(billId).orElseThrow {
                ResourceNotFoundException("Bill not found")
            }
        if (bill.organizationId != organizationId) {
            throw ResourceNotFoundException("Bill not found")
        }
        return bill
    }

    fun listBills(
        organizationId: String,
        status: BillStatus? = null,
        vendorId: String? = null,
    ): List<Bill> =
        when {
            status != null && vendorId != null ->
                billRepository.findByOrganizationIdAndStatusAndVendorId(organizationId, status, vendorId)
            status != null ->
                billRepository.findByOrganizationIdAndStatus(organizationId, status)
            vendorId != null ->
                billRepository.findByOrganizationIdAndVendorId(organizationId, vendorId)
            else ->
                billRepository.findByOrganizationId(organizationId)
        }

    @Transactional
    fun approveBill(
        billId: String,
        organizationId: String,
        approvedBy: String,
    ): Bill {
        val bill = getBill(billId, organizationId)

        if (bill.status != BillStatus.DRAFT) {
            throw BusinessRuleException("Only draft bills can be approved")
        }

        val apAccount = getApAccount(organizationId)

        val journalLines =
            bill.lines.map { line ->
                JournalEntryLine(
                    accountId = line.accountId,
                    accountCode = line.accountCode,
                    accountName = line.accountName,
                    debit = line.amount,
                    credit = BigDecimal.ZERO,
                    description = line.description,
                )
            } +
                JournalEntryLine(
                    accountId = apAccount.id,
                    accountCode = apAccount.code,
                    accountName = apAccount.name,
                    debit = BigDecimal.ZERO,
                    credit = bill.totalAmount,
                    description = "AP - ${bill.vendorName} - ${bill.billNumber}",
                )

        val journalEntry =
            journalEntryService.createSystemEntry(
                date = bill.date,
                description = "Bill ${bill.billNumber} - ${bill.vendorName}",
                organizationId = organizationId,
                lines = journalLines,
                sourceReference = "BILL-APPROVE-${bill.id}",
                createdBy = approvedBy,
            )

        val now = LocalDateTime.now(ZoneOffset.UTC)
        return billRepository.save(
            bill.copy(
                status = BillStatus.APPROVED,
                journalEntryId = journalEntry.id,
                approvedAt = now,
                approvedBy = approvedBy,
            ),
        )
    }

    @Transactional
    fun voidBill(
        billId: String,
        organizationId: String,
        reason: String,
        voidedBy: String,
    ): Bill {
        val bill = getBill(billId, organizationId)

        if (bill.status == BillStatus.VOID) {
            throw BusinessRuleException("Bill is already voided")
        }
        val now = LocalDateTime.now(ZoneOffset.UTC)

        // Draft voids skip fiscal validation — no GL entries are posted
        if (bill.status == BillStatus.DRAFT) {
            return billRepository.save(
                bill.copy(
                    status = BillStatus.VOID,
                    voidedAt = now,
                    voidedBy = voidedBy,
                    voidReason = reason,
                ),
            )
        }
        if (bill.amountPaid.compareTo(BigDecimal.ZERO) != 0) {
            throw BusinessRuleException("Cannot void a bill with recorded payments")
        }

        if (bill.journalEntryId != null) {
            journalEntryService.voidJournalEntry(
                bill.journalEntryId,
                organizationId,
                reason,
            )
        }

        return billRepository.save(
            bill.copy(
                status = BillStatus.VOID,
                voidedAt = now,
                voidedBy = voidedBy,
                voidReason = reason,
            ),
        )
    }

    @Transactional
    fun recordPayment(
        billId: String,
        request: RecordPaymentRequest,
        organizationId: String,
        createdBy: String,
    ): BillPayment {
        val bill = getBill(billId, organizationId)

        if (bill.status != BillStatus.APPROVED && bill.status != BillStatus.PARTIALLY_PAID) {
            throw BusinessRuleException("Bill must be approved or partially paid to record payment")
        }

        if (request.amount <= BigDecimal.ZERO) {
            throw BusinessRuleException("Payment amount must be positive")
        }

        val remaining = bill.totalAmount.subtract(bill.amountPaid)
        if (request.amount.compareTo(remaining) > 0) {
            throw BusinessRuleException(
                "Payment amount exceeds remaining balance of $remaining",
            )
        }

        val apAccount = getApAccount(organizationId)
        val cashAccount = getCashAccount(organizationId)

        val paymentId = UUID.randomUUID().toString()

        val journalEntry =
            journalEntryService.createSystemEntry(
                date = request.paymentDate,
                description = "Payment for bill ${bill.billNumber} - ${bill.vendorName}",
                organizationId = organizationId,
                lines =
                    listOf(
                        JournalEntryLine(
                            accountId = apAccount.id,
                            accountCode = apAccount.code,
                            accountName = apAccount.name,
                            debit = request.amount,
                            credit = BigDecimal.ZERO,
                            description = "Payment - ${bill.vendorName}",
                        ),
                        JournalEntryLine(
                            accountId = cashAccount.id,
                            accountCode = cashAccount.code,
                            accountName = cashAccount.name,
                            debit = BigDecimal.ZERO,
                            credit = request.amount,
                            description = "Payment - ${bill.vendorName}",
                        ),
                    ),
                sourceReference = "BILL-PAYMENT-$paymentId",
                createdBy = createdBy,
            )

        val payment =
            billPaymentRepository.save(
                BillPayment(
                    id = paymentId,
                    billId = bill.id,
                    paymentDate = request.paymentDate,
                    amount = request.amount,
                    paymentMethod = request.paymentMethod,
                    referenceNumber = request.referenceNumber,
                    journalEntryId = journalEntry.id,
                    organizationId = organizationId,
                    createdBy = createdBy,
                ),
            )

        val newAmountPaid = bill.amountPaid.add(request.amount)
        val fullyPaid = newAmountPaid.compareTo(bill.totalAmount) >= 0
        val newStatus = if (fullyPaid) BillStatus.PAID else BillStatus.PARTIALLY_PAID

        billRepository.save(
            bill.copy(
                amountPaid = newAmountPaid,
                status = newStatus,
                paidAt = if (fullyPaid) LocalDateTime.now(ZoneOffset.UTC) else null,
            ),
        )

        return payment
    }

    fun getPayments(
        billId: String,
        organizationId: String,
    ): List<BillPayment> {
        getBill(billId, organizationId)
        return billPaymentRepository.findByBillIdAndOrganizationId(billId, organizationId)
    }

    fun getAgingReport(
        organizationId: String,
        asOfDate: LocalDate = LocalDate.now(ZoneOffset.UTC),
    ): ApAgingReportResponse {
        val outstandingStatuses =
            listOf(BillStatus.APPROVED, BillStatus.PARTIALLY_PAID)
        val bills = billRepository.findByOrganizationIdAndStatusIn(organizationId, outstandingStatuses)

        val vendorBills = bills.groupBy { it.vendorId }

        val vendorAgingList =
            vendorBills.map { (_, vendorBillList) ->
                val vendorName = vendorBillList.first().vendorName
                val vendorId = vendorBillList.first().vendorId
                val bucket = calculateAgingBucket(vendorBillList, asOfDate)
                VendorAgingResponse(
                    vendorId = vendorId,
                    vendorName = vendorName,
                    aging = bucket,
                )
            }

        val totals =
            AgingBucket(
                current = vendorAgingList.fold(BigDecimal.ZERO) { sum, v -> sum.add(v.aging.current) },
                days1to30 = vendorAgingList.fold(BigDecimal.ZERO) { sum, v -> sum.add(v.aging.days1to30) },
                days31to60 = vendorAgingList.fold(BigDecimal.ZERO) { sum, v -> sum.add(v.aging.days31to60) },
                days61to90 = vendorAgingList.fold(BigDecimal.ZERO) { sum, v -> sum.add(v.aging.days61to90) },
                days90plus = vendorAgingList.fold(BigDecimal.ZERO) { sum, v -> sum.add(v.aging.days90plus) },
                total = vendorAgingList.fold(BigDecimal.ZERO) { sum, v -> sum.add(v.aging.total) },
            )

        return ApAgingReportResponse(
            asOfDate = asOfDate.toString(),
            vendors = vendorAgingList,
            totals = totals,
        )
    }

    private fun calculateAgingBucket(
        bills: List<Bill>,
        asOfDate: LocalDate,
    ): AgingBucket {
        var current = BigDecimal.ZERO
        var days1to30 = BigDecimal.ZERO
        var days31to60 = BigDecimal.ZERO
        var days61to90 = BigDecimal.ZERO
        var days90plus = BigDecimal.ZERO

        bills.forEach { bill ->
            val outstanding = bill.totalAmount.subtract(bill.amountPaid)
            val daysOverdue = ChronoUnit.DAYS.between(bill.dueDate, asOfDate)

            when {
                daysOverdue <= 0 -> current = current.add(outstanding)
                daysOverdue <= 30 -> days1to30 = days1to30.add(outstanding)
                daysOverdue <= 60 -> days31to60 = days31to60.add(outstanding)
                daysOverdue <= 90 -> days61to90 = days61to90.add(outstanding)
                else -> days90plus = days90plus.add(outstanding)
            }
        }

        val total =
            current
                .add(days1to30)
                .add(days31to60)
                .add(days61to90)
                .add(days90plus)
        return AgingBucket(
            current = current,
            days1to30 = days1to30,
            days31to60 = days31to60,
            days61to90 = days61to90,
            days90plus = days90plus,
            total = total,
        )
    }

    private fun saveBillWithRetry(
        organizationId: String,
        maxRetries: Int = 3,
        buildBill: (String) -> Bill,
    ): Bill {
        repeat(maxRetries) {
            val count = billRepository.countByOrganizationId(organizationId)
            val billNumber = "BILL-${(count + 1).toString().padStart(4, '0')}"
            try {
                return billRepository.save(buildBill(billNumber))
            } catch (e: DuplicateKeyException) {
                if (it == maxRetries - 1) {
                    throw IllegalStateException("Failed to generate unique bill number: $billNumber", e)
                }
            }
        }
        throw IllegalStateException("Failed to generate unique bill number")
    }

    private fun getApAccount(organizationId: String): Account {
        val account =
            accountRepository.findByOrganizationIdAndCode(organizationId, "2000").orElseThrow {
                IllegalStateException("Accounts Payable account (2000) not found")
            }
        if (!account.isActive) {
            throw BusinessRuleException("Accounts Payable account (2000) is inactive")
        }
        return account
    }

    private fun getCashAccount(organizationId: String): Account {
        val account =
            accountRepository.findByOrganizationIdAndCode(organizationId, "1000").orElseThrow {
                IllegalStateException("Cash account (1000) not found")
            }
        if (!account.isActive) {
            throw BusinessRuleException("Cash account (1000) is inactive")
        }
        return account
    }
}
