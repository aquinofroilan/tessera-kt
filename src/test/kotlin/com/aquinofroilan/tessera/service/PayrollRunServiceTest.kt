package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.CreatePayrollRunRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.model.Account
import com.aquinofroilan.tessera.model.AccountType
import com.aquinofroilan.tessera.model.Currency
import com.aquinofroilan.tessera.model.Employee
import com.aquinofroilan.tessera.model.EmployeeCompensation
import com.aquinofroilan.tessera.model.EmploymentStatus
import com.aquinofroilan.tessera.model.JournalEntry
import com.aquinofroilan.tessera.model.JournalEntryLine
import com.aquinofroilan.tessera.model.Organizations
import com.aquinofroilan.tessera.model.PayPeriod
import com.aquinofroilan.tessera.model.PayrollRun
import com.aquinofroilan.tessera.model.PayrollRunLine
import com.aquinofroilan.tessera.model.PayrollRunStatus
import com.aquinofroilan.tessera.repository.AccountRepository
import com.aquinofroilan.tessera.repository.OrganizationRepository
import com.aquinofroilan.tessera.repository.PayrollRunRepository
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

    private val orgId = java.util.UUID.fromString("e5628ca4-87a8-3e6f-8ae2-20213cc7ef92")
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
        Account(id = java.util.UUID.fromString("ae92ad4d-a711-3aa4-9401-800b5a2fdac2"), code = code, name = "Account $code", type = AccountType.EXPENSE, organizationId = orgId)

    private fun draftRun() =
        PayrollRun(
            id = java.util.UUID.fromString("01cdc64c-d62f-3b43-8b91-1dd0193edcac"),
            runNumber = "PAY-0001",
            periodStart = LocalDate.of(2026, 1, 1),
            periodEnd = periodEnd,
            payDate = LocalDate.of(2026, 2, 1),
            organizationId = orgId,
            status = PayrollRunStatus.DRAFT,
            lines =
                listOf(
                    PayrollRunLine(
                        employeeId = java.util.UUID.fromString("afd0b036-625a-3aa8-b639-9dc8c8fff0ff"),
                        employeeNumber = "EMP-1",
                        employeeName = "First Last",
                        compensationId = java.util.UUID.fromString("9a3f84ab-0b36-31b0-a58a-311af949c743"),
                        grossAmount = BigDecimal("15000"),
                    ),
                ),
            totalGross = BigDecimal("15000"),
            currency = "USD",
            createdBy = java.util.UUID.fromString("d4763ac6-a6a6-34ed-aeb4-dd91bdcf7fbb"),
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

    private fun employee(id: java.util.UUID) =
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
        id: java.util.UUID,
        rate: String,
        period: PayPeriod,
        currency: String = "USD",
    ) = EmployeeCompensation(
        id = id,
        employeeId = java.util.UUID.fromString("ec794efe-a384-3b11-a0b6-ec8995bc6acc"),
        payRate = BigDecimal(rate),
        currency = currency,
        payPeriod = period,
        effectiveDate = LocalDate.of(2025, 1, 1),
        organizationId = orgId,
        createdBy = java.util.UUID.fromString("d4763ac6-a6a6-34ed-aeb4-dd91bdcf7fbb"),
    )

    private fun request() =
        CreatePayrollRunRequest(
            periodStart = LocalDate.of(2026, 1, 1),
            periodEnd = periodEnd,
            payDate = LocalDate.of(2026, 2, 1),
        )

    @Test
    fun `create snapshots active employees with monthly gross`() {
        val e1 = employee(java.util.UUID.fromString("afd0b036-625a-3aa8-b639-9dc8c8fff0ff"))
        val e2 = employee(java.util.UUID.fromString("9c45c2f1-1761-3daa-ad31-1ff8703ae846"))
        whenever(employeeService.listEmployees(orgId, EmploymentStatus.ACTIVE, null)).thenReturn(listOf(e1, e2))
        whenever(compensationService.currentCompensationOrNull(eq(java.util.UUID.fromString("afd0b036-625a-3aa8-b639-9dc8c8fff0ff")), eq(orgId), any()))
            .thenReturn(comp(java.util.UUID.randomUUID(), "120000", PayPeriod.ANNUAL)) // → 10000/mo
        whenever(compensationService.currentCompensationOrNull(eq(java.util.UUID.fromString("9c45c2f1-1761-3daa-ad31-1ff8703ae846")), eq(orgId), any()))
            .thenReturn(comp(java.util.UUID.randomUUID(), "5000", PayPeriod.MONTHLY)) // → 5000/mo

        val run = service.createPayrollRun(request(), orgId, java.util.UUID.fromString("d4763ac6-a6a6-34ed-aeb4-dd91bdcf7fbb"))

        assertThat(run.runNumber).isEqualTo("PAY-0001")
        assertThat(run.lines).hasSize(2)
        assertThat(run.totalGross).isEqualByComparingTo("15000")
        assertThat(run.currency).isEqualTo("USD")
    }

    @Test
    fun `create skips non-base-currency and hourly compensation`() {
        val e1 = employee(java.util.UUID.fromString("afd0b036-625a-3aa8-b639-9dc8c8fff0ff"))
        val e2 = employee(java.util.UUID.fromString("9c45c2f1-1761-3daa-ad31-1ff8703ae846"))
        whenever(employeeService.listEmployees(orgId, EmploymentStatus.ACTIVE, null)).thenReturn(listOf(e1, e2))
        whenever(compensationService.currentCompensationOrNull(eq(java.util.UUID.fromString("afd0b036-625a-3aa8-b639-9dc8c8fff0ff")), eq(orgId), any()))
            .thenReturn(comp(java.util.UUID.randomUUID(), "8000", PayPeriod.MONTHLY, currency = "EUR")) // skipped (currency)
        whenever(compensationService.currentCompensationOrNull(eq(java.util.UUID.fromString("9c45c2f1-1761-3daa-ad31-1ff8703ae846")), eq(orgId), any()))
            .thenReturn(comp(java.util.UUID.randomUUID(), "50", PayPeriod.HOURLY)) // skipped (hourly)

        assertThatThrownBy { service.createPayrollRun(request(), orgId, java.util.UUID.fromString("d4763ac6-a6a6-34ed-aeb4-dd91bdcf7fbb")) }
            .isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `approve posts a salary accrual debiting expense and crediting wages payable`() {
        whenever(repository.findById(java.util.UUID.fromString("01cdc64c-d62f-3b43-8b91-1dd0193edcac"))).thenReturn(Optional.of(draftRun()))
        whenever(accountRepository.findByOrganizationIdAndCode(orgId, "6000")).thenReturn(Optional.of(account("6000")))
        whenever(accountRepository.findByOrganizationIdAndCode(orgId, "2200")).thenReturn(Optional.of(account("2200")))
        whenever(journalEntryService.createSystemEntry(any(), any(), any(), any(), any(), any()))
            .thenReturn(mock(JournalEntry::class.java))

        val approved = service.approvePayrollRun(java.util.UUID.fromString("01cdc64c-d62f-3b43-8b91-1dd0193edcac"), orgId, java.util.UUID.fromString("339851d6-a2ee-38e3-9908-6d48907f4a92"))

        assertThat(approved.status).isEqualTo(PayrollRunStatus.APPROVED)
        val captor = argumentCaptor<List<JournalEntryLine>>()
        verify(journalEntryService).createSystemEntry(any(), any(), eq(orgId), captor.capture(), any(), any())
        val lines = captor.firstValue
        assertThat(lines.first { it.accountCode == "6000" }.debit).isEqualByComparingTo("15000")
        assertThat(lines.first { it.accountCode == "2200" }.credit).isEqualByComparingTo("15000")
    }

    @Test
    fun `pay posts wages payable debit and cash credit`() {
        val approved = draftRun().apply { status = PayrollRunStatus.APPROVED }
        whenever(repository.findById(java.util.UUID.fromString("01cdc64c-d62f-3b43-8b91-1dd0193edcac"))).thenReturn(Optional.of(approved))
        whenever(accountRepository.findByOrganizationIdAndCode(orgId, "2200")).thenReturn(Optional.of(account("2200")))
        whenever(accountRepository.findByOrganizationIdAndCode(orgId, "1000")).thenReturn(Optional.of(account("1000")))
        whenever(journalEntryService.createSystemEntry(any(), any(), any(), any(), any(), any()))
            .thenReturn(mock(JournalEntry::class.java))

        val paid = service.payPayrollRun(java.util.UUID.fromString("01cdc64c-d62f-3b43-8b91-1dd0193edcac"), orgId, java.util.UUID.fromString("339851d6-a2ee-38e3-9908-6d48907f4a92"))

        assertThat(paid.status).isEqualTo(PayrollRunStatus.PAID)
        val captor = argumentCaptor<List<JournalEntryLine>>()
        verify(journalEntryService).createSystemEntry(any(), any(), eq(orgId), captor.capture(), any(), any())
        val lines = captor.firstValue
        assertThat(lines.first { it.accountCode == "2200" }.debit).isEqualByComparingTo("15000")
        assertThat(lines.first { it.accountCode == "1000" }.credit).isEqualByComparingTo("15000")
    }

    @Test
    fun `cannot cancel an approved run`() {
        whenever(repository.findById(java.util.UUID.fromString("01cdc64c-d62f-3b43-8b91-1dd0193edcac"))).thenReturn(Optional.of(draftRun().apply { status = PayrollRunStatus.APPROVED }))
        assertThatThrownBy { service.cancelPayrollRun(java.util.UUID.fromString("01cdc64c-d62f-3b43-8b91-1dd0193edcac"), orgId) }
            .isInstanceOf(BusinessRuleException::class.java)
    }
}
