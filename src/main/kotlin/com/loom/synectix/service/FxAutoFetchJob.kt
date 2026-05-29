package com.loom.synectix.service

import com.loom.synectix.model.ExchangeRate
import com.loom.synectix.model.ExchangeRateSource
import com.loom.synectix.repository.ExchangeRateRepository
import com.loom.synectix.repository.OrganizationRepository
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.dao.DuplicateKeyException
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDate

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
    private val meterRegistry: MeterRegistry,
) {
    private val log = LoggerFactory.getLogger(FxAutoFetchJob::class.java)
    private val jobTimer: Timer = meterRegistry.timer("synectix.fx.auto_fetch.job.duration")

    @Scheduled(cron = "\${synectix.fx.auto-fetch.cron}", zone = "UTC")
    fun fetchDailyRates() {
        meterRegistry.counter("synectix.fx.auto_fetch.job.runs").increment()
        val sample = Timer.start(meterRegistry)
        try {
            val orgs = organizationRepository.findAll().filter { it.isActive }
            if (orgs.isEmpty()) {
                meterRegistry.counter("synectix.fx.auto_fetch.job.skipped", "reason", "no_active_orgs").increment()
                return
            }

            val byBase = orgs.groupBy { it.baseCurrency }
            byBase.forEach { (base, orgsForBase) ->
                val result = frankfurterClient.fetchLatest(base, FrankfurterClient.SUPPORTED_CURRENCIES)
                if (result == null) {
                    meterRegistry.counter("synectix.fx.auto_fetch.base.fetches", "base", base, "outcome", "empty").increment()
                    return@forEach
                }
                meterRegistry.counter("synectix.fx.auto_fetch.base.fetches", "base", base, "outcome", "success").increment()
                orgsForBase.forEach { org ->
                    result.rates.forEach { (target, rate) ->
                        upsertAuto(org.uuid, base, target, rate, result.asOfDate)
                    }
                }
            }
        } catch (e: Exception) {
            meterRegistry.counter("synectix.fx.auto_fetch.job.failures").increment()
            throw e
        } finally {
            sample.stop(jobTimer)
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
            meterRegistry.counter("synectix.fx.auto_fetch.rate.upserts", "outcome", "skipped_invalid_rate").increment()
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
            meterRegistry.counter("synectix.fx.auto_fetch.rate.upserts", "outcome", "failed").increment()
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
            meterRegistry.counter("synectix.fx.auto_fetch.rate.upserts", "outcome", "skipped_manual").increment()
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
        meterRegistry.counter("synectix.fx.auto_fetch.rate.upserts", "outcome", "saved").increment()
    }
}
