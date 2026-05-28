package com.loom.synectix.service

import com.loom.synectix.dto.CreateExchangeRateRequest
import com.loom.synectix.exception.BusinessRuleException
import com.loom.synectix.exception.ResourceNotFoundException
import com.loom.synectix.model.Currency
import com.loom.synectix.model.ExchangeRate
import com.loom.synectix.model.ExchangeRateSource
import com.loom.synectix.repository.ExchangeRateRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional

class ExchangeRateServiceTest {
    private lateinit var service: ExchangeRateService
    private lateinit var repository: ExchangeRateRepository
    private lateinit var currencyService: CurrencyService

    private val orgId = "org-1"
    private val today = LocalDate.of(2026, 4, 19)

    @BeforeEach
    fun setup() {
        repository = mock(ExchangeRateRepository::class.java)
        currencyService = mock(CurrencyService::class.java)
        `when`(currencyService.getCurrency(any())).thenAnswer {
            val code = it.arguments[0] as String
            Currency(code = code, name = code, symbol = code, decimalPlaces = 2)
        }
        service = ExchangeRateService(repository, currencyService)
    }

    @Test
    fun `getRate returns ONE for same currency`() {
        val rate = service.getRate(orgId, "USD", "USD", today)
        assertThat(rate).isEqualByComparingTo(BigDecimal.ONE)
    }

    @Test
    fun `getRate returns direct rate when present`() {
        val direct = rateOf("USD", "PHP", BigDecimal("57.50"), today.minusDays(2))
        `when`(
            repository.findTopByOrganizationIdAndFromCurrencyAndToCurrencyAndAsOfDateLessThanEqualOrderByAsOfDateDesc(
                orgId,
                "USD",
                "PHP",
                today,
            ),
        ).thenReturn(Optional.of(direct))

        val rate = service.getRate(orgId, "USD", "PHP", today)

        assertThat(rate).isEqualByComparingTo(BigDecimal("57.50"))
    }

    @Test
    fun `getRate falls back to inverse with 1 over rate`() {
        `when`(
            repository.findTopByOrganizationIdAndFromCurrencyAndToCurrencyAndAsOfDateLessThanEqualOrderByAsOfDateDesc(
                orgId,
                "PHP",
                "USD",
                today,
            ),
        ).thenReturn(Optional.empty())
        val inverse = rateOf("USD", "PHP", BigDecimal("50.00"), today.minusDays(1))
        `when`(
            repository.findTopByOrganizationIdAndFromCurrencyAndToCurrencyAndAsOfDateLessThanEqualOrderByAsOfDateDesc(
                orgId,
                "USD",
                "PHP",
                today,
            ),
        ).thenReturn(Optional.of(inverse))

        val rate = service.getRate(orgId, "PHP", "USD", today)

        assertThat(rate).isEqualByComparingTo(BigDecimal("0.0200000000"))
    }

    @Test
    fun `getRate throws when no direct or inverse exists`() {
        `when`(
            repository.findTopByOrganizationIdAndFromCurrencyAndToCurrencyAndAsOfDateLessThanEqualOrderByAsOfDateDesc(
                any(),
                any(),
                any(),
                any(),
            ),
        ).thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> {
            service.getRate(orgId, "USD", "GBP", today)
        }
    }

    @Test
    fun `createManualRate rejects same-currency request`() {
        val request =
            CreateExchangeRateRequest(
                fromCurrency = "USD",
                toCurrency = "USD",
                rate = BigDecimal.ONE,
                asOfDate = today,
            )
        val ex =
            assertThrows<BusinessRuleException> {
                service.createManualRate(request, orgId)
            }
        assertThat(ex.message).contains("must differ")
    }

    @Test
    fun `createManualRate updates existing rate on same date`() {
        val existing = rateOf("USD", "PHP", BigDecimal("50.00"), today, source = ExchangeRateSource.AUTO)
        `when`(
            repository.findByOrganizationIdAndFromCurrencyAndToCurrencyAndAsOfDate(orgId, "USD", "PHP", today),
        ).thenReturn(Optional.of(existing))
        `when`(repository.save(any<ExchangeRate>())).thenAnswer { it.arguments[0] }

        val saved =
            service.createManualRate(
                CreateExchangeRateRequest("USD", "PHP", BigDecimal("57.50"), today),
                orgId,
            )

        assertThat(saved.rate).isEqualByComparingTo(BigDecimal("57.50"))
        assertThat(saved.source).isEqualTo(ExchangeRateSource.MANUAL)
    }

    @Test
    fun `deleteRate rejects cross-org`() {
        val rate = rateOf("USD", "PHP", BigDecimal("57.50"), today, orgId = "other-org")
        `when`(repository.findById(rate.id)).thenReturn(Optional.of(rate))

        assertThrows<ResourceNotFoundException> {
            service.deleteRate(rate.id, orgId)
        }
    }

    private fun rateOf(
        from: String,
        to: String,
        rate: BigDecimal,
        date: LocalDate,
        orgId: String = this.orgId,
        source: ExchangeRateSource = ExchangeRateSource.MANUAL,
    ) = ExchangeRate(
        organizationId = orgId,
        fromCurrency = from,
        toCurrency = to,
        rate = rate,
        asOfDate = date,
        source = source,
    )
}
