package com.loom.synectix.service

import com.loom.synectix.dto.CreateInvoiceRequest
import com.loom.synectix.dto.InvoiceLineRequest
import com.loom.synectix.dto.RecordReceiptRequest
import com.loom.synectix.exception.BusinessRuleException
import com.loom.synectix.model.Account
import com.loom.synectix.model.AccountType
import com.loom.synectix.model.Currency
import com.loom.synectix.model.Customer
import com.loom.synectix.model.Invoice
import com.loom.synectix.model.InvoiceLine
import com.loom.synectix.model.InvoiceReceipt
import com.loom.synectix.model.InvoiceStatus
import com.loom.synectix.model.JournalEntry
import com.loom.synectix.model.JournalEntryLine
import com.loom.synectix.model.JournalEntryStatus
import com.loom.synectix.model.Organizations
import com.loom.synectix.model.PaymentMethod
import com.loom.synectix.repository.AccountRepository
import com.loom.synectix.repository.InvoiceReceiptRepository
import com.loom.synectix.repository.InvoiceRepository
import com.loom.synectix.repository.OrganizationRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional

class InvoiceServiceTest {
    private lateinit var invoiceService: InvoiceService
    private lateinit var invoiceRepository: InvoiceRepository
    private lateinit var invoiceReceiptRepository: InvoiceReceiptRepository
    private lateinit var accountRepository: AccountRepository
    private lateinit var customerService: CustomerService
    private lateinit var journalEntryService: JournalEntryService
    private lateinit var taxGroupService: TaxGroupService
    private lateinit var organizationRepository: OrganizationRepository
    private lateinit var currencyService: CurrencyService
    private lateinit var exchangeRateService: ExchangeRateService

    private val orgId = "org-123"
    private val userId = "user-1"

    @BeforeEach
    fun setup() {
        invoiceRepository = mock(InvoiceRepository::class.java)
        invoiceReceiptRepository = mock(InvoiceReceiptRepository::class.java)
        accountRepository = mock(AccountRepository::class.java)
        customerService = mock(CustomerService::class.java)
        journalEntryService = mock(JournalEntryService::class.java)
        taxGroupService = mock(TaxGroupService::class.java)
        organizationRepository = mock(OrganizationRepository::class.java)
        currencyService = mock(CurrencyService::class.java)
        exchangeRateService = mock(ExchangeRateService::class.java)
        `when`(taxGroupService.calculateTaxAmount(anyOrNull(), any(), any()))
            .thenReturn(java.math.BigDecimal.ZERO)
        `when`(organizationRepository.findById(orgId)).thenReturn(
            Optional.of(
                Organizations(
                    uuid = orgId,
                    orgSlug = "test",
                    name = "Test",
                    legalName = "Test",
                    tradeName = "Test",
                    baseCurrency = "USD",
                    fiscalYearStart = LocalDateTime.of(2026, 1, 1, 0, 0),
                    timezone = "UTC",
                ),
            ),
        )
        `when`(currencyService.getCurrency("USD")).thenReturn(Currency("USD", "US Dollar", "\$", 2))
        `when`(currencyService.getCurrency("PHP")).thenReturn(Currency("PHP", "Philippine Peso", "₱", 2))
        invoiceService =
            InvoiceService(
                invoiceRepository = invoiceRepository,
                invoiceReceiptRepository = invoiceReceiptRepository,
                accountRepository = accountRepository,
                customerService = customerService,
                journalEntryService = journalEntryService,
                taxGroupService = taxGroupService,
                organizationRepository = organizationRepository,
                currencyService = currencyService,
                exchangeRateService = exchangeRateService,
            )
    }

