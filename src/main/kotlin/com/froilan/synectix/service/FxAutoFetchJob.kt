package com.froilan.synectix.service

import com.froilan.synectix.model.ExchangeRate
import com.froilan.synectix.model.ExchangeRateSource
import com.froilan.synectix.repository.ExchangeRateRepository
import com.froilan.synectix.repository.OrganizationRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.dao.DuplicateKeyException
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.ZoneOffset

@Service
@ConditionalOnProperty(
    name = ["synectix.fx.auto-fetch.enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
class FxAutoFetchJob(
    private val organizationRepository: OrganizationRepository,
    private val exchangeRateRepository: ExchangeRateRepository,
    private val frankfurterClient: FrankfurterClient,
) {
    private val log = LoggerFactory.getLogger(FxAutoFetchJob::class.java)

    @Scheduled(cron = "\${synectix.fx.auto-fetch.cron}")
    fun fetchDailyRates() {
        val today = LocalDate.now(ZoneOffset.UTC)
        val orgs = organizationRepository.findAll().filter { it.isActive }
        if (orgs.isEmpty()) return

        val byBase = orgs.groupBy { it.baseCurrency }
        byBase.forEach { (base, orgsForBase) ->
            val rates = frankfurterClient.fetchLatest(base, FrankfurterClient.SUPPORTED_CURRENCIES)
            if (rates.isEmpty()) return@forEach
            orgsForBase.forEach { org ->
                rates.forEach { (target, rate) ->
                    upsertAuto(org.uuid, base, target, rate, today)
                }
            }
        }
    }

    private fun upsertAuto(
        organizationId: String,
        fromCurrency: String,
        toCurrency: String,
        rate: java.math.BigDecimal,
        asOfDate: LocalDate,
    ) {
        if (rate.signum() <= 0) {
            log.warn(
                "Skipping non-positive FX rate for org={} {}->{} on {}: {}",
                organizationId,
                fromCurrency,
                toCurrency,
                asOfDate,
                rate,
            )
            return
        }
        runCatching {
            try {
                saveAuto(organizationId, fromCurrency, toCurrency, rate, asOfDate)
            } catch (_: DuplicateKeyException) {
                saveAuto(organizationId, fromCurrency, toCurrency, rate, asOfDate)
            }
        }.onFailure { e ->
            log.warn(
                "FX upsert failed for org={} {}->{} on {}: {}",
                organizationId,
                fromCurrency,
                toCurrency,
                asOfDate,
                e.message,
            )
        }
    }

    private fun saveAuto(
        organizationId: String,
        fromCurrency: String,
        toCurrency: String,
        rate: java.math.BigDecimal,
        asOfDate: LocalDate,
    ) {
        val existing =
            exchangeRateRepository.findByOrganizationIdAndFromCurrencyAndToCurrencyAndAsOfDate(
                organizationId,
                fromCurrency,
                toCurrency,
                asOfDate,
            )
        if (existing.isPresent && existing.get().source == ExchangeRateSource.MANUAL) {
            return
        }
        val toSave =
            existing
                .map { it.copy(rate = rate, source = ExchangeRateSource.AUTO) }
                .orElse(
                    ExchangeRate(
                        organizationId = organizationId,
                        fromCurrency = fromCurrency,
                        toCurrency = toCurrency,
                        rate = rate,
                        asOfDate = asOfDate,
                        source = ExchangeRateSource.AUTO,
                    ),
                )
        exchangeRateRepository.save(toSave)
    }
}
