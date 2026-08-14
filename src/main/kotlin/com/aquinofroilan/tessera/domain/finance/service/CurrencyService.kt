package com.aquinofroilan.tessera.domain.finance.service

import com.aquinofroilan.tessera.domain.finance.model.Currency
import com.aquinofroilan.tessera.domain.finance.repository.CurrencyRepository
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
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
