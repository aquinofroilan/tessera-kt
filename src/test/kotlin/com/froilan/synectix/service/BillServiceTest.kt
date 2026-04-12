package com.froilan.synectix.service

import com.froilan.synectix.dto.BillLineRequest
import com.froilan.synectix.dto.CreateBillRequest
import com.froilan.synectix.dto.RecordPaymentRequest
import com.froilan.synectix.exception.BusinessRuleException
import com.froilan.synectix.model.Account
import com.froilan.synectix.model.AccountType
import com.froilan.synectix.model.Bill
import com.froilan.synectix.model.BillLine
import com.froilan.synectix.model.BillPayment
import com.froilan.synectix.model.BillStatus
import com.froilan.synectix.model.JournalEntry
import com.froilan.synectix.model.JournalEntryStatus
import com.froilan.synectix.model.PaymentMethod
import com.froilan.synectix.model.Vendor
import com.froilan.synectix.repository.AccountRepository
import com.froilan.synectix.repository.BillPaymentRepository
import com.froilan.synectix.repository.BillRepository
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

class BillServiceTest {
    private lateinit var billService: BillService
    private lateinit var billRepository: BillRepository
    private lateinit var billPaymentRepository: BillPaymentRepository
    private lateinit var accountRepository: AccountRepository
    private lateinit var vendorService: VendorService
    private lateinit var journalEntryService: JournalEntryService

    private val orgId = "org-123"
    private val userId = "user-1"

    @BeforeEach
    fun setup() {
        billRepository = mock(BillRepository::class.java)
        billPaymentRepository = mock(BillPaymentRepository::class.java)
        accountRepository = mock(AccountRepository::class.java)
        vendorService = mock(VendorService::class.java)
        journalEntryService = mock(JournalEntryService::class.java)
        billService =
            BillService(
                billRepository = billRepository,
                billPaymentRepository = billPaymentRepository,
                accountRepository = accountRepository,
                vendorService = vendorService,
                journalEntryService = journalEntryService,
            )
    }

    @Test
    fun `create should save bill as DRAFT with correct total`() {
        val vendor = createVendor()
        val expenseAccount = createAccount("acc-1", "5000", "Office Supplies", AccountType.EXPENSE)

        `when`(vendorService.getVendor("v-1", orgId)).thenReturn(vendor)
        `when`(accountRepository.findAllById(listOf("acc-1")))
            .thenReturn(listOf(expenseAccount))
        `when`(billRepository.countByOrganizationId(orgId)).thenReturn(0L)
        `when`(billRepository.save(any<Bill>())).thenAnswer { it.arguments[0] }

        val request =
            CreateBillRequest(
                vendorId = "v-1",
                date = LocalDate.of(2026, 3, 1),
                dueDate = LocalDate.of(2026, 3, 31),
                lines =
                    listOf(
                        BillLineRequest(accountId = "acc-1", amount = BigDecimal("250.00")),
                    ),
            )

        val result = billService.createBill(request, orgId, userId)

        assertThat(result.status).isEqualTo(BillStatus.DRAFT)
        assertThat(result.vendorName).isEqualTo("Acme Corp")
        assertThat(result.totalAmount).isEqualByComparingTo(BigDecimal("250.00"))
        assertThat(result.amountPaid).isEqualByComparingTo(BigDecimal.ZERO)
        assertThat(result.billNumber).isEqualTo("BILL-0001")
        assertThat(result.lines).hasSize(1)
        assertThat(result.lines[0].accountCode).isEqualTo("5000")
    }

    @Test
    fun `create should reject inactive vendor`() {
        val vendor = createVendor(isActive = false)
        `when`(vendorService.getVendor("v-1", orgId)).thenReturn(vendor)

        val request =
            CreateBillRequest(
                vendorId = "v-1",
                date = LocalDate.of(2026, 3, 1),
                dueDate = LocalDate.of(2026, 3, 31),
                lines =
                    listOf(
                        BillLineRequest(accountId = "acc-1", amount = BigDecimal("100.00")),
                    ),
            )

        val exception =
            assertThrows<BusinessRuleException> {
                billService.createBill(request, orgId, userId)
            }
        assertThat(exception.message).contains("inactive vendor")
    }

