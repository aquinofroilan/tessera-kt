package com.froilan.synectix.service

import com.froilan.synectix.exception.ResourceNotFoundException
import com.froilan.synectix.model.Currency
import com.froilan.synectix.repository.CurrencyRepository
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
