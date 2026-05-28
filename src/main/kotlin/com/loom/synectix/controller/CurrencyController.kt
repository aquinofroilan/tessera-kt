package com.loom.synectix.controller

import com.loom.synectix.annotation.LogLevel
import com.loom.synectix.annotation.Loggable
import com.loom.synectix.dto.CurrencyResponse
import com.loom.synectix.model.Currency
import com.loom.synectix.security.AuthenticationContext
import com.loom.synectix.service.CurrencyService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/finance/currencies")
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