    @Test
    fun `approve should post journal entry and update status`() {
        val bill = createBill()
        val apAccount = createAccount("acc-ap", "2000", "Accounts Payable", AccountType.LIABILITY)
        val mockEntry =
            JournalEntry(
                id = "je-1",
                entryNumber = "JE-0001",
                date = bill.date,
                description = "Bill BILL-0001 - Acme Corp",
                organizationId = orgId,
                status = JournalEntryStatus.POSTED,
                lines = emptyList(),
                createdBy = userId,
            )

        `when`(billRepository.findById("bill-1")).thenReturn(Optional.of(bill))
        `when`(accountRepository.findByOrganizationIdAndCode(orgId, "2000"))
            .thenReturn(Optional.of(apAccount))
        `when`(journalEntryService.createSystemEntry(any(), any(), any(), any(), any(), any()))
            .thenReturn(mockEntry)
        `when`(billRepository.save(any<Bill>())).thenAnswer { it.arguments[0] }

        val result = billService.approveBill("bill-1", orgId, userId)

        assertThat(result.status).isEqualTo(BillStatus.APPROVED)
        assertThat(result.journalEntryId).isEqualTo("je-1")
        assertThat(result.approvedBy).isEqualTo(userId)

        verify(journalEntryService).createSystemEntry(any(), any(), any(), any(), any(), any())
    }

    @Test
    fun `approve should reject non-draft bill`() {
        val bill = createBill(status = BillStatus.APPROVED)
        `when`(billRepository.findById("bill-1")).thenReturn(Optional.of(bill))

        val exception =
            assertThrows<BusinessRuleException> {
                billService.approveBill("bill-1", orgId, userId)
            }
        assertThat(exception.message).contains("Only draft")
    }

    @Test
    fun `void should void journal entry for approved bill`() {
        val bill = createBill(status = BillStatus.APPROVED, journalEntryId = "je-1")
        val voidedEntry =
            JournalEntry(
                id = "je-1",
                entryNumber = "JE-0001",
                date = bill.date,
                description = "Bill BILL-0001",
                organizationId = orgId,
                status = JournalEntryStatus.VOIDED,
                lines = emptyList(),
                createdBy = userId,
            )

        `when`(billRepository.findById("bill-1")).thenReturn(Optional.of(bill))
        `when`(journalEntryService.voidJournalEntry("je-1", orgId, "Duplicate"))
            .thenReturn(voidedEntry)
        `when`(billRepository.save(any<Bill>())).thenAnswer { it.arguments[0] }

        val result = billService.voidBill("bill-1", orgId, "Duplicate", userId)

        assertThat(result.status).isEqualTo(BillStatus.VOID)
        assertThat(result.voidReason).isEqualTo("Duplicate")
        verify(journalEntryService).voidJournalEntry("je-1", orgId, "Duplicate")
    }

    @Test
    fun `void should allow voiding draft without journal entry`() {
        val bill = createBill(status = BillStatus.DRAFT)
        `when`(billRepository.findById("bill-1")).thenReturn(Optional.of(bill))
        `when`(billRepository.save(any<Bill>())).thenAnswer { it.arguments[0] }

        val result = billService.voidBill("bill-1", orgId, "Not needed", userId)

        assertThat(result.status).isEqualTo(BillStatus.VOID)
    }

    @Test
    fun `void should reject bill with payments`() {
        val bill =
            createBill(
                status = BillStatus.PARTIALLY_PAID,
                amountPaid = BigDecimal("100.00"),
            )
        `when`(billRepository.findById("bill-1")).thenReturn(Optional.of(bill))

        val exception =
            assertThrows<BusinessRuleException> {
                billService.voidBill("bill-1", orgId, "Cancel", userId)
            }
        assertThat(exception.message).contains("recorded payments")
    }

