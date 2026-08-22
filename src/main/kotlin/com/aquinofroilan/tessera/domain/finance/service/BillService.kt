package com.aquinofroilan.tessera.domain.finance.service

import com.aquinofroilan.tessera.domain.finance.dto.AgingBucket
import com.aquinofroilan.tessera.domain.finance.dto.ApAgingReportResponse
import com.aquinofroilan.tessera.domain.finance.dto.CreateBillRequest
import com.aquinofroilan.tessera.domain.finance.dto.RecordPaymentRequest
import com.aquinofroilan.tessera.domain.finance.dto.VendorAgingResponse
import com.aquinofroilan.tessera.domain.finance.model.Account
import com.aquinofroilan.tessera.domain.finance.model.Bill
import com.aquinofroilan.tessera.domain.finance.model.BillLine
import com.aquinofroilan.tessera.domain.finance.model.BillPayment
import com.aquinofroilan.tessera.domain.finance.model.BillStatus
import com.aquinofroilan.tessera.domain.finance.model.JournalEntryLine
import com.aquinofroilan.tessera.domain.finance.repository.AccountRepository
import com.aquinofroilan.tessera.domain.finance.repository.BillPaymentRepository
import com.aquinofroilan.tessera.domain.finance.repository.BillRepository
import com.aquinofroilan.tessera.domain.organization.repository.OrganizationRepository
import com.aquinofroilan.tessera.domain.procurement.service.VendorService
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
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
    private val taxGroupService: TaxGroupService,
    private val organizationRepository: OrganizationRepository,
    private val currencyService: CurrencyService,
    private val exchangeRateService: ExchangeRateService,
) {
    @Transactional
    fun createBill(
        request: CreateBillRequest,
        organizationId: java.util.UUID,
        createdBy: java.util.UUID,
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
                throw BusinessRuleException("Account '${account.id}' not found")
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

        val subtotalAmount = lines.fold(BigDecimal.ZERO) { sum, line -> sum.add(line.amount) }
        val taxAmount = taxGroupService.calculateTaxAmount(request.taxGroupId, organizationId, subtotalAmount)
        val totalAmount = subtotalAmount.add(taxAmount)

        val baseCurrency = getBaseCurrency(organizationId)
        val docCurrency = request.currencyCode ?: baseCurrency
        if (docCurrency != baseCurrency) {
            currencyService.getCurrency(docCurrency)
        }
        val exchangeRate =
            if (docCurrency == baseCurrency) {
                BigDecimal.ONE
            } else {
                exchangeRateService.getRate(organizationId, docCurrency, baseCurrency, request.date)
            }
        val baseDecimals = currencyService.getCurrency(baseCurrency).decimalPlaces
        val baseCurrencyAmount = totalAmount.multiply(exchangeRate).setScale(baseDecimals, RoundingMode.HALF_UP)
        val baseCurrencyTaxAmount = taxAmount.multiply(exchangeRate).setScale(baseDecimals, RoundingMode.HALF_UP)

        return saveBillWithRetry(organizationId) { billNumber ->
            Bill(
                billNumber = billNumber,
                vendorId = vendor.id,
                vendorName = vendor.name,
                date = request.date,
                dueDate = request.dueDate,
                referenceNumber = request.referenceNumber,
                taxGroupId = request.taxGroupId,
                organizationId = organizationId,
                lines = lines,
                totalAmount = totalAmount,
                taxAmount = taxAmount,
                currencyCode = docCurrency,
                exchangeRate = exchangeRate,
                baseCurrencyAmount = baseCurrencyAmount,
                baseCurrencyTaxAmount = baseCurrencyTaxAmount,
                createdBy = createdBy,
            )
        }
    }

    private fun getBaseCurrency(organizationId: java.util.UUID): String =
        organizationRepository
            .findById(organizationId)
            .orElseThrow {
                ResourceNotFoundException("Organization not found")
            }.baseCurrency

    fun getBill(
        billId: java.util.UUID,
        organizationId: java.util.UUID,
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
        organizationId: java.util.UUID,
        status: BillStatus? = null,
        vendorId: java.util.UUID? = null,
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
        billId: java.util.UUID,
        organizationId: java.util.UUID,
        approvedBy: java.util.UUID,
    ): Bill {
        val bill = getBill(billId, organizationId)

        if (bill.status != BillStatus.DRAFT) {
            throw BusinessRuleException("Only draft bills can be approved")
        }

        val apAccount = getApAccount(organizationId)
        val baseCurrency = getBaseCurrency(organizationId)
        val baseDecimals = currencyService.getCurrency(baseCurrency).decimalPlaces

        val rawExpenseLines =
            bill.lines.map { line ->
                JournalEntryLine(
                    accountId = line.accountId,
                    accountCode = line.accountCode,
                    accountName = line.accountName,
                    debit = line.amount.multiply(bill.exchangeRate).setScale(baseDecimals, RoundingMode.HALF_UP),
                    credit = BigDecimal.ZERO,
                    description = line.description,
                )
            }

        val expectedExpenseTotal = bill.baseCurrencyAmount.subtract(bill.baseCurrencyTaxAmount)
        val rawExpenseTotal = rawExpenseLines.fold(BigDecimal.ZERO) { sum, line -> sum.add(line.debit) }
        val expenseLines = absorbDebitDelta(rawExpenseLines, expectedExpenseTotal.subtract(rawExpenseTotal))

        val taxLines =
            if (bill.baseCurrencyTaxAmount.compareTo(BigDecimal.ZERO) > 0) {
                val taxInputAccount = getTaxInputAccount(organizationId)
                listOf(
                    JournalEntryLine(
                        accountId = taxInputAccount.id,
                        accountCode = taxInputAccount.code,
                        accountName = taxInputAccount.name,
                        debit = bill.baseCurrencyTaxAmount,
                        credit = BigDecimal.ZERO,
                        description = "Tax Input - ${bill.vendorName} - ${bill.billNumber}",
                    ),
                )
            } else {
                emptyList()
            }

        val journalLines =
            expenseLines +
                taxLines +
                JournalEntryLine(
                    accountId = apAccount.id,
                    accountCode = apAccount.code,
                    accountName = apAccount.name,
                    debit = BigDecimal.ZERO,
                    credit = bill.baseCurrencyAmount,
                    description = "AP - ${bill.vendorName} - ${bill.billNumber}",
                )

        val description =
            if (bill.currencyCode == baseCurrency) {
                "Bill ${bill.billNumber} - ${bill.vendorName}"
            } else {
                "Bill ${bill.billNumber} - ${bill.vendorName} (${bill.currencyCode} ${bill.totalAmount} @ ${bill.exchangeRate})"
            }

        val journalEntry =
            journalEntryService.createSystemEntry(
                date = bill.date,
                description = description,
                organizationId = organizationId,
                lines = journalLines,
                sourceReference = "BILL-APPROVE-${bill.id}",
                createdBy = approvedBy,
            )

        val now = LocalDateTime.now(ZoneOffset.UTC)
        bill.status = BillStatus.APPROVED
        bill.journalEntryId = journalEntry.id
        bill.approvedAt = now
        bill.approvedBy = approvedBy
        return billRepository.save(bill)
    }

    @Transactional
    fun voidBill(
        billId: java.util.UUID,
        organizationId: java.util.UUID,
        reason: String,
        voidedBy: java.util.UUID,
    ): Bill {
        val bill = getBill(billId, organizationId)

        if (bill.status == BillStatus.VOID) {
            throw BusinessRuleException("Bill is already voided")
        }
        val now = LocalDateTime.now(ZoneOffset.UTC)

        if (bill.status == BillStatus.DRAFT) {
            bill.status = BillStatus.VOID
            bill.voidedAt = now
            bill.voidedBy = voidedBy
            bill.voidReason = reason
            return billRepository.save(bill)
        }
        if (bill.amountPaid.compareTo(BigDecimal.ZERO) != 0) {
            throw BusinessRuleException("Cannot void a bill with recorded payments")
        }

        val journalEntryId = bill.journalEntryId
        if (journalEntryId != null) {
            journalEntryService.voidJournalEntry(
                journalEntryId,
                organizationId,
                reason,
            )
        }

        bill.status = BillStatus.VOID
        bill.voidedAt = now
        bill.voidedBy = voidedBy
        bill.voidReason = reason
        return billRepository.save(bill)
    }

    @Transactional
    fun recordPayment(
        billId: java.util.UUID,
        request: RecordPaymentRequest,
        organizationId: java.util.UUID,
        createdBy: java.util.UUID,
        cashAccountOverride: Account? = null,
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
        val cashAccount = cashAccountOverride ?: getCashAccount(organizationId)

        val paymentId = java.util.UUID.ofEpochMillis(System.currentTimeMillis())
        val baseDecimals = currencyService.getCurrency(getBaseCurrency(organizationId)).decimalPlaces
        val newAmountPaid = bill.amountPaid.add(request.amount)
        val fullyPaid = newAmountPaid.compareTo(bill.totalAmount) >= 0
        val paymentBaseAmount =
            if (fullyPaid) {
                bill.baseCurrencyAmount.subtract(bill.baseCurrencyAmountPaid)
            } else {
                request.amount.multiply(bill.exchangeRate).setScale(baseDecimals, RoundingMode.HALF_UP)
            }

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
                            debit = paymentBaseAmount,
                            credit = BigDecimal.ZERO,
                            description = "Payment - ${bill.vendorName}",
                        ),
                        JournalEntryLine(
                            accountId = cashAccount.id,
                            accountCode = cashAccount.code,
                            accountName = cashAccount.name,
                            debit = BigDecimal.ZERO,
                            credit = paymentBaseAmount,
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
                    baseCurrencyAmount = paymentBaseAmount,
                    exchangeRate = bill.exchangeRate,
                    paymentMethod = request.paymentMethod,
                    referenceNumber = request.referenceNumber,
                    journalEntryId = journalEntry.id,
                    organizationId = organizationId,
                    createdBy = createdBy,
                ),
            )

        val newBaseAmountPaid = bill.baseCurrencyAmountPaid.add(paymentBaseAmount)
        val newStatus = if (fullyPaid) BillStatus.PAID else BillStatus.PARTIALLY_PAID

        bill.amountPaid = newAmountPaid
        bill.baseCurrencyAmountPaid = newBaseAmountPaid
        bill.status = newStatus
        bill.paidAt = if (fullyPaid) LocalDateTime.now(ZoneOffset.UTC) else null
        billRepository.save(bill)

        return payment
    }

    fun getPayments(
        billId: java.util.UUID,
        organizationId: java.util.UUID,
    ): List<BillPayment> {
        getBill(billId, organizationId)
        return billPaymentRepository.findByBillIdAndOrganizationId(billId, organizationId)
    }

    fun getAgingReport(
        organizationId: java.util.UUID,
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
            val outstanding = bill.baseCurrencyAmount.subtract(bill.baseCurrencyAmountPaid)
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
        organizationId: java.util.UUID,
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

    private fun getApAccount(organizationId: java.util.UUID): Account {
        val account =
            accountRepository.findByOrganizationIdAndCode(organizationId, "2000").orElseThrow {
                IllegalStateException("Accounts Payable account (2000) not found")
            }
        if (!account.isActive) {
            throw BusinessRuleException("Accounts Payable account (2000) is inactive")
        }
        return account
    }

    private fun getCashAccount(organizationId: java.util.UUID): Account {
        val account =
            accountRepository.findByOrganizationIdAndCode(organizationId, "1000").orElseThrow {
                IllegalStateException("Cash account (1000) not found")
            }
        if (!account.isActive) {
            throw BusinessRuleException("Cash account (1000) is inactive")
        }
        return account
    }

    private fun absorbDebitDelta(
        lines: List<JournalEntryLine>,
        delta: BigDecimal,
    ): List<JournalEntryLine> {
        if (delta.signum() == 0 || lines.isEmpty()) return lines
        val mutable = lines.toMutableList()
        var remaining = delta
        val order = mutable.indices.sortedByDescending { mutable[it].debit }
        for (i in order) {
            if (remaining.signum() == 0) break
            val line = mutable[i]
            val applied =
                if (remaining.signum() < 0) remaining.max(line.debit.negate()) else remaining
            mutable[i] = line.apply { debit = line.debit.add(applied) }
            remaining = remaining.subtract(applied)
        }
        return mutable
    }

    private fun getTaxInputAccount(organizationId: java.util.UUID): Account {
        val account =
            accountRepository.findByOrganizationIdAndCode(organizationId, "2310").orElseThrow {
                IllegalStateException("Tax Input Credits account (2310) not found")
            }
        if (!account.isActive) {
            throw BusinessRuleException("Tax Input Credits account (2310) is inactive")
        }
        return account
    }
}
