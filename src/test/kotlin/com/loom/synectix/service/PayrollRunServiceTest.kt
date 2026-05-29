package com.loom.synectix.service

import com.loom.synectix.dto.CreatePayrollRunRequest
import com.loom.synectix.exception.BusinessRuleException
import com.loom.synectix.model.Account
import com.loom.synectix.model.AccountType
import com.loom.synectix.model.Currency
import com.loom.synectix.model.Employee
import com.loom.synectix.model.EmployeeCompensation
import com.loom.synectix.model.EmploymentStatus
import com.loom.synectix.model.JournalEntry
import com.loom.synectix.model.JournalEntryLine
import com.loom.synectix.model.Organizations
import com.loom.synectix.model.PayPeriod
import com.loom.synectix.model.PayrollRun
import com.loom.synectix.model.PayrollRunLine
import com.loom.synectix.model.PayrollRunStatus
import com.loom.synectix.repository.AccountRepository
import com.loom.synectix.repository.OrganizationRepository
import com.loom.synectix.repository.PayrollRunRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional

class PayrollRunServiceTest {
    private lateinit var repository: PayrollRunRepository
    private lateinit var employeeService: EmployeeService
    private lateinit var compensationService: EmployeeCompensationService
    private lateinit var currencyService: CurrencyService
    private lateinit var organizationRepository: OrganizationRepository
    private lateinit var accountRepository: AccountRepository
    private lateinit var journalEntryService: JournalEntryService
    private lateinit var service: PayrollRunService

    private val orgId = "org-1"
    private val periodEnd = LocalDate.of(2026, 1, 31)

    @BeforeEach
    fun setup() {
        repository = mock(PayrollRunRepository::class.java)
        employeeService = mock(EmployeeService::class.java)
        compensationService = mock(EmployeeCompensationService::class.java)
        currencyService = mock(CurrencyService::class.java)
        organizationRepository = mock(OrganizationRepository::class.java)
        whenever(repository.countByOrganizationId(orgId)).thenReturn(0L)
        whenever(repository.save(any<PayrollRun>())).thenAnswer { it.arguments[0] }
        whenever(currencyService.getCurrency(any())).thenReturn(Currency("USD", "US Dollar", "$", 2))
        whenever(organizationRepository.findById(orgId)).thenReturn(Optional.of(organization()))
        accountRepository = mock(AccountRepository::class.java)
        journalEntryService = mock(JournalEntryService::class.java)
        service =
            PayrollRunService(
                repository,
                employeeService,
                compensationService,
                currencyService,
                organizationRepository,
                accountRepository,
                journalEntryService,
            )
    }

    private fun account(code: String) =
        Account(id = "acc-$code", code = code, name = "Account $code", type = AccountType.EXPENSE, organizationId = orgId)

    private fun draftRun() =
        PayrollRun(
            id = "run1",
            runNumber = "PAY-0001",
            periodStart = LocalDate.of(2026, 1, 1),
            periodEnd = periodEnd,
            payDate = LocalDate.of(2026, 2, 1),
            organizationId = orgId,
            status = PayrollRunStatus.DRAFT,
            lines =
                listOf(
                    PayrollRunLine(
                        employeeId = "1",
                        employeeNumber = "EMP-1",
                        employeeName = "First Last",
                        compensationId = "c1",
                        grossAmount = BigDecimal("15000"),
                    ),
                ),
            totalGross = BigDecimal("15000"),
            currency = "USD",
            createdBy = "u1",
        )

    private fun organization() =
        Organizations(
            uuid = orgId,
            orgSlug = "slug",
            name = "Test",
            legalName = "Test",
            tradeName = "Test",
            baseCurrency = "USD",
            fiscalYearStart = LocalDateTime.of(2026, 1, 1, 0, 0),
            timezone = "UTC",
        )

    private fun employee(id: String) =
        Employee(
            id = id,
            employeeNumber = "EMP-$id",
            firstName = "First$id",
            lastName = "Last$id",
            hireDate = LocalDate.of(2020, 1, 1),
            status = EmploymentStatus.ACTIVE,
            organizationId = orgId,
        )

    private fun comp(
        id: String,
        rate: String,
        period: PayPeriod,
        currency: String = "USD",
    ) = EmployeeCompensation(
        id = id,
        employeeId = "x",
        payRate = BigDecimal(rate),
        currency = currency,
        payPeriod = period,
        effectiveDate = LocalDate.of(2025, 1, 1),
        organizationId = orgId,
        createdBy = "u1",
    )

    private fun request() =
        CreatePayrollRunRequest(
            periodStart = LocalDate.of(2026, 1, 1),
            periodEnd = periodEnd,
            payDate = LocalDate.of(2026, 2, 1),
        )

