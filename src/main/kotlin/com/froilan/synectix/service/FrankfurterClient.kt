package com.froilan.synectix.service

import com.froilan.synectix.config.FxAutoFetchProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import java.math.BigDecimal

data class FrankfurterLatestResponse(
    val base: String? = null,
    val date: String? = null,
    val rates: Map<String, BigDecimal> = emptyMap(),
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
    ): Map<String, BigDecimal> {
        if (base !in SUPPORTED_CURRENCIES) {
            log.info("Skipping FX fetch for unsupported base currency {}", base)
            return emptyMap()
        }
        val targets = symbols.filter { it != base && it in SUPPORTED_CURRENCIES }
        if (targets.isEmpty()) return emptyMap()

        val uri = "${properties.baseUrl}/latest?base={base}&symbols={symbols}"
        return runCatching {
            restClient
                .get()
                .uri(uri, base, targets.joinToString(","))
                .retrieve()
                .body(FrankfurterLatestResponse::class.java)
                ?.rates
                ?: emptyMap()
        }.onFailure { e ->
            log.warn("Frankfurter fetch failed for base={}: {}", base, e.message)
        }.getOrDefault(emptyMap())
    }
}
