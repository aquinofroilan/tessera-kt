package com.loom.synectix.service

import com.loom.synectix.config.FxAutoFetchProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import java.math.BigDecimal
import java.time.LocalDate

data class FrankfurterLatestResponse(
    val base: String? = null,
    val date: String? = null,
    val rates: Map<String, BigDecimal> = emptyMap(),
)

data class FxFetchResult(
    val asOfDate: LocalDate,
    val rates: Map<String, BigDecimal>,
)

@Service
class FrankfurterClient(
    private val restClient: RestClient,
    private val properties: FxAutoFetchProperties,
) {
    private val log = LoggerFactory.getLogger(FrankfurterClient::class.java)

    companion object {
        val SUPPORTED_CURRENCIES = setOf("USD", "EUR", "GBP", "JPY", "PHP")
    }

    fun fetchLatest(
        base: String,
        symbols: Collection<String>,
    ): FxFetchResult? {
        if (base !in SUPPORTED_CURRENCIES) {
            log.info("Skipping FX fetch for unsupported base currency {}", base)
            return null
        }
        val targets = symbols.filter { it != base && it in SUPPORTED_CURRENCIES }
        if (targets.isEmpty()) return null

        val uri = "${properties.baseUrl}/latest?base={base}&symbols={symbols}"
        return runCatching {
            val response =
                restClient
                    .get()
                    .uri(uri, base, targets.joinToString(","))
                    .retrieve()
                    .body(FrankfurterLatestResponse::class.java)
            val rates = response?.rates.orEmpty()
            val date = response?.date?.let(LocalDate::parse)
            if (rates.isEmpty() || date == null) null else FxFetchResult(date, rates)
        }.onFailure { e ->
            log.warn("Frankfurter fetch failed for base={}: {}", base, e.message)
        }.getOrNull()
    }
}
