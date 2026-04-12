package com.froilan.synectix.service

import com.froilan.synectix.dto.CreateInvoiceRequest
import com.froilan.synectix.dto.InvoiceLineRequest
import com.froilan.synectix.dto.RecordReceiptRequest
import com.froilan.synectix.model.Account
import com.froilan.synectix.model.AccountType
import com.froilan.synectix.model.Customer
import com.froilan.synectix.model.Invoice
import com.froilan.synectix.model.InvoiceLine
import com.froilan.synectix.model.InvoiceReceipt
import com.froilan.synectix.model.InvoiceStatus
import com.froilan.synectix.model.JournalEntry
import com.froilan.synectix.model.JournalEntryStatus
import com.froilan.synectix.model.PaymentMethod
import com.froilan.synectix.repository.AccountRepository
import com.froilan.synectix.repository.InvoiceReceiptRepository
import com.froilan.synectix.repository.InvoiceRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional

class InvoiceServiceTest {
    private lateinit var invoiceService: InvoiceService
    private lateinit var invoiceRepository: InvoiceRepository
    private lateinit var invoiceReceiptRepository: InvoiceReceiptRepository
    private lateinit var accountRepository: AccountRepository
    private lateinit var customerService: CustomerService
    private lateinit var journalEntryService: JournalEntryService

    private val orgId = "org-123"
    private val userId = "user-1"

    @BeforeEach
    fun setup() {
        invoiceRepository = mock(InvoiceRepository::class.java)
        invoiceReceiptRepository = mock(InvoiceReceiptRepository::class.java)
        accountRepository = mock(AccountRepository::class.java)
        customerService = mock(CustomerService::class.java)
        journalEntryService = mock(JournalEntryService::class.java)
        invoiceService =
            InvoiceService(
                invoiceRepository = invoiceRepository,
                invoiceReceiptRepository = invoiceReceiptRepository,
                accountRepository = accountRepository,
                customerService = customerService,
                journalEntryService = journalEntryService,
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
            assertThrows<IllegalArgumentException> {
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
            assertThrows<IllegalArgumentException> {
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
            assertThrows<IllegalArgumentException> {
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
            assertThrows<IllegalArgumentException> {
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
            assertThrows<IllegalArgumentException> {
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

        // inv-1: due May 15, asOf May 1 -> not overdue -> current
        assertThat(customerAging.aging.current).isEqualByComparingTo(BigDecimal("500.00"))
        // inv-2: due Apr 15, asOf May 1 -> 16 days overdue -> 1-30 bucket
        assertThat(customerAging.aging.days1to30).isEqualByComparingTo(BigDecimal("800.00"))
        // inv-3: due Feb 1, asOf May 1 -> 89 days overdue -> 61-90 bucket, outstanding = 800
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
        amountReceived: BigDecimal = BigDecimal.ZERO,
        dueDate: LocalDate = LocalDate.of(2026, 3, 31),
        journalEntryId: String? = null,
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
                    amount = totalAmount,
                ),
            ),
        totalAmount = totalAmount,
        amountReceived = amountReceived,
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
}
