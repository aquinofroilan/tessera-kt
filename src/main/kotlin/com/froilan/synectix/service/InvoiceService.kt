package com.froilan.synectix.service

import com.froilan.synectix.dto.AgingBucket
import com.froilan.synectix.dto.ArAgingReportResponse
import com.froilan.synectix.dto.CreateInvoiceRequest
import com.froilan.synectix.dto.CustomerAgingResponse
import com.froilan.synectix.dto.RecordReceiptRequest
import com.froilan.synectix.exception.BusinessRuleException
import com.froilan.synectix.exception.ResourceNotFoundException
import com.froilan.synectix.model.Account
import com.froilan.synectix.model.Invoice
import com.froilan.synectix.model.InvoiceLine
import com.froilan.synectix.model.InvoiceReceipt
import com.froilan.synectix.model.InvoiceStatus
import com.froilan.synectix.model.JournalEntryLine
import com.froilan.synectix.repository.AccountRepository
import com.froilan.synectix.repository.InvoiceReceiptRepository
import com.froilan.synectix.repository.InvoiceRepository
import com.froilan.synectix.repository.OrganizationRepository
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
class InvoiceService(
    private val invoiceRepository: InvoiceRepository,
    private val invoiceReceiptRepository: InvoiceReceiptRepository,
    private val accountRepository: AccountRepository,
    private val customerService: CustomerService,
    private val journalEntryService: JournalEntryService,
    private val taxGroupService: TaxGroupService,
    private val organizationRepository: OrganizationRepository,
    private val currencyService: CurrencyService,
    private val exchangeRateService: ExchangeRateService,
) {
    @Transactional
    fun createInvoice(
        request: CreateInvoiceRequest,
        organizationId: String,
        createdBy: String,
    ): Invoice {
        val customer = customerService.getCustomer(request.customerId, organizationId)
        if (!customer.isActive) {
            throw BusinessRuleException("Cannot create invoice for inactive customer")
        }

        if (request.date.isAfter(request.dueDate)) {
            throw BusinessRuleException("Due date must be on or after invoice date")
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
                InvoiceLine(
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

        return saveInvoiceWithRetry(organizationId) { invoiceNumber ->
            Invoice(
                invoiceNumber = invoiceNumber,
                customerId = customer.id,
                customerName = customer.name,
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

    private fun getBaseCurrency(organizationId: String): String =
        organizationRepository
            .findById(organizationId)
            .orElseThrow {
                ResourceNotFoundException("Organization not found")
            }.baseCurrency

    fun getInvoice(
        invoiceId: String,
        organizationId: String,
    ): Invoice {
        val invoice =
            invoiceRepository.findById(invoiceId).orElseThrow {
                ResourceNotFoundException("Invoice not found")
            }
        if (invoice.organizationId != organizationId) {
            throw ResourceNotFoundException("Invoice not found")
        }
        return invoice
    }

    fun listInvoices(
        organizationId: String,
        status: InvoiceStatus? = null,
        customerId: String? = null,
    ): List<Invoice> =
        when {
            status != null && customerId != null ->
                invoiceRepository.findByOrganizationIdAndStatusAndCustomerId(organizationId, status, customerId)
            status != null ->
                invoiceRepository.findByOrganizationIdAndStatus(organizationId, status)
            customerId != null ->
                invoiceRepository.findByOrganizationIdAndCustomerId(organizationId, customerId)
            else ->
                invoiceRepository.findByOrganizationId(organizationId)
        }

    @Transactional
    fun approveInvoice(
        invoiceId: String,
        organizationId: String,
        approvedBy: String,
    ): Invoice {
        val invoice = getInvoice(invoiceId, organizationId)

        if (invoice.status != InvoiceStatus.DRAFT) {
            throw BusinessRuleException("Only draft invoices can be approved")
        }

        val arAccount = getArAccount(organizationId)
        val baseCurrency = getBaseCurrency(organizationId)
        val baseDecimals = currencyService.getCurrency(baseCurrency).decimalPlaces

        val rawRevenueLines =
            invoice.lines.map { line ->
                JournalEntryLine(
                    accountId = line.accountId,
                    accountCode = line.accountCode,
                    accountName = line.accountName,
                    debit = BigDecimal.ZERO,
                    credit = line.amount.multiply(invoice.exchangeRate).setScale(baseDecimals, RoundingMode.HALF_UP),
                    description = line.description,
                )
            }

        val expectedRevenueTotal = invoice.baseCurrencyAmount.subtract(invoice.baseCurrencyTaxAmount)
        val rawRevenueTotal = rawRevenueLines.fold(BigDecimal.ZERO) { sum, line -> sum.add(line.credit) }
        val revenueLines = absorbCreditDelta(rawRevenueLines, expectedRevenueTotal.subtract(rawRevenueTotal))

        val taxLines =
            if (invoice.baseCurrencyTaxAmount.compareTo(BigDecimal.ZERO) > 0) {
                val taxPayableAccount = getTaxPayableAccount(organizationId)
                listOf(
                    JournalEntryLine(
                        accountId = taxPayableAccount.id,
                        accountCode = taxPayableAccount.code,
                        accountName = taxPayableAccount.name,
                        debit = BigDecimal.ZERO,
                        credit = invoice.baseCurrencyTaxAmount,
                        description = "Tax Payable - ${invoice.customerName} - ${invoice.invoiceNumber}",
                    ),
                )
            } else {
                emptyList()
            }

        val journalLines =
            listOf(
                JournalEntryLine(
                    accountId = arAccount.id,
                    accountCode = arAccount.code,
                    accountName = arAccount.name,
                    debit = invoice.baseCurrencyAmount,
                    credit = BigDecimal.ZERO,
                    description = "AR - ${invoice.customerName} - ${invoice.invoiceNumber}",
                ),
            ) + revenueLines + taxLines

        val description =
            if (invoice.currencyCode == baseCurrency) {
                "Invoice ${invoice.invoiceNumber} - ${invoice.customerName}"
            } else {
                "Invoice ${invoice.invoiceNumber} - ${invoice.customerName} " +
                    "(${invoice.currencyCode} ${invoice.totalAmount} @ ${invoice.exchangeRate})"
            }

        val journalEntry =
            journalEntryService.createSystemEntry(
                date = invoice.date,
                description = description,
                organizationId = organizationId,
                lines = journalLines,
                sourceReference = "INVOICE-APPROVE-${invoice.id}",
                createdBy = approvedBy,
            )

        val now = LocalDateTime.now(ZoneOffset.UTC)
        return invoiceRepository.save(
            invoice.copy(
                status = InvoiceStatus.APPROVED,
                journalEntryId = journalEntry.id,
                approvedAt = now,
                approvedBy = approvedBy,
            ),
        )
    }

    @Transactional
    fun voidInvoice(
        invoiceId: String,
        organizationId: String,
        reason: String,
        voidedBy: String,
    ): Invoice {
        val invoice = getInvoice(invoiceId, organizationId)

        if (invoice.status == InvoiceStatus.VOID) {
            throw BusinessRuleException("Invoice is already voided")
        }

        val now = LocalDateTime.now(ZoneOffset.UTC)

        if (invoice.status == InvoiceStatus.DRAFT) {
            return invoiceRepository.save(
                invoice.copy(
                    status = InvoiceStatus.VOID,
                    voidedAt = now,
                    voidedBy = voidedBy,
                    voidReason = reason,
                ),
            )
        }

        if (invoice.amountReceived.compareTo(BigDecimal.ZERO) != 0) {
            throw BusinessRuleException("Cannot void an invoice with recorded receipts")
        }

        if (invoice.journalEntryId != null) {
            journalEntryService.voidJournalEntry(
                invoice.journalEntryId,
                organizationId,
                reason,
            )
        }

        return invoiceRepository.save(
            invoice.copy(
                status = InvoiceStatus.VOID,
                voidedAt = now,
                voidedBy = voidedBy,
                voidReason = reason,
            ),
        )
    }

    @Transactional
    fun recordReceipt(
        invoiceId: String,
        request: RecordReceiptRequest,
        organizationId: String,
        createdBy: String,
    ): InvoiceReceipt {
        val invoice = getInvoice(invoiceId, organizationId)

        if (invoice.status != InvoiceStatus.APPROVED && invoice.status != InvoiceStatus.PARTIALLY_PAID) {
            throw BusinessRuleException("Invoice must be approved or partially paid to record receipt")
        }

        if (request.amount <= BigDecimal.ZERO) {
            throw BusinessRuleException("Receipt amount must be positive")
        }

        val remaining = invoice.totalAmount.subtract(invoice.amountReceived)
        if (request.amount.compareTo(remaining) > 0) {
            throw BusinessRuleException(
                "Receipt amount exceeds remaining balance of $remaining",
            )
        }

        val arAccount = getArAccount(organizationId)
        val cashAccount = getCashAccount(organizationId)

        val receiptId = UUID.randomUUID().toString()
        val baseDecimals = currencyService.getCurrency(getBaseCurrency(organizationId)).decimalPlaces
        val newAmountReceived = invoice.amountReceived.add(request.amount)
        val fullyPaid = newAmountReceived.compareTo(invoice.totalAmount) >= 0
        val receiptBaseAmount =
            if (fullyPaid) {
                invoice.baseCurrencyAmount.subtract(invoice.baseCurrencyAmountReceived)
            } else {
                request.amount.multiply(invoice.exchangeRate).setScale(baseDecimals, RoundingMode.HALF_UP)
            }

        val journalEntry =
            journalEntryService.createSystemEntry(
                date = request.receiptDate,
                description = "Receipt for invoice ${invoice.invoiceNumber} - ${invoice.customerName}",
                organizationId = organizationId,
                lines =
                    listOf(
                        JournalEntryLine(
                            accountId = cashAccount.id,
                            accountCode = cashAccount.code,
                            accountName = cashAccount.name,
                            debit = receiptBaseAmount,
                            credit = BigDecimal.ZERO,
                            description = "Receipt - ${invoice.customerName}",
                        ),
                        JournalEntryLine(
                            accountId = arAccount.id,
                            accountCode = arAccount.code,
                            accountName = arAccount.name,
                            debit = BigDecimal.ZERO,
                            credit = receiptBaseAmount,
                            description = "Receipt - ${invoice.customerName}",
                        ),
                    ),
                sourceReference = "INVOICE-RECEIPT-$receiptId",
                createdBy = createdBy,
            )

        val receipt =
            invoiceReceiptRepository.save(
                InvoiceReceipt(
                    id = receiptId,
                    invoiceId = invoice.id,
                    receiptDate = request.receiptDate,
                    amount = request.amount,
                    baseCurrencyAmount = receiptBaseAmount,
                    exchangeRate = invoice.exchangeRate,
                    paymentMethod = request.paymentMethod,
                    referenceNumber = request.referenceNumber,
                    journalEntryId = journalEntry.id,
                    organizationId = organizationId,
                    createdBy = createdBy,
                ),
            )

        val newBaseAmountReceived = invoice.baseCurrencyAmountReceived.add(receiptBaseAmount)
        val newStatus = if (fullyPaid) InvoiceStatus.PAID else InvoiceStatus.PARTIALLY_PAID

        invoiceRepository.save(
            invoice.copy(
                amountReceived = newAmountReceived,
                baseCurrencyAmountReceived = newBaseAmountReceived,
                status = newStatus,
                paidAt = if (fullyPaid) LocalDateTime.now(ZoneOffset.UTC) else null,
            ),
        )

        return receipt
    }

    fun getReceipts(
        invoiceId: String,
        organizationId: String,
    ): List<InvoiceReceipt> {
        getInvoice(invoiceId, organizationId)
        return invoiceReceiptRepository.findByInvoiceIdAndOrganizationId(invoiceId, organizationId)
    }

    fun getAgingReport(
        organizationId: String,
        asOfDate: LocalDate = LocalDate.now(ZoneOffset.UTC),
    ): ArAgingReportResponse {
        val outstandingStatuses =
            listOf(InvoiceStatus.APPROVED, InvoiceStatus.PARTIALLY_PAID)
        val invoices = invoiceRepository.findByOrganizationIdAndStatusIn(organizationId, outstandingStatuses)

        val customerInvoices = invoices.groupBy { it.customerId }

        val customerAgingList =
            customerInvoices.map { (_, invoiceList) ->
                val customerName = invoiceList.first().customerName
                val customerId = invoiceList.first().customerId
                val bucket = calculateAgingBucket(invoiceList, asOfDate)
                CustomerAgingResponse(
                    customerId = customerId,
                    customerName = customerName,
                    aging = bucket,
                )
            }

        val totals =
            AgingBucket(
                current = customerAgingList.fold(BigDecimal.ZERO) { sum, c -> sum.add(c.aging.current) },
                days1to30 = customerAgingList.fold(BigDecimal.ZERO) { sum, c -> sum.add(c.aging.days1to30) },
                days31to60 = customerAgingList.fold(BigDecimal.ZERO) { sum, c -> sum.add(c.aging.days31to60) },
                days61to90 = customerAgingList.fold(BigDecimal.ZERO) { sum, c -> sum.add(c.aging.days61to90) },
                days90plus = customerAgingList.fold(BigDecimal.ZERO) { sum, c -> sum.add(c.aging.days90plus) },
                total = customerAgingList.fold(BigDecimal.ZERO) { sum, c -> sum.add(c.aging.total) },
            )

        return ArAgingReportResponse(
            asOfDate = asOfDate.toString(),
            customers = customerAgingList,
            totals = totals,
        )
    }

    private fun calculateAgingBucket(
        invoices: List<Invoice>,
        asOfDate: LocalDate,
    ): AgingBucket {
        var current = BigDecimal.ZERO
        var days1to30 = BigDecimal.ZERO
        var days31to60 = BigDecimal.ZERO
        var days61to90 = BigDecimal.ZERO
        var days90plus = BigDecimal.ZERO

        invoices.forEach { invoice ->
            val outstanding = invoice.baseCurrencyAmount.subtract(invoice.baseCurrencyAmountReceived)
            val daysOverdue = ChronoUnit.DAYS.between(invoice.dueDate, asOfDate)

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

    private fun absorbCreditDelta(
        lines: List<JournalEntryLine>,
        delta: BigDecimal,
    ): List<JournalEntryLine> {
        if (delta.signum() == 0 || lines.isEmpty()) return lines
        val mutable = lines.toMutableList()
        var remaining = delta
        val order = mutable.indices.sortedByDescending { mutable[it].credit }
        for (i in order) {
            if (remaining.signum() == 0) break
            val line = mutable[i]
            val applied =
                if (remaining.signum() < 0) remaining.max(line.credit.negate()) else remaining
            mutable[i] = line.copy(credit = line.credit.add(applied))
            remaining = remaining.subtract(applied)
        }
        return mutable
    }

    private fun getArAccount(organizationId: String): Account {
        val account =
            accountRepository.findByOrganizationIdAndCode(organizationId, "1100").orElseThrow {
                IllegalStateException("Accounts Receivable account (1100) not found")
            }
        if (!account.isActive) {
            throw BusinessRuleException("Accounts Receivable account (1100) is inactive")
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

    private fun getTaxPayableAccount(organizationId: String): Account {
        val account =
            accountRepository.findByOrganizationIdAndCode(organizationId, "2300").orElseThrow {
                IllegalStateException("Sales Tax Payable account (2300) not found")
            }
        if (!account.isActive) {
            throw BusinessRuleException("Sales Tax Payable account (2300) is inactive")
        }
        return account
    }

    private fun saveInvoiceWithRetry(
        organizationId: String,
        maxRetries: Int = 3,
        buildInvoice: (String) -> Invoice,
    ): Invoice {
        repeat(maxRetries) {
            val count = invoiceRepository.countByOrganizationId(organizationId)
            val invoiceNumber = "INV-${(count + 1).toString().padStart(4, '0')}"
            try {
                return invoiceRepository.save(buildInvoice(invoiceNumber))
            } catch (e: DuplicateKeyException) {
                if (it == maxRetries - 1) {
                    throw IllegalStateException("Failed to generate unique invoice number: $invoiceNumber", e)
                }
            }
        }
        throw IllegalStateException("Failed to generate unique invoice number")
    }
}
