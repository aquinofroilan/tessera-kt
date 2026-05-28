package com.loom.synectix.service

import com.loom.synectix.dto.CreateExchangeRateRequest
import com.loom.synectix.exception.BusinessRuleException
import com.loom.synectix.exception.ResourceNotFoundException
import com.loom.synectix.model.ExchangeRate
import com.loom.synectix.model.ExchangeRateSource
import com.loom.synectix.repository.ExchangeRateRepository
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

@Service
class ExchangeRateService(
    private val exchangeRateRepository: ExchangeRateRepository,
    private val currencyService: CurrencyService,
) {
    fun getRate(
        organizationId: String,
        fromCurrency: String,
        toCurrency: String,
        onOrBefore: LocalDate,
    ): BigDecimal = lookupRate(organizationId, fromCurrency, toCurrency, onOrBefore).rate

    data class RateLookup(
        val rate: BigDecimal,
        val effectiveDate: LocalDate?,
        val source: ExchangeRateSource?,
        val direction: String,
    )

    fun lookupRate(
        organizationId: String,
        fromCurrency: String,
        toCurrency: String,
        onOrBefore: LocalDate,
    ): RateLookup {
        if (fromCurrency == toCurrency) {
            return RateLookup(BigDecimal.ONE, null, null, "SAME_CURRENCY")
        }

        val direct =
            exchangeRateRepository
                .findTopByOrganizationIdAndFromCurrencyAndToCurrencyAndAsOfDateLessThanEqualOrderByAsOfDateDesc(
                    organizationId,
                    fromCurrency,
                    toCurrency,
                    onOrBefore,
                )
        if (direct.isPresent) {
            val r = direct.get()
            return RateLookup(r.rate, r.asOfDate, r.source, "DIRECT")
        }

        val inverse =
            exchangeRateRepository
                .findTopByOrganizationIdAndFromCurrencyAndToCurrencyAndAsOfDateLessThanEqualOrderByAsOfDateDesc(
                    organizationId,
                    toCurrency,
                    fromCurrency,
                    onOrBefore,
                )
        if (inverse.isPresent) {
            val r = inverse.get()
            if (r.rate.signum() <= 0) {
                throw BusinessRuleException(
                    "Stored exchange rate ${r.fromCurrency}->${r.toCurrency} on ${r.asOfDate} is non-positive",
                )
            }
            return RateLookup(
                BigDecimal.ONE.divide(r.rate, 10, RoundingMode.HALF_UP),
                r.asOfDate,
                r.source,
                "INVERSE",
            )
        }

        throw ResourceNotFoundException(
            "No exchange rate found for $fromCurrency -> $toCurrency on or before $onOrBefore",
        )
    }

    @Transactional
    fun createManualRate(
        request: CreateExchangeRateRequest,
        organizationId: String,
    ): ExchangeRate {
        if (request.fromCurrency == request.toCurrency) {
            throw BusinessRuleException("fromCurrency and toCurrency must differ")
        }
        if (request.rate.signum() <= 0) {
            throw BusinessRuleException("Exchange rate must be positive")
        }
        currencyService.getCurrency(request.fromCurrency)
        currencyService.getCurrency(request.toCurrency)

        val existing =
            exchangeRateRepository.findByOrganizationIdAndFromCurrencyAndToCurrencyAndAsOfDate(
                organizationId,
                request.fromCurrency,
                request.toCurrency,
                request.asOfDate,
            )
        val toSave =
            if (existing.isPresent) {
                existing.get().copy(
                    rate = request.rate,
                    source = ExchangeRateSource.MANUAL,
                )
            } else {
                ExchangeRate(
                    organizationId = organizationId,
                    fromCurrency = request.fromCurrency,
                    toCurrency = request.toCurrency,
                    rate = request.rate,
                    asOfDate = request.asOfDate,
                    source = ExchangeRateSource.MANUAL,
                )
            }
        return try {
            exchangeRateRepository.save(toSave)
        } catch (e: DuplicateKeyException) {
            throw BusinessRuleException(
                "Exchange rate already exists for ${request.fromCurrency}->${request.toCurrency} on ${request.asOfDate}",
                e,
            )
        }
    }

    fun listRates(
        organizationId: String,
        fromCurrency: String?,
        toCurrency: String?,
    ): List<ExchangeRate> {
        val all =
            if (fromCurrency != null && toCurrency != null) {
                exchangeRateRepository.findByOrganizationIdAndFromCurrencyAndToCurrency(
                    organizationId,
                    fromCurrency,
                    toCurrency,
                )
            } else {
                exchangeRateRepository.findByOrganizationId(organizationId)
            }
        return all.sortedWith(
            compareBy({ it.fromCurrency }, { it.toCurrency }, { it.asOfDate }),
        )
    }

    @Transactional
    fun deleteRate(
        rateId: String,
        organizationId: String,
    ): ExchangeRate {
        val rate =
            exchangeRateRepository.findById(rateId).orElseThrow {
                ResourceNotFoundException("Exchange rate not found")
            }
        if (rate.organizationId != organizationId) {
            throw ResourceNotFoundException("Exchange rate not found")
        }
        exchangeRateRepository.deleteById(rateId)
        return rate
    }
}
