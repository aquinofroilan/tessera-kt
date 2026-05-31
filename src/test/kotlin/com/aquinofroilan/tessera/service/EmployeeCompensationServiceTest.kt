package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.CreateEmployeeCompensationRequest
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import com.aquinofroilan.tessera.model.Currency
import com.aquinofroilan.tessera.model.Employee
import com.aquinofroilan.tessera.model.EmployeeCompensation
import com.aquinofroilan.tessera.model.EmploymentStatus
import com.aquinofroilan.tessera.model.PayPeriod
import com.aquinofroilan.tessera.repository.EmployeeCompensationRepository
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

    private val orgId = "org-1"
    private val empId = "e1"

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
        createdBy = "u1",
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
                "u1",
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
