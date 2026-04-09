package com.froilan.synectix.service

import com.froilan.synectix.dto.AgingBucket
import com.froilan.synectix.dto.ApAgingReportResponse
import com.froilan.synectix.dto.CreateBillRequest
import com.froilan.synectix.dto.RecordPaymentRequest
import com.froilan.synectix.dto.VendorAgingResponse
import com.froilan.synectix.model.Bill
import com.froilan.synectix.model.BillLine
import com.froilan.synectix.model.BillPayment
import com.froilan.synectix.model.BillStatus
import com.froilan.synectix.model.JournalEntry
import com.froilan.synectix.model.JournalEntryLine
import com.froilan.synectix.model.JournalEntrySource
import com.froilan.synectix.model.JournalEntryStatus
import com.froilan.synectix.repository.AccountRepository
import com.froilan.synectix.repository.BillPaymentRepository
import com.froilan.synectix.repository.BillRepository
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

@Service
class BillService(
    private val billRepository: BillRepository,
    private val billPaymentRepository: BillPaymentRepository,
    private val accountRepository: AccountRepository,
    private val vendorService: VendorService,
    private val entryNumberGenerator: JournalEntryNumberGenerator,
) {
    @Transactional
    fun createBill(
        request: CreateBillRequest,
        organizationId: String,
        createdBy: String,
    ): Bill {
        val vendor = vendorService.getVendor(request.vendorId, organizationId)
        if (!vendor.isActive) {
            throw IllegalArgumentException("Cannot create bill for inactive vendor")
        }

        if (!request.date.isBefore(request.dueDate) && request.date != request.dueDate) {
            throw IllegalArgumentException("Due date must be on or after bill date")
        }

        val accountIds = request.lines.map { it.accountId }
        val accounts = accountRepository.findAllById(accountIds).associateBy { it.id }

        val missingAccounts = accountIds.filter { it !in accounts }
        if (missingAccounts.isNotEmpty()) {
            throw IllegalArgumentException("Accounts not found: ${missingAccounts.joinToString(", ")}")
        }

        accounts.values.forEach { account ->
            if (account.organizationId != organizationId) {
                throw IllegalArgumentException("Account '${account.code}' does not belong to this organization")
            }
            if (!account.isActive) {
                throw IllegalArgumentException("Account '${account.code}' is inactive")
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
                IllegalArgumentException("Bill not found")
            }
        if (bill.organizationId != organizationId) {
            throw IllegalArgumentException("Bill not found")
        }
        return bill
    }

    fun listBills(
        organizationId: String,
        status: BillStatus? = null,
        vendorId: String? = null,
    ): List<Bill> {
        val bills =
            when {
                status != null -> billRepository.findByOrganizationIdAndStatus(organizationId, status)
                vendorId != null -> billRepository.findByOrganizationIdAndVendorId(organizationId, vendorId)
                else -> billRepository.findByOrganizationId(organizationId)
            }
        return bills
    }

    @Transactional
    fun approveBill(
        billId: String,
        organizationId: String,
        approvedBy: String,
    ): Bill {
        val bill = getBill(billId, organizationId)

        if (bill.status != BillStatus.DRAFT) {
            throw IllegalArgumentException("Only draft bills can be approved")
        }

        val apAccount =
            accountRepository.findByOrganizationIdAndCode(organizationId, "2000").orElseThrow {
                IllegalStateException("Accounts Payable account (2000) not found")
            }

        val journalLines = mutableListOf<JournalEntryLine>()

        bill.lines.forEach { line ->
            journalLines.add(
                JournalEntryLine(
                    accountId = line.accountId,
                    accountCode = line.accountCode,
                    accountName = line.accountName,
                    debit = line.amount,
                    credit = BigDecimal.ZERO,
                    description = line.description,
                ),
            )
        }

        journalLines.add(
            JournalEntryLine(
                accountId = apAccount.id,
                accountCode = apAccount.code,
                accountName = apAccount.name,
                debit = BigDecimal.ZERO,
                credit = bill.totalAmount,
                description = "AP - ${bill.vendorName} - ${bill.billNumber}",
            ),
        )

        val journalEntry =
            entryNumberGenerator.saveWithRetry(organizationId) { entryNumber ->
                JournalEntry(
                    entryNumber = entryNumber,
                    date = bill.date,
                    description = "Bill ${bill.billNumber} - ${bill.vendorName}",
                    organizationId = organizationId,
                    status = JournalEntryStatus.POSTED,
                    source = JournalEntrySource.SYSTEM,
                    sourceReference = "BILL-APPROVE-${bill.id}",
                    lines = journalLines,
                    createdBy = approvedBy,
                    postedAt = LocalDateTime.now(),
                )
            }

        return billRepository.save(
            bill.copy(
                status = BillStatus.APPROVED,
                journalEntryId = journalEntry.id,
                approvedAt = LocalDateTime.now(),
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
            throw IllegalArgumentException("Bill is already voided")
        }
        if (bill.status == BillStatus.DRAFT) {
            return billRepository.save(
                bill.copy(
                    status = BillStatus.VOID,
                    voidedAt = LocalDateTime.now(),
                    voidReason = reason,
                ),
            )
        }
        if (bill.amountPaid.compareTo(BigDecimal.ZERO) != 0) {
            throw IllegalArgumentException("Cannot void a bill with recorded payments")
        }

        val apAccount =
            accountRepository.findByOrganizationIdAndCode(organizationId, "2000").orElseThrow {
                IllegalStateException("Accounts Payable account (2000) not found")
            }

        val reversingLines = mutableListOf<JournalEntryLine>()

        bill.lines.forEach { line ->
            reversingLines.add(
                JournalEntryLine(
                    accountId = line.accountId,
                    accountCode = line.accountCode,
                    accountName = line.accountName,
                    debit = BigDecimal.ZERO,
                    credit = line.amount,
                    description = "Void: ${line.description ?: ""}".trim(),
                ),
            )
        }

        reversingLines.add(
            JournalEntryLine(
                accountId = apAccount.id,
                accountCode = apAccount.code,
                accountName = apAccount.name,
                debit = bill.totalAmount,
                credit = BigDecimal.ZERO,
                description = "Void AP - ${bill.vendorName} - ${bill.billNumber}",
            ),
        )

        entryNumberGenerator.saveWithRetry(organizationId) { entryNumber ->
            JournalEntry(
                entryNumber = entryNumber,
                date = bill.date,
                description = "Void bill ${bill.billNumber} - ${bill.vendorName}",
                organizationId = organizationId,
                status = JournalEntryStatus.POSTED,
                source = JournalEntrySource.SYSTEM,
                sourceReference = "BILL-VOID-${bill.id}",
                lines = reversingLines,
                createdBy = voidedBy,
                postedAt = LocalDateTime.now(),
            )
        }

        return billRepository.save(
            bill.copy(
                status = BillStatus.VOID,
                voidedAt = LocalDateTime.now(),
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
            throw IllegalArgumentException("Bill must be approved or partially paid to record payment")
        }

        val remaining = bill.totalAmount.subtract(bill.amountPaid)
        if (request.amount.compareTo(remaining) > 0) {
            throw IllegalArgumentException(
                "Payment amount exceeds remaining balance of $remaining",
            )
        }

        val apAccount =
            accountRepository.findByOrganizationIdAndCode(organizationId, "2000").orElseThrow {
                IllegalStateException("Accounts Payable account (2000) not found")
            }
        val cashAccount =
            accountRepository.findByOrganizationIdAndCode(organizationId, "1000").orElseThrow {
                IllegalStateException("Cash account (1000) not found")
            }

        val journalEntry =
            entryNumberGenerator.saveWithRetry(organizationId) { entryNumber ->
                JournalEntry(
                    entryNumber = entryNumber,
                    date = request.paymentDate,
                    description = "Payment for bill ${bill.billNumber} - ${bill.vendorName}",
                    organizationId = organizationId,
                    status = JournalEntryStatus.POSTED,
                    source = JournalEntrySource.SYSTEM,
                    sourceReference = "BILL-PAYMENT-${bill.id}",
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
                    createdBy = createdBy,
                    postedAt = LocalDateTime.now(),
                )
            }

        val payment =
            billPaymentRepository.save(
                BillPayment(
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
                paidAt = if (fullyPaid) LocalDateTime.now() else null,
            ),
        )

        return payment
    }

    fun getPayments(
        billId: String,
        organizationId: String,
    ): List<BillPayment> {
        getBill(billId, organizationId)
        return billPaymentRepository.findByBillId(billId)
    }

    fun getAgingReport(
        organizationId: String,
        asOfDate: LocalDate = LocalDate.now(),
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

        val total = current.add(days1to30).add(days31to60).add(days61to90).add(days90plus)
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
                if (it == maxRetries - 1) throw e
            }
        }
        throw IllegalStateException("Failed to generate unique bill number")
    }
}
