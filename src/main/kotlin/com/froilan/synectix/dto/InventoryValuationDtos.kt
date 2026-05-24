package com.froilan.synectix.dto

import com.froilan.synectix.model.InventoryCostingMethod
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
