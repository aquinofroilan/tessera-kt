package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.model.ExchangeRate
import com.aquinofroilan.tessera.model.ExchangeRateSource
import com.aquinofroilan.tessera.model.Organizations
import com.aquinofroilan.tessera.repository.ExchangeRateRepository
import com.aquinofroilan.tessera.repository.OrganizationRepository
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Optional

class FxAutoFetchJobTest {
    private lateinit var job: FxAutoFetchJob
    private lateinit var organizationRepository: OrganizationRepository
    private lateinit var exchangeRateRepository: ExchangeRateRepository
    private lateinit var frankfurterClient: FrankfurterClient

    private val orgId = "org-1"

    @BeforeEach
    fun setup() {
        organizationRepository = mock(OrganizationRepository::class.java)
        exchangeRateRepository = mock(ExchangeRateRepository::class.java)
        frankfurterClient = mock(FrankfurterClient::class.java)
        job = FxAutoFetchJob(organizationRepository, exchangeRateRepository, frankfurterClient, SimpleMeterRegistry())
    }

    @Test
    fun `inserts AUTO rates for each target currency`() {
        `when`(organizationRepository.findAll()).thenReturn(listOf(orgWithBase("USD")))
        `when`(frankfurterClient.fetchLatest("USD", FrankfurterClient.SUPPORTED_CURRENCIES))
            .thenReturn(
                FxFetchResult(
                    asOfDate = LocalDate.of(2026, 5, 1),
                    rates =
                        mapOf(
                            "EUR" to BigDecimal("0.92"),
                            "PHP" to BigDecimal("57.50"),
                        ),
                ),
            )
        `when`(
            exchangeRateRepository.findByOrganizationIdAndFromCurrencyAndToCurrencyAndAsOfDate(
                any(),
                any(),
                any(),
                any(),
            ),
        ).thenReturn(Optional.empty())
        `when`(exchangeRateRepository.save(any<ExchangeRate>())).thenAnswer { it.arguments[0] }

        job.fetchDailyRates()

        val captor = argumentCaptor<ExchangeRate>()
        verify(exchangeRateRepository, org.mockito.Mockito.times(2)).save(captor.capture())
        assertThat(captor.allValues).allMatch { it.source == ExchangeRateSource.AUTO }
        assertThat(captor.allValues)
            .extracting<String> { it.toCurrency }
            .containsExactlyInAnyOrder("EUR", "PHP")
    }

    @Test
    fun `skips updating MANUAL rates`() {
        val today = LocalDate.now(ZoneOffset.UTC)
        val manualRate =
            ExchangeRate(
                organizationId = orgId,
                fromCurrency = "USD",
                toCurrency = "PHP",
                rate = BigDecimal("57.00"),
                asOfDate = today,
                source = ExchangeRateSource.MANUAL,
            )

        `when`(organizationRepository.findAll()).thenReturn(listOf(orgWithBase("USD")))
        `when`(frankfurterClient.fetchLatest("USD", FrankfurterClient.SUPPORTED_CURRENCIES))
            .thenReturn(FxFetchResult(asOfDate = today, rates = mapOf("PHP" to BigDecimal("57.50"))))
        `when`(
            exchangeRateRepository.findByOrganizationIdAndFromCurrencyAndToCurrencyAndAsOfDate(
                orgId,
                "USD",
                "PHP",
                today,
            ),
        ).thenReturn(Optional.of(manualRate))

        job.fetchDailyRates()

        verify(exchangeRateRepository, never()).save(any<ExchangeRate>())
    }

    @Test
    fun `does nothing when no orgs exist`() {
        `when`(organizationRepository.findAll()).thenReturn(emptyList())

        job.fetchDailyRates()

        verify(frankfurterClient, never()).fetchLatest(any(), any())
        verify(exchangeRateRepository, never()).save(any<ExchangeRate>())
    }

    @Test
    fun `skips inactive orgs`() {
        `when`(organizationRepository.findAll()).thenReturn(listOf(orgWithBase("USD", isActive = false)))

        job.fetchDailyRates()

        verify(frankfurterClient, never()).fetchLatest(any(), any())
    }

    private fun orgWithBase(
        base: String,
        isActive: Boolean = true,
    ) = Organizations(
        uuid = orgId,
        orgSlug = "slug",
        name = "Test",
        legalName = "Test",
        tradeName = "Test",
        baseCurrency = base,
        fiscalYearStart = LocalDateTime.of(2026, 1, 1, 0, 0),
        timezone = "UTC",
        isActive = isActive,
    )
}
