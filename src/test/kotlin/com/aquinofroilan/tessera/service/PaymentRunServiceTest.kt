package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.CreatePaymentRunRequest
import com.aquinofroilan.tessera.dto.RecordPaymentRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.model.Account
import com.aquinofroilan.tessera.model.AccountType
import com.aquinofroilan.tessera.model.BankAccount
import com.aquinofroilan.tessera.model.Bill
import com.aquinofroilan.tessera.model.BillPayment
import com.aquinofroilan.tessera.model.BillStatus
import com.aquinofroilan.tessera.model.PaymentRun
import com.aquinofroilan.tessera.model.PaymentRunLineStatus
import com.aquinofroilan.tessera.model.PaymentRunStatus
import com.aquinofroilan.tessera.repository.PaymentRunRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional

class PaymentRunServiceTest {
    private lateinit var repository: PaymentRunRepository
    private lateinit var bankAccountService: BankAccountService
    private lateinit var billService: BillService
    private lateinit var accountService: AccountService
    private lateinit var service: PaymentRunService

    private val orgId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000100")
    private val userId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000101")
    private val bankId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000102")
    private val glAccountId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000103")

    @BeforeEach
    fun setup() {
        repository = mock(PaymentRunRepository::class.java)
        bankAccountService = mock(BankAccountService::class.java)
        billService = mock(BillService::class.java)
        accountService = mock(AccountService::class.java)
        whenever(repository.save(any<PaymentRun>())).thenAnswer { it.arguments[0] }
        whenever(repository.findByOrganizationIdAndCode(any(), any())).thenReturn(Optional.empty())
        whenever(bankAccountService.getBankAccount(bankId, orgId)).thenReturn(bankAccount())
        whenever(accountService.getAccount(glAccountId, orgId)).thenReturn(glAccount())
        service = PaymentRunService(repository, bankAccountService, billService, accountService)
    }

    @Test
    fun `create rolls up remaining bill amounts into total`() {
        whenever(
            billService.getBill(java.util.UUID.fromString("00000000-0000-0000-0000-000000000201"), orgId),
        ).thenReturn(bill(java.util.UUID.fromString("00000000-0000-0000-0000-000000000201"), "BILL-1", BigDecimal("500"), BigDecimal.ZERO))
        whenever(
            billService.getBill(java.util.UUID.fromString("00000000-0000-0000-0000-000000000202"), orgId),
        ).thenReturn(
            bill(java.util.UUID.fromString("00000000-0000-0000-0000-000000000202"), "BILL-2", BigDecimal("300"), BigDecimal("100")),
        )

        val run =
            service.createPaymentRun(
                CreatePaymentRunRequest(
                    code = "may-2026",
                    bankAccountId = bankId,
                    runDate = LocalDate.of(2026, 5, 31),
                    billIds =
                        listOf(
                            java.util.UUID.fromString("00000000-0000-0000-0000-000000000201"),
                            java.util.UUID.fromString("00000000-0000-0000-0000-000000000202"),
                        ),
                ),
                orgId,
                userId,
            )

        assertThat(run.code).isEqualTo("MAY-2026")
        assertThat(run.status).isEqualTo(PaymentRunStatus.DRAFT)
        assertThat(run.lines).hasSize(2)
        // b1 remaining=500, b2 remaining=200, total=700
        assertThat(run.totalAmount).isEqualByComparingTo(BigDecimal("700"))
    }

