package com.aquinofroilan.tessera.domain.inventory.dto

import java.math.BigDecimal
import java.time.LocalDate

data class ExpiryReportResponse(
    val lines: List<ExpiryReportLineResponse>,
)

data class ExpiryReportLineResponse(
    val productId: java.util.UUID,
    val warehouseId: java.util.UUID,
    val lotNumber: String,
    val quantity: BigDecimal,
    val expiryDate: LocalDate,
)