    @Test
    fun `create should save invoice as DRAFT with correct total`() {
        val customer = createCustomer()
        val revenueAccount = createAccount("acc-1", "4100", "Service Revenue", AccountType.REVENUE)

        `when`(customerService.getCustomer("c-1", orgId)).thenReturn(customer)
        `when`(accountRepository.findAllById(listOf("acc-1")))
            .thenReturn(listOf(revenueAccount))
        `when`(invoiceRepository.countByOrganizationId(orgId)).thenReturn(0L)
        `when`(invoiceRepository.save(any<Invoice>())).thenAnswer { it.arguments[0] }

        val request =
            CreateInvoiceRequest(
                customerId = "c-1",
                date = LocalDate.of(2026, 3, 1),
                dueDate = LocalDate.of(2026, 3, 31),
                lines =
                    listOf(
                        InvoiceLineRequest(accountId = "acc-1", amount = BigDecimal("2000.00")),
                    ),
            )

        val result = invoiceService.createInvoice(request, orgId, userId)

        assertThat(result.status).isEqualTo(InvoiceStatus.DRAFT)
        assertThat(result.customerName).isEqualTo("BigCorp")
        assertThat(result.totalAmount).isEqualByComparingTo(BigDecimal("2000.00"))
        assertThat(result.amountReceived).isEqualByComparingTo(BigDecimal.ZERO)
        assertThat(result.invoiceNumber).isEqualTo("INV-0001")
        assertThat(result.lines).hasSize(1)
        assertThat(result.lines[0].accountCode).isEqualTo("4100")
    }

    @Test
    fun `create should reject inactive customer`() {
        val customer = createCustomer(isActive = false)
        `when`(customerService.getCustomer("c-1", orgId)).thenReturn(customer)

        val request =
            CreateInvoiceRequest(
                customerId = "c-1",
                date = LocalDate.of(2026, 3, 1),
                dueDate = LocalDate.of(2026, 3, 31),
                lines =
                    listOf(
                        InvoiceLineRequest(accountId = "acc-1", amount = BigDecimal("100.00")),
                    ),
            )

        val exception =
            assertThrows<BusinessRuleException> {
                invoiceService.createInvoice(request, orgId, userId)
            }
        assertThat(exception.message).contains("inactive customer")
    }

    @Test
    fun `approve should post journal entry and update status`() {
        val invoice = createInvoice()
        val arAccount = createAccount("acc-ar", "1100", "Accounts Receivable", AccountType.ASSET)
        val mockEntry = createMockJournalEntry()

        `when`(invoiceRepository.findById("inv-1")).thenReturn(Optional.of(invoice))
        `when`(accountRepository.findByOrganizationIdAndCode(orgId, "1100"))
            .thenReturn(Optional.of(arAccount))
        `when`(journalEntryService.createSystemEntry(any(), any(), any(), any(), any(), any()))
            .thenReturn(mockEntry)
        `when`(invoiceRepository.save(any<Invoice>())).thenAnswer { it.arguments[0] }

        val result = invoiceService.approveInvoice("inv-1", orgId, userId)

        assertThat(result.status).isEqualTo(InvoiceStatus.APPROVED)
        assertThat(result.journalEntryId).isEqualTo("je-1")
        assertThat(result.approvedBy).isEqualTo(userId)

        verify(journalEntryService).createSystemEntry(any(), any(), any(), any(), any(), any())
    }

    @Test
    fun `approve should reject non-draft invoice`() {
        val invoice = createInvoice(status = InvoiceStatus.APPROVED)
        `when`(invoiceRepository.findById("inv-1")).thenReturn(Optional.of(invoice))

        val exception =
            assertThrows<BusinessRuleException> {
                invoiceService.approveInvoice("inv-1", orgId, userId)
            }
        assertThat(exception.message).contains("Only draft")
    }

    @Test
    fun `void should void journal entry for approved invoice`() {
        val invoice = createInvoice(status = InvoiceStatus.APPROVED, journalEntryId = "je-1")
        val voidedEntry = createMockJournalEntry(status = JournalEntryStatus.VOIDED)

        `when`(invoiceRepository.findById("inv-1")).thenReturn(Optional.of(invoice))
        `when`(journalEntryService.voidJournalEntry("je-1", orgId, "Duplicate"))
            .thenReturn(voidedEntry)
        `when`(invoiceRepository.save(any<Invoice>())).thenAnswer { it.arguments[0] }

        val result = invoiceService.voidInvoice("inv-1", orgId, "Duplicate", userId)

        assertThat(result.status).isEqualTo(InvoiceStatus.VOID)
        assertThat(result.voidReason).isEqualTo("Duplicate")
        verify(journalEntryService).voidJournalEntry("je-1", orgId, "Duplicate")
    }

