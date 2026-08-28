package com.aquinofroilan.tessera.domain.finance.service

import com.aquinofroilan.tessera.domain.finance.model.Currency
import com.aquinofroilan.tessera.domain.finance.model.ExchangeRate
import com.aquinofroilan.tessera.domain.finance.model.ExchangeRateSource
import com.aquinofroilan.tessera.domain.finance.repository.ExchangeRateRepository
import com.aquinofroilan.tessera.domain.platform.dto.CreateExchangeRateRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
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

    private val orgId = java.util.UUID.fromString("e5628ca4-87a8-3e6f-8ae2-20213cc7ef92")
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
        val rate =
            rateOf(
                "fe2db454-a7f7-3170-995e-74c0045ab41b",
                "e6eb0ebe-1a8c-39eb-851a-4495c5096549",
                BigDecimal("57.50"),
                today,
                orgId = java.util.UUID.fromString("fbede99a-0bef-3bf9-ba0b-8d28f050479d"),
            )
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
        orgId: java.util.UUID = this.orgId,
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
