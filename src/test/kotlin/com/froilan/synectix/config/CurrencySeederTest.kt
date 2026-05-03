package com.froilan.synectix.config

import com.froilan.synectix.model.Currency
import com.froilan.synectix.repository.CurrencyRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.springframework.boot.ApplicationArguments
import java.util.Optional

class CurrencySeederTest {
    private lateinit var seeder: CurrencySeeder
    private lateinit var currencyRepository: CurrencyRepository

    @BeforeEach
    fun setup() {
        currencyRepository = mock(CurrencyRepository::class.java)
        seeder = CurrencySeeder(currencyRepository)
    }

    @Test
    fun `seeds five currencies with JPY at zero decimals`() {
        `when`(currencyRepository.findById(any())).thenReturn(Optional.empty())

        seeder.run(mock(ApplicationArguments::class.java))

        val captor = argumentCaptor<Currency>()
        verify(currencyRepository, org.mockito.Mockito.times(5)).save(captor.capture())

        val saved = captor.allValues
        assertThat(saved)
            .extracting<String> { it.code }
            .containsExactlyInAnyOrder("USD", "EUR", "GBP", "JPY", "PHP")

        val jpy = saved.first { it.code == "JPY" }
        assertThat(jpy.decimalPlaces).isZero
        assertThat(jpy.symbol).isEqualTo("¥")
    }

    @Test
    fun `does not save when currency exists with same fields`() {
        val usd = Currency(code = "USD", name = "US Dollar", symbol = "\$", decimalPlaces = 2)
        `when`(currencyRepository.findById("USD")).thenReturn(Optional.of(usd))
        `when`(currencyRepository.findById("EUR")).thenReturn(Optional.empty())
        `when`(currencyRepository.findById("GBP")).thenReturn(Optional.empty())
        `when`(currencyRepository.findById("JPY")).thenReturn(Optional.empty())
        `when`(currencyRepository.findById("PHP")).thenReturn(Optional.empty())

        seeder.run(mock(ApplicationArguments::class.java))

        val captor = argumentCaptor<Currency>()
        verify(currencyRepository, org.mockito.Mockito.times(4)).save(captor.capture())
        assertThat(captor.allValues).noneMatch { it.code == "USD" }
    }

    @Test
    fun `updates currency when stored fields drift`() {
        val driftedUsd = Currency(code = "USD", name = "OLD NAME", symbol = "X", decimalPlaces = 2)
        `when`(currencyRepository.findById("USD")).thenReturn(Optional.of(driftedUsd))
        `when`(currencyRepository.findById("EUR")).thenReturn(Optional.empty())
        `when`(currencyRepository.findById("GBP")).thenReturn(Optional.empty())
        `when`(currencyRepository.findById("JPY")).thenReturn(Optional.empty())
        `when`(currencyRepository.findById("PHP")).thenReturn(Optional.empty())

        seeder.run(mock(ApplicationArguments::class.java))

        val captor = argumentCaptor<Currency>()
        verify(currencyRepository, org.mockito.Mockito.atLeast(1)).save(captor.capture())
        val savedUsd = captor.allValues.first { it.code == "USD" }
        assertThat(savedUsd.name).isEqualTo("US Dollar")
        assertThat(savedUsd.symbol).isEqualTo("\$")
    }

    @Test
    fun `noop when nothing changes`() {
        val seeded =
            mapOf(
                "USD" to Currency("USD", "US Dollar", "\$", 2),
                "EUR" to Currency("EUR", "Euro", "€", 2),
                "GBP" to Currency("GBP", "Pound Sterling", "£", 2),
                "JPY" to Currency("JPY", "Japanese Yen", "¥", 0),
                "PHP" to Currency("PHP", "Philippine Peso", "₱", 2),
            )
        seeded.forEach { (code, currency) ->
            `when`(currencyRepository.findById(code)).thenReturn(Optional.of(currency))
        }

        seeder.run(mock(ApplicationArguments::class.java))

        verify(currencyRepository, never()).save(any())
    }
}