    @Test
    fun `create rejects DRAFT bill`() {
        whenever(billService.getBill(java.util.UUID.fromString("00000000-0000-0000-0000-000000000201"), orgId)).thenReturn(
            bill(
                java.util.UUID.fromString("00000000-0000-0000-0000-000000000201"),
                "BILL-1",
                BigDecimal("500"),
                BigDecimal.ZERO,
                status = BillStatus.DRAFT,
            ),
        )
        assertThatThrownBy {
            service.createPaymentRun(
                CreatePaymentRunRequest(
                    code = "X",
                    bankAccountId = bankId,
                    runDate = LocalDate.now(),
                    billIds = listOf(java.util.UUID.fromString("00000000-0000-0000-0000-000000000201")),
                ),
                orgId,
                userId,
            )
        }.isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `create rejects mismatched currency`() {
        whenever(billService.getBill(java.util.UUID.fromString("00000000-0000-0000-0000-000000000201"), orgId)).thenReturn(
            bill(
                java.util.UUID.fromString("00000000-0000-0000-0000-000000000201"),
                "BILL-1",
                BigDecimal("500"),
                BigDecimal.ZERO,
                currencyCode = "EUR",
            ),
        )
        assertThatThrownBy {
            service.createPaymentRun(
                CreatePaymentRunRequest(
                    code = "X",
                    bankAccountId = bankId,
                    runDate = LocalDate.now(),
                    billIds = listOf(java.util.UUID.fromString("00000000-0000-0000-0000-000000000201")),
                ),
                orgId,
                userId,
            )
        }.isInstanceOf(BusinessRuleException::class.java)
            .hasMessageContaining("currency")
    }

    @Test
    fun `create rejects duplicate bill IDs`() {
        assertThatThrownBy {
            service.createPaymentRun(
                CreatePaymentRunRequest(
                    code = "X",
                    bankAccountId = bankId,
                    runDate = LocalDate.now(),
                    billIds =
                        listOf(
                            java.util.UUID.fromString("00000000-0000-0000-0000-000000000201"),
                            java.util.UUID.fromString("00000000-0000-0000-0000-000000000201"),
                        ),
                ),
                orgId,
                userId,
            )
        }.isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `approve flips DRAFT to APPROVED`() {
        val draft = draftRun()
        whenever(repository.findById(java.util.UUID.fromString("00000000-0000-0000-0000-000000000203"))).thenReturn(Optional.of(draft))
        val approved = service.approvePaymentRun(java.util.UUID.fromString("00000000-0000-0000-0000-000000000203"), orgId, userId)
        assertThat(approved.status).isEqualTo(PaymentRunStatus.APPROVED)
        assertThat(approved.approvedBy).isEqualTo(userId)
    }

    @Test
    fun `approve rejects non-DRAFT`() {
        val approved = draftRun(status = PaymentRunStatus.APPROVED)
        whenever(repository.findById(java.util.UUID.fromString("00000000-0000-0000-0000-000000000203"))).thenReturn(Optional.of(approved))
        assertThatThrownBy { service.approvePaymentRun(java.util.UUID.fromString("00000000-0000-0000-0000-000000000203"), orgId, userId) }
            .isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `execute records bill payments with bank GL as cash override`() {
        val approved = draftRun(status = PaymentRunStatus.APPROVED)
        whenever(repository.findById(java.util.UUID.fromString("00000000-0000-0000-0000-000000000203"))).thenReturn(Optional.of(approved))
        whenever(
            billService.recordPayment(any(), any<RecordPaymentRequest>(), any(), any(), any<Account>()),
        ).thenReturn(payment())

        val executed = service.executePaymentRun(java.util.UUID.fromString("00000000-0000-0000-0000-000000000203"), orgId, userId)

        assertThat(executed.status).isEqualTo(PaymentRunStatus.EXECUTED)
        assertThat(executed.lines.all { it.status == PaymentRunLineStatus.PAID }).isTrue
        verify(billService).recordPayment(
            eq(approved.lines[0].billId),
            any<RecordPaymentRequest>(),
            eq(orgId),
            eq(userId),
            any<Account>(),
        )
        verify(bankAccountService).applyBalanceDelta(bankId, orgId, BigDecimal("-500"))
    }

    @Test
    fun `execute marks line FAILED when bill payment throws`() {
        val approved = draftRun(status = PaymentRunStatus.APPROVED)
        whenever(repository.findById(java.util.UUID.fromString("00000000-0000-0000-0000-000000000203"))).thenReturn(Optional.of(approved))
        whenever(
            billService.recordPayment(any(), any<RecordPaymentRequest>(), any(), any(), any<Account>()),
        ).thenThrow(BusinessRuleException("boom"))

        val executed = service.executePaymentRun(java.util.UUID.fromString("00000000-0000-0000-0000-000000000203"), orgId, userId)

        assertThat(executed.status).isEqualTo(PaymentRunStatus.EXECUTED)
        assertThat(executed.lines[0].status).isEqualTo(PaymentRunLineStatus.FAILED)
        assertThat(executed.lines[0].notes).contains("boom")
    }

    @Test
    fun `cancel rejects EXECUTED`() {
        val executed = draftRun(status = PaymentRunStatus.EXECUTED)
        whenever(repository.findById(java.util.UUID.fromString("00000000-0000-0000-0000-000000000203"))).thenReturn(Optional.of(executed))
        assertThatThrownBy { service.cancelPaymentRun(java.util.UUID.fromString("00000000-0000-0000-0000-000000000203"), orgId, userId) }
            .isInstanceOf(BusinessRuleException::class.java)
    }

    private fun bankAccount() =
        BankAccount(
            id = bankId,
            organizationId = orgId,
            code = "MAIN",
            name = "Main",
            currency = "USD",
            glAccountId = glAccountId,
            isActive = true,
            createdBy = userId,
        )

    private fun glAccount() =
        Account(
            id = glAccountId,
            code = "1000",
            name = "Cash - Main",
            type = AccountType.ASSET,
            organizationId = orgId,
            isActive = true,
        )

    private fun bill(
        id: java.util.UUID,
        billNumber: String,
        total: BigDecimal,
        paid: BigDecimal,
        status: BillStatus = BillStatus.APPROVED,
        currencyCode: String = "USD",
    ) = Bill(
        id = id,
        billNumber = billNumber,
        vendorId = java.util.UUID.randomUUID(),
        vendorName = "Vendor 1",
        organizationId = orgId,
        date = LocalDate.now(),
        dueDate = LocalDate.now().plusDays(30),
        status = status,
        taxAmount = BigDecimal.ZERO,
        totalAmount = total,
        amountPaid = paid,
        currencyCode = currencyCode,
        baseCurrencyAmount = total,
        baseCurrencyAmountPaid = paid,
        exchangeRate = BigDecimal.ONE,
        lines = emptyList(),
        createdBy = userId,
    )

    private fun draftRun(status: PaymentRunStatus = PaymentRunStatus.DRAFT) =
        PaymentRun(
            id = java.util.UUID.fromString("00000000-0000-0000-0000-000000000203"),
            organizationId = orgId,
            code = "MAY-2026",
            bankAccountId = bankId,
            runDate = LocalDate.of(2026, 5, 31),
            status = status,
            totalAmount = BigDecimal("500"),
            currency = "USD",
            lines =
                listOf(
                    com.aquinofroilan.tessera.model.PaymentRunLine(
                        lineNumber = 1,
                        billId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000201"),
                        vendorId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000205"),
                        vendorName = "Vendor 1",
                        billNumber = "BILL-1",
                        amount = BigDecimal("500"),
                    ),
                ),
            createdBy = userId,
        )

    private fun payment() =
        BillPayment(
            id = java.util.UUID.fromString("00000000-0000-0000-0000-000000000401"),
            billId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000201"),
            paymentDate = LocalDate.now(),
            amount = BigDecimal("500"),
            baseCurrencyAmount = BigDecimal("500"),
            exchangeRate = BigDecimal.ONE,
            paymentMethod = com.aquinofroilan.tessera.model.PaymentMethod.BANK_TRANSFER,
            referenceNumber = "PAYRUN-MAY-2026",
            journalEntryId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000402"),
            organizationId = orgId,
            createdBy = userId,
        )
}