    @Test
    fun `create snapshots active employees with monthly gross`() {
        val e1 = employee("1")
        val e2 = employee("2")
        whenever(employeeService.listEmployees(orgId, EmploymentStatus.ACTIVE, null)).thenReturn(listOf(e1, e2))
        whenever(compensationService.currentCompensationOrNull(eq("1"), eq(orgId), any()))
            .thenReturn(comp("c1", "120000", PayPeriod.ANNUAL)) // → 10000/mo
        whenever(compensationService.currentCompensationOrNull(eq("2"), eq(orgId), any()))
            .thenReturn(comp("c2", "5000", PayPeriod.MONTHLY)) // → 5000/mo

        val run = service.createPayrollRun(request(), orgId, "u1")

        assertThat(run.runNumber).isEqualTo("PAY-0001")
        assertThat(run.lines).hasSize(2)
        assertThat(run.totalGross).isEqualByComparingTo("15000")
        assertThat(run.currency).isEqualTo("USD")
    }

    @Test
    fun `create skips non-base-currency and hourly compensation`() {
        val e1 = employee("1")
        val e2 = employee("2")
        whenever(employeeService.listEmployees(orgId, EmploymentStatus.ACTIVE, null)).thenReturn(listOf(e1, e2))
        whenever(compensationService.currentCompensationOrNull(eq("1"), eq(orgId), any()))
            .thenReturn(comp("c1", "8000", PayPeriod.MONTHLY, currency = "EUR")) // skipped (currency)
        whenever(compensationService.currentCompensationOrNull(eq("2"), eq(orgId), any()))
            .thenReturn(comp("c2", "50", PayPeriod.HOURLY)) // skipped (hourly)

        assertThatThrownBy { service.createPayrollRun(request(), orgId, "u1") }
            .isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `approve posts a salary accrual debiting expense and crediting wages payable`() {
        whenever(repository.findById("run1")).thenReturn(Optional.of(draftRun()))
        whenever(accountRepository.findByOrganizationIdAndCode(orgId, "6000")).thenReturn(Optional.of(account("6000")))
        whenever(accountRepository.findByOrganizationIdAndCode(orgId, "2200")).thenReturn(Optional.of(account("2200")))
        whenever(journalEntryService.createSystemEntry(any(), any(), any(), any(), any(), any()))
            .thenReturn(mock(JournalEntry::class.java))

        val approved = service.approvePayrollRun("run1", orgId, "mgr")

        assertThat(approved.status).isEqualTo(PayrollRunStatus.APPROVED)
        val captor = argumentCaptor<List<JournalEntryLine>>()
        verify(journalEntryService).createSystemEntry(any(), any(), eq(orgId), captor.capture(), any(), any())
        val lines = captor.firstValue
        assertThat(lines.first { it.accountCode == "6000" }.debit).isEqualByComparingTo("15000")
        assertThat(lines.first { it.accountCode == "2200" }.credit).isEqualByComparingTo("15000")
    }

    @Test
    fun `pay posts wages payable debit and cash credit`() {
        val approved = draftRun().copy(status = PayrollRunStatus.APPROVED)
        whenever(repository.findById("run1")).thenReturn(Optional.of(approved))
        whenever(accountRepository.findByOrganizationIdAndCode(orgId, "2200")).thenReturn(Optional.of(account("2200")))
        whenever(accountRepository.findByOrganizationIdAndCode(orgId, "1000")).thenReturn(Optional.of(account("1000")))
        whenever(journalEntryService.createSystemEntry(any(), any(), any(), any(), any(), any()))
            .thenReturn(mock(JournalEntry::class.java))

        val paid = service.payPayrollRun("run1", orgId, "mgr")

        assertThat(paid.status).isEqualTo(PayrollRunStatus.PAID)
        val captor = argumentCaptor<List<JournalEntryLine>>()
        verify(journalEntryService).createSystemEntry(any(), any(), eq(orgId), captor.capture(), any(), any())
        val lines = captor.firstValue
        assertThat(lines.first { it.accountCode == "2200" }.debit).isEqualByComparingTo("15000")
        assertThat(lines.first { it.accountCode == "1000" }.credit).isEqualByComparingTo("15000")
    }

    @Test
    fun `cannot cancel an approved run`() {
        whenever(repository.findById("run1")).thenReturn(Optional.of(draftRun().copy(status = PayrollRunStatus.APPROVED)))
        assertThatThrownBy { service.cancelPayrollRun("run1", orgId) }
            .isInstanceOf(BusinessRuleException::class.java)
    }
}
