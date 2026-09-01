package com.aquinofroilan.tessera.domain.finance.service

import com.aquinofroilan.tessera.domain.finance.model.ExchangeRate
import com.aquinofroilan.tessera.domain.finance.model.ExchangeRateSource
import com.aquinofroilan.tessera.domain.finance.repository.ExchangeRateRepository
import com.aquinofroilan.tessera.domain.organization.repository.OrganizationRepository
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import jakarta.annotation.PostConstruct
import org.jobrunr.jobs.annotations.Job
import org.jobrunr.scheduling.JobScheduler
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
@ConditionalOnProperty(
    name = ["tessera.fx.auto-fetch.enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
class FxAutoFetchJob(
    private val organizationRepository: OrganizationRepository,
    private val exchangeRateRepository: ExchangeRateRepository,
    private val frankfurterClient: FrankfurterClient,
    private val meterRegistry: MeterRegistry,
    private val jobScheduler: JobScheduler,
    @Value("\${tessera.fx.auto-fetch.cron:0 0 17 * * MON-FRI}") private val cronExpression: String,
) {
    private val log = LoggerFactory.getLogger(FxAutoFetchJob::class.java)
    private val jobTimer: Timer = meterRegistry.timer("tessera.fx.auto_fetch.job.duration")

    @PostConstruct
    fun scheduleJob() {
        jobScheduler.scheduleRecurrently("fx-auto-fetch", cronExpression) {
            fetchDailyRates()
        }
    }

    @Job(name = "FX Auto Fetch Daily Rates")
    fun fetchDailyRates() {
        meterRegistry.counter("tessera.fx.auto_fetch.job.runs").increment()
        val sample = Timer.start(meterRegistry)
        try {
            val orgs = organizationRepository.findAll().filter { it.isActive }
            if (orgs.isEmpty()) {
                meterRegistry.counter("tessera.fx.auto_fetch.job.skipped", "reason", "no_active_orgs").increment()
                return
            }

            val byBase = orgs.groupBy { it.baseCurrency }
            byBase.forEach { (base, orgsForBase) ->
                val result = frankfurterClient.fetchLatest(base, FrankfurterClient.SUPPORTED_CURRENCIES)
                if (result == null) {
                    meterRegistry.counter("tessera.fx.auto_fetch.base.fetches", "base", base, "outcome", "empty").increment()
                    return@forEach
                }
                meterRegistry.counter("tessera.fx.auto_fetch.base.fetches", "base", base, "outcome", "success").increment()
                orgsForBase.forEach { org ->
                    result.rates.forEach { (target, rate) ->
                        upsertAuto(org.uuid, base, target, rate, result.asOfDate)
                    }
                }
            }
        } catch (e: Exception) {
            meterRegistry.counter("tessera.fx.auto_fetch.job.failures").increment()
            throw e
        } finally {
            sample.stop(jobTimer)
        }
    }

    private fun upsertAuto(
        organizationId: java.util.UUID,
        fromCurrency: String,
        toCurrency: String,
        rate: java.math.BigDecimal,
        asOfDate: LocalDate,
    ) {
        if (rate.signum() <= 0) {
            meterRegistry.counter("tessera.fx.auto_fetch.rate.upserts", "outcome", "skipped_invalid_rate").increment()
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
            meterRegistry.counter("tessera.fx.auto_fetch.rate.upserts", "outcome", "failed").increment()
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
        organizationId: java.util.UUID,
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
            meterRegistry.counter("tessera.fx.auto_fetch.rate.upserts", "outcome", "skipped_manual").increment()
            return
        }
        val toSave =
            existing
                .map {
                    it.apply {
                        this.rate = rate
                        source = ExchangeRateSource.AUTO
                    }
                }.orElse(
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
        meterRegistry.counter("tessera.fx.auto_fetch.rate.upserts", "outcome", "saved").increment()
    }
}