    @Test
    fun `void should allow voiding draft without journal entry`() {
        val invoice = createInvoice(status = InvoiceStatus.DRAFT)
        `when`(invoiceRepository.findById("inv-1")).thenReturn(Optional.of(invoice))
        `when`(invoiceRepository.save(any<Invoice>())).thenAnswer { it.arguments[0] }

        val result = invoiceService.voidInvoice("inv-1", orgId, "Not needed", userId)

        assertThat(result.status).isEqualTo(InvoiceStatus.VOID)
    }

    @Test
    fun `void should reject invoice with receipts`() {
        val invoice =
            createInvoice(
                status = InvoiceStatus.PARTIALLY_PAID,
                amountReceived = BigDecimal("500.00"),
            )
        `when`(invoiceRepository.findById("inv-1")).thenReturn(Optional.of(invoice))

        val exception =
            assertThrows<BusinessRuleException> {
                invoiceService.voidInvoice("inv-1", orgId, "Cancel", userId)
            }
        assertThat(exception.message).contains("recorded receipts")
    }

    @Test
    fun `recordReceipt should create receipt and update invoice status`() {
        val invoice = createInvoice(status = InvoiceStatus.APPROVED)
        val arAccount = createAccount("acc-ar", "1100", "Accounts Receivable", AccountType.ASSET)
        val cashAccount = createAccount("acc-cash", "1000", "Cash", AccountType.ASSET)
        val mockEntry = createMockJournalEntry()

        `when`(invoiceRepository.findById("inv-1")).thenReturn(Optional.of(invoice))
        `when`(accountRepository.findByOrganizationIdAndCode(orgId, "1100"))
            .thenReturn(Optional.of(arAccount))
        `when`(accountRepository.findByOrganizationIdAndCode(orgId, "1000"))
            .thenReturn(Optional.of(cashAccount))
        `when`(journalEntryService.createSystemEntry(any(), any(), any(), any(), any(), any()))
            .thenReturn(mockEntry)
        `when`(invoiceReceiptRepository.save(any<InvoiceReceipt>())).thenAnswer { it.arguments[0] }
        `when`(invoiceRepository.save(any<Invoice>())).thenAnswer { it.arguments[0] }

        val request =
            RecordReceiptRequest(
                receiptDate = LocalDate.of(2026, 3, 15),
                amount = BigDecimal("1000.00"),
                paymentMethod = PaymentMethod.BANK_TRANSFER,
            )

        val receipt = invoiceService.recordReceipt("inv-1", request, orgId, userId)

        assertThat(receipt.amount).isEqualByComparingTo(BigDecimal("1000.00"))
        assertThat(receipt.paymentMethod).isEqualTo(PaymentMethod.BANK_TRANSFER)

        val invoiceCaptor = argumentCaptor<Invoice>()
        verify(invoiceRepository).save(invoiceCaptor.capture())
        assertThat(invoiceCaptor.firstValue.status).isEqualTo(InvoiceStatus.PARTIALLY_PAID)
        assertThat(invoiceCaptor.firstValue.amountReceived).isEqualByComparingTo(BigDecimal("1000.00"))
    }

