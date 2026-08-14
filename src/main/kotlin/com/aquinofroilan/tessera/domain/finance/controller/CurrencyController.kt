package com.aquinofroilan.tessera.domain.finance.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.domain.finance.dto.CurrencyResponse
import com.aquinofroilan.tessera.domain.finance.model.Currency
import com.aquinofroilan.tessera.domain.finance.service.CurrencyService
import com.aquinofroilan.tessera.security.AuthenticationContext
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/finance/currencies")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class CurrencyController(
    private val currencyService: CurrencyService,
    private val authContext: AuthenticationContext,
) {
    @GetMapping
    @PreAuthorize("hasAuthority('fx:read')")
    fun listCurrencies(): ResponseEntity<Any> {
        authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(currencyService.listCurrencies().map { it.toResponse() })
    }

    private fun Currency.toResponse() =
        CurrencyResponse(
            code = code,
            name = name,
            symbol = symbol,
            decimalPlaces = decimalPlaces,
        )
}
