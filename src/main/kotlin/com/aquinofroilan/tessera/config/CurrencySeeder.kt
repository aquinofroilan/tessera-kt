package com.aquinofroilan.tessera.config

import com.aquinofroilan.tessera.model.Currency
import com.aquinofroilan.tessera.repository.CurrencyRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.annotation.Order
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Component

@Component
@Order(1)
class CurrencySeeder(
    private val currencyRepository: CurrencyRepository,
) : ApplicationRunner {
    private val log = LoggerFactory.getLogger(CurrencySeeder::class.java)

    override fun run(args: ApplicationArguments) {
        val seeded =
            listOf(
                Currency(code = "USD", name = "US Dollar", symbol = "\$", decimalPlaces = 2),
                Currency(code = "EUR", name = "Euro", symbol = "€", decimalPlaces = 2),
                Currency(code = "GBP", name = "Pound Sterling", symbol = "£", decimalPlaces = 2),
                Currency(code = "JPY", name = "Japanese Yen", symbol = "¥", decimalPlaces = 0),
                Currency(code = "PHP", name = "Philippine Peso", symbol = "₱", decimalPlaces = 2),
            )

        seeded.forEach { currency ->
            val existing = currencyRepository.findById(currency.code)
            if (existing.isEmpty) {
                try {
                    currencyRepository.save(currency)
                    log.info("Seeded currency: {} ({})", currency.code, currency.name)
                } catch (e: DataIntegrityViolationException) {
                    // Only treat as a race-loss if the row is now present;
                    // otherwise this is a real constraint problem worth surfacing.
                    if (currencyRepository.findById(currency.code).isEmpty) {
                        throw e
                    }
                }
            } else if (!sameCurrency(existing.get(), currency)) {
                currencyRepository.save(currency)
                log.info("Updated currency: {}", currency.code)
            }
        }
    }

    private fun sameCurrency(
        left: Currency,
        right: Currency,
    ): Boolean =
        left.code == right.code &&
            left.name == right.name &&
            left.symbol == right.symbol &&
            left.decimalPlaces == right.decimalPlaces
}