    @Test
    fun `recordReceipt should mark invoice as PAID when fully paid`() {
        val invoice = createInvoice(status = InvoiceStatus.APPROVED)
        val arAccount = createAccount("acc-ar", "1100", "Accounts Receivable", AccountType.ASSET)
        val cashAccount = createAccount("acc-cash", "1000", "Cash", AccountType.ASSET)
        val mockEntry = createMockJournalEntry()

        `when`(invoiceRepository.findById("inv-1")).thenReturn(Optional.of(invoice))
        `when`(accountRepository.findByOrganizationIdAndCode(orgId, "1100"))
            .thenReturn(Optional.of(arAccount))
        `when`(accountRepository.findByOrganizationIdAndCode(orgId, "1000"))
            .thenReturn(Optional.of(cashAccount))
        `when`(journalEntryService.createSystemEntry(any(), any(), any(), any(), any(), any()))
            .thenReturn(mockEntry)
        `when`(invoiceReceiptRepository.save(any<InvoiceReceipt>())).thenAnswer { it.arguments[0] }
        `when`(invoiceRepository.save(any<Invoice>())).thenAnswer { it.arguments[0] }

        val request =
            RecordReceiptRequest(
                receiptDate = LocalDate.of(2026, 3, 15),
                amount = BigDecimal("2000.00"),
                paymentMethod = PaymentMethod.CHECK,
            )

        invoiceService.recordReceipt("inv-1", request, orgId, userId)

        val invoiceCaptor = argumentCaptor<Invoice>()
        verify(invoiceRepository).save(invoiceCaptor.capture())
        assertThat(invoiceCaptor.firstValue.status).isEqualTo(InvoiceStatus.PAID)
        assertThat(invoiceCaptor.firstValue.paidAt).isNotNull()
    }

    @Test
    fun `recordReceipt should reject overpayment`() {
        val invoice = createInvoice(status = InvoiceStatus.APPROVED)
        `when`(invoiceRepository.findById("inv-1")).thenReturn(Optional.of(invoice))

        val request =
            RecordReceiptRequest(
                receiptDate = LocalDate.of(2026, 3, 15),
                amount = BigDecimal("9999.00"),
                paymentMethod = PaymentMethod.CASH,
            )

        val exception =
            assertThrows<BusinessRuleException> {
                invoiceService.recordReceipt("inv-1", request, orgId, userId)
            }
        assertThat(exception.message).contains("exceeds remaining balance")
    }

    @Test
    fun `recordReceipt should reject draft invoice`() {
        val invoice = createInvoice(status = InvoiceStatus.DRAFT)
        `when`(invoiceRepository.findById("inv-1")).thenReturn(Optional.of(invoice))

        val request =
            RecordReceiptRequest(
                receiptDate = LocalDate.of(2026, 3, 15),
                amount = BigDecimal("100.00"),
                paymentMethod = PaymentMethod.CASH,
            )

        val exception =
            assertThrows<BusinessRuleException> {
                invoiceService.recordReceipt("inv-1", request, orgId, userId)
            }
        assertThat(exception.message).contains("approved or partially paid")
    }

    @Test
    fun `aging report should bucket invoices by overdue days`() {
        val asOfDate = LocalDate.of(2026, 5, 1)
        val invoices =
            listOf(
                createInvoice(
                    id = "inv-1",
                    customerId = "c-1",
                    dueDate = LocalDate.of(2026, 5, 15),
                    totalAmount = BigDecimal("500.00"),
                    status = InvoiceStatus.APPROVED,
                ),
                createInvoice(
                    id = "inv-2",
                    customerId = "c-1",
                    dueDate = LocalDate.of(2026, 4, 15),
                    totalAmount = BigDecimal("800.00"),
                    status = InvoiceStatus.APPROVED,
                ),
                createInvoice(
                    id = "inv-3",
                    customerId = "c-1",
                    dueDate = LocalDate.of(2026, 2, 1),
                    totalAmount = BigDecimal("1000.00"),
                    status = InvoiceStatus.PARTIALLY_PAID,
                    amountReceived = BigDecimal("200.00"),
                ),
            )

        val outstandingStatuses = listOf(InvoiceStatus.APPROVED, InvoiceStatus.PARTIALLY_PAID)
        `when`(invoiceRepository.findByOrganizationIdAndStatusIn(orgId, outstandingStatuses))
            .thenReturn(invoices)

        val report = invoiceService.getAgingReport(orgId, asOfDate)

        assertThat(report.customers).hasSize(1)
        val customerAging = report.customers[0]
        assertThat(customerAging.customerName).isEqualTo("BigCorp")

        assertThat(customerAging.aging.current).isEqualByComparingTo(BigDecimal("500.00"))
        assertThat(customerAging.aging.days1to30).isEqualByComparingTo(BigDecimal("800.00"))
        assertThat(customerAging.aging.days61to90).isEqualByComparingTo(BigDecimal("800.00"))

        assertThat(report.totals.total).isEqualByComparingTo(BigDecimal("2100.00"))
    }

