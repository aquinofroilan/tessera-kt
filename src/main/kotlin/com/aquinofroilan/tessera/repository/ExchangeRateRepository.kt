package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.ExchangeRate
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.Optional

@Repository
interface ExchangeRateRepository : JpaRepository<ExchangeRate, java.util.UUID> {
    fun findTopByOrganizationIdAndFromCurrencyAndToCurrencyAndAsOfDateLessThanEqualOrderByAsOfDateDesc(
        organizationId: java.util.UUID,
        fromCurrency: String,
        toCurrency: String,
        asOfDate: LocalDate,
    ): Optional<ExchangeRate>

    fun findByOrganizationIdAndFromCurrencyAndToCurrency(
        organizationId: java.util.UUID,
        fromCurrency: String,
        toCurrency: String,
    ): List<ExchangeRate>

    fun findByOrganizationIdAndFromCurrencyAndToCurrencyAndAsOfDate(
        organizationId: java.util.UUID,
        fromCurrency: String,
        toCurrency: String,
        asOfDate: LocalDate,
    ): Optional<ExchangeRate>

    fun findByOrganizationId(organizationId: java.util.UUID): List<ExchangeRate>
}
