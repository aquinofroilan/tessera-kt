package com.froilan.synectix.config

import com.froilan.synectix.model.Currency
import com.froilan.synectix.repository.CurrencyRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.core.annotation.Order
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
                currencyRepository.save(currency)
                log.info("Seeded currency: {} ({})", currency.code, currency.name)
            } else if (existing.get() != currency) {
                currencyRepository.save(currency)
                log.info("Updated currency: {}", currency.code)
            }
        }
    }
}