    private fun createCustomer(
        id: String = "c-1",
        isActive: Boolean = true,
    ) = Customer(
        id = id,
        name = "BigCorp",
        contactName = "Jane",
        paymentTermDays = 30,
        organizationId = orgId,
        isActive = isActive,
    )

    @Test
    fun `create with taxGroupId should compute and store tax`() {
        val customer = createCustomer()
        val revenueAccount = createAccount("acc-1", "4100", "Service Revenue", AccountType.REVENUE)

        `when`(customerService.getCustomer("c-1", orgId)).thenReturn(customer)
        `when`(accountRepository.findAllById(listOf("acc-1")))
            .thenReturn(listOf(revenueAccount))
        `when`(invoiceRepository.countByOrganizationId(orgId)).thenReturn(0L)
        `when`(invoiceRepository.save(any<Invoice>())).thenAnswer { it.arguments[0] }
        `when`(taxGroupService.calculateTaxAmount(any(), any(), any()))
            .thenReturn(BigDecimal("170.00"))

        val request =
            CreateInvoiceRequest(
                customerId = "c-1",
                date = LocalDate.of(2026, 3, 1),
                dueDate = LocalDate.of(2026, 3, 31),
                taxGroupId = "tg-1",
                lines = listOf(InvoiceLineRequest(accountId = "acc-1", amount = BigDecimal("2000.00"))),
            )

        val result = invoiceService.createInvoice(request, orgId, userId)

        assertThat(result.taxGroupId).isEqualTo("tg-1")
        assertThat(result.taxAmount).isEqualByComparingTo(BigDecimal("170.00"))
        assertThat(result.totalAmount).isEqualByComparingTo(BigDecimal("2170.00"))
    }

    @Test
    fun `approve with tax should include tax payable credit and correct AR debit`() {
        val invoice =
            createInvoice(
                status = InvoiceStatus.DRAFT,
                totalAmount = BigDecimal("2170.00"),
                taxAmount = BigDecimal("170.00"),
            )
        val arAccount = createAccount("acc-ar", "1100", "Accounts Receivable", AccountType.ASSET)
        val taxPayableAccount = createAccount("acc-tax", "2300", "Sales Tax Payable", AccountType.LIABILITY)
        val mockEntry = createMockJournalEntry()

        `when`(invoiceRepository.findById("inv-1")).thenReturn(Optional.of(invoice))
        `when`(accountRepository.findByOrganizationIdAndCode(orgId, "1100"))
            .thenReturn(Optional.of(arAccount))
        `when`(accountRepository.findByOrganizationIdAndCode(orgId, "2300"))
            .thenReturn(Optional.of(taxPayableAccount))
        `when`(journalEntryService.createSystemEntry(any(), any(), any(), any(), any(), any()))
            .thenReturn(mockEntry)
        `when`(invoiceRepository.save(any<Invoice>())).thenAnswer { it.arguments[0] }

        invoiceService.approveInvoice("inv-1", orgId, userId)

        val linesCaptor = argumentCaptor<List<JournalEntryLine>>()
        verify(journalEntryService).createSystemEntry(
            any(),
            any(),
            any(),
            linesCaptor.capture(),
            any(),
            any(),
        )
        val lines = linesCaptor.firstValue

        val arDebit = lines.find { it.accountCode == "1100" }
        assertThat(arDebit).isNotNull
        assertThat(arDebit!!.debit).isEqualByComparingTo(BigDecimal("2170.00"))

        val revenueCredit = lines.find { it.accountCode == "4100" }
        assertThat(revenueCredit).isNotNull
        assertThat(revenueCredit!!.credit).isEqualByComparingTo(BigDecimal("2000.00"))

        val taxCredit = lines.find { it.accountCode == "2300" }
        assertThat(taxCredit).isNotNull
        assertThat(taxCredit!!.credit).isEqualByComparingTo(BigDecimal("170.00"))
    }

