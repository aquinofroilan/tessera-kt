package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.model.BankAccount
import com.aquinofroilan.tessera.model.Bill
import com.aquinofroilan.tessera.model.BillStatus
import com.aquinofroilan.tessera.model.Invoice
import com.aquinofroilan.tessera.model.InvoiceStatus
import com.aquinofroilan.tessera.repository.BankAccountRepository
import com.aquinofroilan.tessera.repository.BillRepository
import com.aquinofroilan.tessera.repository.InvoiceRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.LocalDate

class CashFlowForecastServiceTest {
    private lateinit var bankRepository: BankAccountRepository
    private lateinit var invoiceRepository: InvoiceRepository
    private lateinit var billRepository: BillRepository
    private lateinit var service: CashFlowForecastService

    private val orgId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000100")
    private val userId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000101")
    private val asOf = LocalDate.of(2026, 6, 1)
    private val horizon = LocalDate.of(2026, 7, 31)

    @BeforeEach
    fun setup() {
        bankRepository = mock(BankAccountRepository::class.java)
        invoiceRepository = mock(InvoiceRepository::class.java)
        billRepository = mock(BillRepository::class.java)
        service = CashFlowForecastService(bankRepository, invoiceRepository, billRepository)
    }

    @Test
    fun `forecast returns starting cash from active bank accounts`() {
        whenever(bankRepository.findByOrganizationIdAndIsActive(orgId, true)).thenReturn(
            listOf(
                bankAccount("MAIN", BigDecimal("10000")),
                bankAccount("SAVINGS", BigDecimal("5000")),
            ),
        )
        whenever(invoiceRepository.findByOrganizationIdAndStatusIn(any(), any())).thenReturn(emptyList())
        whenever(billRepository.findByOrganizationIdAndStatusIn(any(), any())).thenReturn(emptyList())

        val r = service.forecast(orgId, asOf, horizon)

        assertThat(r.startingCash).isEqualByComparingTo(BigDecimal("15000"))
        assertThat(r.totalInflow).isEqualByComparingTo(BigDecimal.ZERO)
        assertThat(r.totalOutflow).isEqualByComparingTo(BigDecimal.ZERO)
        assertThat(r.projectedEndingCash).isEqualByComparingTo(BigDecimal("15000"))
    }

    @Test
    fun `forecast nets future inflows and outflows`() {
        whenever(bankRepository.findByOrganizationIdAndIsActive(orgId, true)).thenReturn(
            listOf(bankAccount("MAIN", BigDecimal("1000"))),
        )
        whenever(invoiceRepository.findByOrganizationIdAndStatusIn(any(), any())).thenReturn(
            listOf(invoice(LocalDate.of(2026, 6, 15), BigDecimal("500"))),
        )
        whenever(billRepository.findByOrganizationIdAndStatusIn(any(), any())).thenReturn(
            listOf(bill(LocalDate.of(2026, 6, 20), BigDecimal("200"))),
        )

        val r = service.forecast(orgId, asOf, horizon)

        assertThat(r.totalInflow).isEqualByComparingTo(BigDecimal("500"))
        assertThat(r.totalOutflow).isEqualByComparingTo(BigDecimal("200"))
        assertThat(r.projectedEndingCash).isEqualByComparingTo(BigDecimal("1300"))
    }

    @Test
    fun `forecast surfaces overdue AR and AP separately`() {
        whenever(bankRepository.findByOrganizationIdAndIsActive(orgId, true)).thenReturn(
            listOf(bankAccount("MAIN", BigDecimal("1000"))),
        )
        whenever(invoiceRepository.findByOrganizationIdAndStatusIn(any(), any())).thenReturn(
            listOf(
                invoice(LocalDate.of(2026, 5, 1), BigDecimal("300")),
                invoice(LocalDate.of(2026, 6, 30), BigDecimal("400")),
            ),
        )
        whenever(billRepository.findByOrganizationIdAndStatusIn(any(), any())).thenReturn(
            listOf(bill(LocalDate.of(2026, 4, 15), BigDecimal("250"))),
        )

        val r = service.forecast(orgId, asOf, horizon)

        assertThat(r.overdueAr).isEqualByComparingTo(BigDecimal("300"))
        assertThat(r.overdueAp).isEqualByComparingTo(BigDecimal("250"))
        // Overdue items are NOT in future buckets.
        assertThat(r.totalInflow).isEqualByComparingTo(BigDecimal("400"))
        assertThat(r.totalOutflow).isEqualByComparingTo(BigDecimal.ZERO)
    }

    @Test
    fun `forecast rejects horizon at or before asOf`() {
        assertThatThrownBy { service.forecast(orgId, asOf, asOf) }
            .isInstanceOf(BusinessRuleException::class.java)
        assertThatThrownBy { service.forecast(orgId, asOf, asOf.minusDays(1)) }
            .isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `forecast skips fully-settled items`() {
        whenever(bankRepository.findByOrganizationIdAndIsActive(orgId, true)).thenReturn(
            listOf(bankAccount("MAIN", BigDecimal("0"))),
        )
        // Invoice fully paid -> remaining=0 -> skipped.
        val fullyPaid = invoice(LocalDate.of(2026, 6, 15), BigDecimal("500")).apply { this.amountReceived = BigDecimal("500") }
        whenever(invoiceRepository.findByOrganizationIdAndStatusIn(any(), any())).thenReturn(listOf(fullyPaid))
        whenever(billRepository.findByOrganizationIdAndStatusIn(any(), any())).thenReturn(emptyList())

        val r = service.forecast(orgId, asOf, horizon)
        assertThat(r.totalInflow).isEqualByComparingTo(BigDecimal.ZERO)
    }

    private fun bankAccount(
        code: String,
        balance: BigDecimal,
    ) = BankAccount(
        organizationId = orgId,
        code = code,
        name = code,
        currency = "USD",
        glAccountId = java.util.UUID.randomUUID(),
        currentBalance = balance,
        createdBy = userId,
    )

    private fun invoice(
        dueDate: LocalDate,
        amount: BigDecimal,
    ) = Invoice(
        invoiceNumber = "INV-1",
        customerId = java.util.UUID.randomUUID(),
        customerName = "Customer 1",
        date = dueDate.minusDays(30),
        dueDate = dueDate,
        organizationId = orgId,
        status = InvoiceStatus.APPROVED,
        lines = emptyList(),
        totalAmount = amount,
        amountReceived = BigDecimal.ZERO,
        currencyCode = "USD",
        baseCurrencyAmount = amount,
        baseCurrencyAmountReceived = BigDecimal.ZERO,
        exchangeRate = BigDecimal.ONE,
        createdBy = userId,
    )

    private fun bill(
        dueDate: LocalDate,
        amount: BigDecimal,
    ) = Bill(
        billNumber = "BILL-1",
        vendorId = java.util.UUID.randomUUID(),
        vendorName = "Vendor 1",
        date = dueDate.minusDays(30),
        dueDate = dueDate,
        organizationId = orgId,
        status = BillStatus.APPROVED,
        lines = emptyList(),
        totalAmount = amount,
        taxAmount = BigDecimal.ZERO,
        amountPaid = BigDecimal.ZERO,
        currencyCode = "USD",
        baseCurrencyAmount = amount,
        baseCurrencyAmountPaid = BigDecimal.ZERO,
        exchangeRate = BigDecimal.ONE,
        createdBy = userId,
    )
}