    @Test
    fun `recordPayment should create payment and update bill status`() {
        val bill = createBill(status = BillStatus.APPROVED)
        val apAccount = createAccount("acc-ap", "2000", "Accounts Payable", AccountType.LIABILITY)
        val cashAccount = createAccount("acc-cash", "1000", "Cash", AccountType.ASSET)
        val mockEntry = createMockJournalEntry()

        `when`(billRepository.findById("bill-1")).thenReturn(Optional.of(bill))
        `when`(accountRepository.findByOrganizationIdAndCode(orgId, "2000"))
            .thenReturn(Optional.of(apAccount))
        `when`(accountRepository.findByOrganizationIdAndCode(orgId, "1000"))
            .thenReturn(Optional.of(cashAccount))
        `when`(journalEntryService.createSystemEntry(any(), any(), any(), any(), any(), any()))
            .thenReturn(mockEntry)
        `when`(billPaymentRepository.save(any<BillPayment>())).thenAnswer { it.arguments[0] }
        `when`(billRepository.save(any<Bill>())).thenAnswer { it.arguments[0] }

        val request =
            RecordPaymentRequest(
                paymentDate = LocalDate.of(2026, 3, 15),
                amount = BigDecimal("200.00"),
                paymentMethod = PaymentMethod.BANK_TRANSFER,
            )

        val payment = billService.recordPayment("bill-1", request, orgId, userId)

        assertThat(payment.amount).isEqualByComparingTo(BigDecimal("200.00"))
        assertThat(payment.paymentMethod).isEqualTo(PaymentMethod.BANK_TRANSFER)

        val billCaptor = argumentCaptor<Bill>()
        verify(billRepository).save(billCaptor.capture())
        assertThat(billCaptor.firstValue.status).isEqualTo(BillStatus.PARTIALLY_PAID)
        assertThat(billCaptor.firstValue.amountPaid).isEqualByComparingTo(BigDecimal("200.00"))
    }

    @Test
    fun `recordPayment should mark bill as PAID when fully paid`() {
        val bill = createBill(status = BillStatus.APPROVED)
        val apAccount = createAccount("acc-ap", "2000", "Accounts Payable", AccountType.LIABILITY)
        val cashAccount = createAccount("acc-cash", "1000", "Cash", AccountType.ASSET)
        val mockEntry = createMockJournalEntry()

        `when`(billRepository.findById("bill-1")).thenReturn(Optional.of(bill))
        `when`(accountRepository.findByOrganizationIdAndCode(orgId, "2000"))
            .thenReturn(Optional.of(apAccount))
        `when`(accountRepository.findByOrganizationIdAndCode(orgId, "1000"))
            .thenReturn(Optional.of(cashAccount))
        `when`(journalEntryService.createSystemEntry(any(), any(), any(), any(), any(), any()))
            .thenReturn(mockEntry)
        `when`(billPaymentRepository.save(any<BillPayment>())).thenAnswer { it.arguments[0] }
        `when`(billRepository.save(any<Bill>())).thenAnswer { it.arguments[0] }

        val request =
            RecordPaymentRequest(
                paymentDate = LocalDate.of(2026, 3, 15),
                amount = BigDecimal("500.00"),
                paymentMethod = PaymentMethod.CHECK,
            )

        billService.recordPayment("bill-1", request, orgId, userId)

        val billCaptor = argumentCaptor<Bill>()
        verify(billRepository).save(billCaptor.capture())
        assertThat(billCaptor.firstValue.status).isEqualTo(BillStatus.PAID)
        assertThat(billCaptor.firstValue.paidAt).isNotNull()
    }

    @Test
    fun `recordPayment should reject overpayment`() {
        val bill = createBill(status = BillStatus.APPROVED)
        `when`(billRepository.findById("bill-1")).thenReturn(Optional.of(bill))

        val request =
            RecordPaymentRequest(
                paymentDate = LocalDate.of(2026, 3, 15),
                amount = BigDecimal("999.00"),
                paymentMethod = PaymentMethod.CASH,
            )

        val exception =
            assertThrows<BusinessRuleException> {
                billService.recordPayment("bill-1", request, orgId, userId)
            }
        assertThat(exception.message).contains("exceeds remaining balance")
    }

