package com.loom.synectix.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import java.math.BigDecimal
import java.time.LocalDate

data class CreateExchangeRateRequest(
    @field:NotBlank(message = "fromCurrency is required")
    val fromCurrency: String,
    @field:NotBlank(message = "toCurrency is required")
    val toCurrency: String,
    @field:Positive(message = "rate must be positive")
    val rate: BigDecimal,
    val asOfDate: LocalDate,
)

data class ExchangeRateResponse(
    val id: String,
    val organizationId: String,
    val fromCurrency: String,
    val toCurrency: String,
    val rate: BigDecimal,
    val asOfDate: String,
    val source: String,
    val createdAt: String?,
    val updatedAt: String?,
)
