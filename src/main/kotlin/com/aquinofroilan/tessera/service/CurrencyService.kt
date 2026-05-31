package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import com.aquinofroilan.tessera.model.Currency
import com.aquinofroilan.tessera.repository.CurrencyRepository
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
