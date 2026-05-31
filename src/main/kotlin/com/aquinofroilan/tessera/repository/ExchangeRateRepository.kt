package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.ExchangeRate
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.Optional

@Repository
interface ExchangeRateRepository : JpaRepository<ExchangeRate, String> {
    fun findTopByOrganizationIdAndFromCurrencyAndToCurrencyAndAsOfDateLessThanEqualOrderByAsOfDateDesc(
        organizationId: String,
        fromCurrency: String,
        toCurrency: String,
        asOfDate: LocalDate,
    ): Optional<ExchangeRate>

    fun findByOrganizationIdAndFromCurrencyAndToCurrency(
        organizationId: String,
        fromCurrency: String,
        toCurrency: String,
    ): List<ExchangeRate>

    fun findByOrganizationIdAndFromCurrencyAndToCurrencyAndAsOfDate(
        organizationId: String,
        fromCurrency: String,
        toCurrency: String,
        asOfDate: LocalDate,
    ): Optional<ExchangeRate>

    fun findByOrganizationId(organizationId: String): List<ExchangeRate>
}
