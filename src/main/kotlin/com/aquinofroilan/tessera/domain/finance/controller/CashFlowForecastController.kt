package com.aquinofroilan.tessera.domain.finance.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.domain.finance.service.CashFlowForecastService
import com.aquinofroilan.tessera.security.AuthenticationContext
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/finance/cash-flow")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class CashFlowForecastController(
    private val forecastService: CashFlowForecastService,
    private val authContext: AuthenticationContext,
) {
    @GetMapping("/forecast")
    @PreAuthorize("hasAuthority('bank:read')")
    fun forecast(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) asOf: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) horizonEnd: LocalDate?,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(forecastService.forecast(orgId, asOf, horizonEnd))
    }
}
