package com.aquinofroilan.tessera.domain.finance.dto

data class CurrencyResponse(
    val code: String,
    val name: String,
    val symbol: String,
    val decimalPlaces: Int,
)
