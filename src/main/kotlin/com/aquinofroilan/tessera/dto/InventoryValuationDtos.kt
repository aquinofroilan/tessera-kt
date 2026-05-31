package com.aquinofroilan.tessera.dto

import com.aquinofroilan.tessera.model.InventoryCostingMethod
import java.math.BigDecimal

data class ValuationLineResponse(
    val productId: String,
    val warehouseId: String,
    val quantity: BigDecimal,
    val totalValue: BigDecimal,
)

data class ValuationReportResponse(
    val costingMethod: InventoryCostingMethod,
    val lines: List<ValuationLineResponse>,
    val totalValue: BigDecimal,
)