    private fun createAccount(
        id: String,
        code: String,
        name: String,
        type: AccountType,
    ) = Account(
        id = id,
        code = code,
        name = name,
        type = type,
        organizationId = orgId,
    )

    private fun createInvoice(
        id: String = "inv-1",
        customerId: String = "c-1",
        status: InvoiceStatus = InvoiceStatus.DRAFT,
        totalAmount: BigDecimal = BigDecimal("2000.00"),
        taxAmount: BigDecimal = BigDecimal.ZERO,
        amountReceived: BigDecimal = BigDecimal.ZERO,
        dueDate: LocalDate = LocalDate.of(2026, 3, 31),
        journalEntryId: String? = null,
        currencyCode: String = "USD",
        exchangeRate: BigDecimal = BigDecimal.ONE,
        baseCurrencyAmount: BigDecimal = totalAmount,
        baseCurrencyTaxAmount: BigDecimal = taxAmount,
        baseCurrencyAmountReceived: BigDecimal = amountReceived,
    ) = Invoice(
        id = id,
        invoiceNumber = "INV-0001",
        customerId = customerId,
        customerName = "BigCorp",
        date = LocalDate.of(2026, 3, 1),
        dueDate = dueDate,
        organizationId = orgId,
        status = status,
        lines =
            listOf(
                InvoiceLine(
                    accountId = "acc-1",
                    accountCode = "4100",
                    accountName = "Service Revenue",
                    amount = totalAmount.subtract(taxAmount),
                ),
            ),
        totalAmount = totalAmount,
        taxAmount = taxAmount,
        amountReceived = amountReceived,
        currencyCode = currencyCode,
        exchangeRate = exchangeRate,
        baseCurrencyAmount = baseCurrencyAmount,
        baseCurrencyTaxAmount = baseCurrencyTaxAmount,
        baseCurrencyAmountReceived = baseCurrencyAmountReceived,
        journalEntryId = journalEntryId,
        createdBy = userId,
    )

    private fun createMockJournalEntry(status: JournalEntryStatus = JournalEntryStatus.POSTED) =
        JournalEntry(
            id = "je-1",
            entryNumber = "JE-0001",
            date = LocalDate.of(2026, 3, 1),
            description = "Mock entry",
            organizationId = orgId,
            status = status,
            lines = emptyList(),
            createdBy = userId,
        )

    @Test
    fun `create in foreign currency should lock rate and compute baseCurrencyAmount`() {
        val customer = createCustomer()
        val revenueAccount = createAccount("acc-1", "4100", "Service Revenue", AccountType.REVENUE)

        `when`(customerService.getCustomer("c-1", orgId)).thenReturn(customer)
        `when`(accountRepository.findAllById(listOf("acc-1"))).thenReturn(listOf(revenueAccount))
        `when`(invoiceRepository.countByOrganizationId(orgId)).thenReturn(0L)
        `when`(invoiceRepository.save(any<Invoice>())).thenAnswer { it.arguments[0] }
        `when`(exchangeRateService.getRate(orgId, "PHP", "USD", LocalDate.of(2026, 3, 1)))
            .thenReturn(BigDecimal("0.018"))

        val request =
            CreateInvoiceRequest(
                customerId = "c-1",
                date = LocalDate.of(2026, 3, 1),
                dueDate = LocalDate.of(2026, 3, 31),
                currencyCode = "PHP",
                lines = listOf(InvoiceLineRequest(accountId = "acc-1", amount = BigDecimal("10000.00"))),
            )

        val result = invoiceService.createInvoice(request, orgId, userId)

        assertThat(result.currencyCode).isEqualTo("PHP")
        assertThat(result.totalAmount).isEqualByComparingTo(BigDecimal("10000.00"))
        assertThat(result.exchangeRate).isEqualByComparingTo(BigDecimal("0.018"))
        assertThat(result.baseCurrencyAmount).isEqualByComparingTo(BigDecimal("180.00"))
    }

