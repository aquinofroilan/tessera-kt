package com.aquinofroilan.tessera.domain.hr.service

import com.aquinofroilan.tessera.domain.finance.model.Currency
import com.aquinofroilan.tessera.domain.finance.service.CurrencyService
import com.aquinofroilan.tessera.domain.hr.dto.CreateEmployeeCompensationRequest
import com.aquinofroilan.tessera.domain.hr.model.Employee
import com.aquinofroilan.tessera.domain.hr.model.EmployeeCompensation
import com.aquinofroilan.tessera.domain.hr.model.EmploymentStatus
import com.aquinofroilan.tessera.domain.hr.model.PayPeriod
import com.aquinofroilan.tessera.domain.hr.repository.EmployeeCompensationRepository
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.LocalDate

class EmployeeCompensationServiceTest {
    private lateinit var repository: EmployeeCompensationRepository
    private lateinit var employeeService: EmployeeService
    private lateinit var positionService: PositionService
    private lateinit var currencyService: CurrencyService
    private lateinit var service: EmployeeCompensationService

    private val orgId = java.util.UUID.fromString("e5628ca4-87a8-3e6f-8ae2-20213cc7ef92")
    private val empId = java.util.UUID.fromString("535fd4f7-eb3b-30d3-b784-d16e1d946ff4")

    @BeforeEach
    fun setup() {
        repository = mock(EmployeeCompensationRepository::class.java)
        employeeService = mock(EmployeeService::class.java)
        positionService = mock(PositionService::class.java)
        currencyService = mock(CurrencyService::class.java)
        whenever(repository.save(any<EmployeeCompensation>())).thenAnswer { it.arguments[0] }
        whenever(employeeService.getEmployee(empId, orgId)).thenReturn(
            Employee(
                id = empId,
                employeeNumber = "EMP-0001",
                firstName = "Ada",
                lastName = "Lovelace",
                hireDate = LocalDate.of(2020, 1, 1),
                status = EmploymentStatus.ACTIVE,
                organizationId = orgId,
            ),
        )
        whenever(currencyService.getCurrency(any())).thenReturn(Currency("USD", "US Dollar", "$", 2))
        service = EmployeeCompensationService(repository, employeeService, positionService, currencyService)
    }

    private fun comp(
        rate: String,
        effective: LocalDate,
    ) = EmployeeCompensation(
        employeeId = empId,
        payRate = BigDecimal(rate),
        currency = "USD",
        payPeriod = PayPeriod.ANNUAL,
        effectiveDate = effective,
        organizationId = orgId,
        createdBy = java.util.UUID.fromString("d4763ac6-a6a6-34ed-aeb4-dd91bdcf7fbb"),
    )

    @Test
    fun `add normalizes currency and persists`() {
        val saved =
            service.addCompensation(
                empId,
                CreateEmployeeCompensationRequest(
                    payRate = BigDecimal("90000"),
                    currency = "usd",
                    payPeriod = PayPeriod.ANNUAL,
                    effectiveDate = LocalDate.of(2026, 1, 1),
                ),
                orgId,
                java.util.UUID.fromString("d4763ac6-a6a6-34ed-aeb4-dd91bdcf7fbb"),
            )

        assertThat(saved.currency).isEqualTo("USD")
        assertThat(saved.payRate).isEqualByComparingTo("90000")
    }

    @Test
    fun `current returns the latest record effective on or before the date`() {
        whenever(repository.findByOrganizationIdAndEmployeeIdOrderByEffectiveDateDesc(orgId, empId))
            .thenReturn(
                listOf(
                    comp("100000", LocalDate.of(2026, 1, 1)),
                    comp("90000", LocalDate.of(2025, 1, 1)),
                ),
            )

        val current = service.currentCompensation(empId, orgId, LocalDate.of(2025, 6, 1))
        assertThat(current.payRate).isEqualByComparingTo("90000")
    }

    @Test
    fun `current throws when no record is effective yet`() {
        whenever(repository.findByOrganizationIdAndEmployeeIdOrderByEffectiveDateDesc(orgId, empId))
            .thenReturn(listOf(comp("90000", LocalDate.of(2026, 1, 1))))

        assertThatThrownBy { service.currentCompensation(empId, orgId, LocalDate.of(2025, 6, 1)) }
            .isInstanceOf(ResourceNotFoundException::class.java)
    }
}
