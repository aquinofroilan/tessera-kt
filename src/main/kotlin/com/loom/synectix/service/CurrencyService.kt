package com.loom.synectix.service

import com.loom.synectix.exception.ResourceNotFoundException
import com.loom.synectix.model.Currency
import com.loom.synectix.repository.CurrencyRepository
import org.springframework.stereotype.Service

@Service
class CurrencyService(
    private val currencyRepository: CurrencyRepository,
) {
    fun listCurrencies(): List<Currency> = currencyRepository.findAll().sortedBy { it.code }

    fun getCurrency(code: String): Currency =
        currencyRepository.findById(code).orElseThrow {
            ResourceNotFoundException("Currency '$code' not found")
        }
}