    @Test
    fun `recordPayment should reject draft bill`() {
        val bill = createBill(status = BillStatus.DRAFT)
        `when`(billRepository.findById("bill-1")).thenReturn(Optional.of(bill))

        val request =
            RecordPaymentRequest(
                paymentDate = LocalDate.of(2026, 3, 15),
                amount = BigDecimal("100.00"),
                paymentMethod = PaymentMethod.CASH,
            )

        val exception =
            assertThrows<BusinessRuleException> {
                billService.recordPayment("bill-1", request, orgId, userId)
            }
        assertThat(exception.message).contains("approved or partially paid")
    }

    @Test
    fun `aging report should bucket bills by overdue days`() {
        val asOfDate = LocalDate.of(2026, 5, 1)
        val bills =
            listOf(
                createBill(
                    id = "bill-1",
                    vendorId = "v-1",
                    dueDate = LocalDate.of(2026, 5, 15),
                    totalAmount = BigDecimal("100.00"),
                    status = BillStatus.APPROVED,
                ),
                createBill(
                    id = "bill-2",
                    vendorId = "v-1",
                    dueDate = LocalDate.of(2026, 4, 15),
                    totalAmount = BigDecimal("200.00"),
                    status = BillStatus.APPROVED,
                ),
                createBill(
                    id = "bill-3",
                    vendorId = "v-1",
                    dueDate = LocalDate.of(2026, 2, 1),
                    totalAmount = BigDecimal("300.00"),
                    status = BillStatus.PARTIALLY_PAID,
                    amountPaid = BigDecimal("50.00"),
                ),
            )

        val outstandingStatuses = listOf(BillStatus.APPROVED, BillStatus.PARTIALLY_PAID)
        `when`(billRepository.findByOrganizationIdAndStatusIn(orgId, outstandingStatuses))
            .thenReturn(bills)

        val report = billService.getAgingReport(orgId, asOfDate)

        assertThat(report.vendors).hasSize(1)
        val vendorAging = report.vendors[0]
        assertThat(vendorAging.vendorName).isEqualTo("Acme Corp")

        // bill-1: due May 15, asOf May 1 -> not overdue -> current
        assertThat(vendorAging.aging.current).isEqualByComparingTo(BigDecimal("100.00"))
        // bill-2: due Apr 15, asOf May 1 -> 16 days overdue -> 1-30 bucket
        assertThat(vendorAging.aging.days1to30).isEqualByComparingTo(BigDecimal("200.00"))
        // bill-3: due Feb 1, asOf May 1 -> 89 days overdue -> 61-90 bucket, outstanding = 250
        assertThat(vendorAging.aging.days61to90).isEqualByComparingTo(BigDecimal("250.00"))

        assertThat(report.totals.total).isEqualByComparingTo(BigDecimal("550.00"))
    }

    private fun createVendor(
        id: String = "v-1",
        isActive: Boolean = true,
    ) = Vendor(
        id = id,
        name = "Acme Corp",
        contactName = "John",
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

    private fun createBill(
        id: String = "bill-1",
        vendorId: String = "v-1",
        status: BillStatus = BillStatus.DRAFT,
        totalAmount: BigDecimal = BigDecimal("500.00"),
        amountPaid: BigDecimal = BigDecimal.ZERO,
        dueDate: LocalDate = LocalDate.of(2026, 3, 31),
        journalEntryId: String? = null,
    ) = Bill(
        id = id,
        billNumber = "BILL-0001",
        vendorId = vendorId,
        vendorName = "Acme Corp",
        date = LocalDate.of(2026, 3, 1),
        dueDate = dueDate,
        organizationId = orgId,
        status = status,
        lines =
            listOf(
                BillLine(
                    accountId = "acc-1",
                    accountCode = "5000",
                    accountName = "Office Supplies",
                    amount = totalAmount,
                ),
            ),
        totalAmount = totalAmount,
        amountPaid = amountPaid,
        journalEntryId = journalEntryId,
        createdBy = userId,
    )

    private fun createMockJournalEntry() =
        JournalEntry(
            id = "je-1",
            entryNumber = "JE-0001",
            date = LocalDate.of(2026, 3, 1),
            description = "Mock entry",
            organizationId = orgId,
            status = JournalEntryStatus.POSTED,
            lines = emptyList(),
            createdBy = userId,
        )
}
