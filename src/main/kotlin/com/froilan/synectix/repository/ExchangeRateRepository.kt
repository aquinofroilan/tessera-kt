package com.froilan.synectix.repository

import com.froilan.synectix.model.ExchangeRate
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.util.Optional

@Repository
interface ExchangeRateRepository : MongoRepository<ExchangeRate, String> {
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
