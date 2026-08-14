package com.aquinofroilan.tessera.domain.inventory.dto

import com.aquinofroilan.tessera.domain.organization.model.InventoryCostingMethod
import java.math.BigDecimal

data class ValuationLineResponse(
    val productId: java.util.UUID,
    val warehouseId: java.util.UUID,
    val quantity: BigDecimal,
    val totalValue: BigDecimal,
)

data class ValuationReportResponse(
    val costingMethod: InventoryCostingMethod,
    val lines: List<ValuationLineResponse>,
    val totalValue: BigDecimal,
)