    @Test
    fun `approve in foreign currency should post AR debit in base currency`() {
        val invoice =
            createInvoice(
                status = InvoiceStatus.DRAFT,
                totalAmount = BigDecimal("10000.00"),
                currencyCode = "PHP",
                exchangeRate = BigDecimal("0.018"),
                baseCurrencyAmount = BigDecimal("180.00"),
            )
        val arAccount = createAccount("acc-ar", "1100", "Accounts Receivable", AccountType.ASSET)
        val mockEntry = createMockJournalEntry()

        `when`(invoiceRepository.findById("inv-1")).thenReturn(Optional.of(invoice))
        `when`(accountRepository.findByOrganizationIdAndCode(orgId, "1100"))
            .thenReturn(Optional.of(arAccount))
        `when`(journalEntryService.createSystemEntry(any(), any(), any(), any(), any(), any()))
            .thenReturn(mockEntry)
        `when`(invoiceRepository.save(any<Invoice>())).thenAnswer { it.arguments[0] }

        invoiceService.approveInvoice("inv-1", orgId, userId)

        val linesCaptor = argumentCaptor<List<JournalEntryLine>>()
        verify(journalEntryService).createSystemEntry(any(), any(), any(), linesCaptor.capture(), any(), any())
        val arLine = linesCaptor.firstValue.first { it.accountCode == "1100" }
        assertThat(arLine.debit).isEqualByComparingTo(BigDecimal("180.00"))
    }

    @Test
    fun `receipt in foreign currency should post base amount via locked rate`() {
        val invoice =
            createInvoice(
                status = InvoiceStatus.APPROVED,
                totalAmount = BigDecimal("10000.00"),
                currencyCode = "PHP",
                exchangeRate = BigDecimal("0.018"),
                baseCurrencyAmount = BigDecimal("180.00"),
            )
        val arAccount = createAccount("acc-ar", "1100", "Accounts Receivable", AccountType.ASSET)
        val cashAccount = createAccount("acc-cash", "1000", "Cash", AccountType.ASSET)
        val mockEntry = createMockJournalEntry()

        `when`(invoiceRepository.findById("inv-1")).thenReturn(Optional.of(invoice))
        `when`(accountRepository.findByOrganizationIdAndCode(orgId, "1100")).thenReturn(Optional.of(arAccount))
        `when`(accountRepository.findByOrganizationIdAndCode(orgId, "1000")).thenReturn(Optional.of(cashAccount))
        `when`(journalEntryService.createSystemEntry(any(), any(), any(), any(), any(), any()))
            .thenReturn(mockEntry)
        `when`(invoiceReceiptRepository.save(any<InvoiceReceipt>())).thenAnswer { it.arguments[0] }
        `when`(invoiceRepository.save(any<Invoice>())).thenAnswer { it.arguments[0] }

        val receipt =
            invoiceService.recordReceipt(
                "inv-1",
                RecordReceiptRequest(
                    receiptDate = LocalDate.of(2026, 3, 15),
                    amount = BigDecimal("5000.00"),
                    paymentMethod = PaymentMethod.BANK_TRANSFER,
                ),
                orgId,
                userId,
            )

        assertThat(receipt.amount).isEqualByComparingTo(BigDecimal("5000.00"))
        assertThat(receipt.baseCurrencyAmount).isEqualByComparingTo(BigDecimal("90.00"))
        val invoiceCaptor = argumentCaptor<Invoice>()
        verify(invoiceRepository).save(invoiceCaptor.capture())
        assertThat(invoiceCaptor.firstValue.baseCurrencyAmountReceived).isEqualByComparingTo(BigDecimal("90.00"))
    }
}
