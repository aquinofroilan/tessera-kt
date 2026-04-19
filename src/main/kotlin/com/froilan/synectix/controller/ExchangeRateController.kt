package com.froilan.synectix.controller

import com.froilan.synectix.annotation.LogLevel
import com.froilan.synectix.annotation.Loggable
import com.froilan.synectix.dto.CreateExchangeRateRequest
import com.froilan.synectix.dto.ExchangeRateResponse
import com.froilan.synectix.model.ExchangeRate
import com.froilan.synectix.security.AuthenticationContext
import com.froilan.synectix.service.ExchangeRateService
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/finance/exchange-rates")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class ExchangeRateController(
    private val exchangeRateService: ExchangeRateService,
    private val authContext: AuthenticationContext,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('fx:create')")
    fun createRate(
        @Valid @RequestBody request: CreateExchangeRateRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val rate = exchangeRateService.createManualRate(request, orgId)
        return ResponseEntity.status(HttpStatus.CREATED).body(rate.toResponse())
    }

    @GetMapping
    @PreAuthorize("hasAuthority('fx:read')")
    fun listRates(
        @RequestParam(required = false) from: String?,
        @RequestParam(required = false) to: String?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) asOfDate: LocalDate?,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        if (asOfDate != null) {
            if (from == null || to == null) {
                return ResponseEntity.badRequest().body(mapOf("error" to "from and to are required when asOfDate is set"))
            }
            val rate = exchangeRateService.getRate(orgId, from, to, asOfDate)
            return ResponseEntity.ok(
                mapOf(
                    "from" to from,
                    "to" to to,
                    "asOfDate" to asOfDate.toString(),
                    "rate" to rate,
                ),
            )
        }
        return ResponseEntity.ok(exchangeRateService.listRates(orgId, from, to).map { it.toResponse() })
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('fx:create')")
    fun deleteRate(
        @PathVariable id: String,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        exchangeRateService.deleteRate(id, orgId)
        return ResponseEntity.noContent().build()
    }

    private fun ExchangeRate.toResponse() =
        ExchangeRateResponse(
            id = id,
            organizationId = organizationId,
            fromCurrency = fromCurrency,
            toCurrency = toCurrency,
            rate = rate,
            asOfDate = asOfDate.toString(),
            source = source.name,
            createdAt = createdAt?.toString(),
            updatedAt = updatedAt?.toString(),
        )
}
