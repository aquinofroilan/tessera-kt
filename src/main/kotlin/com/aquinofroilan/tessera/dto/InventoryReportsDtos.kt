package com.aquinofroilan.tessera.dto

import com.aquinofroilan.tessera.model.StockMovementType
import java.math.BigDecimal

data class StockOnHandLineResponse(
    val productId: java.util.UUID,
    val warehouseId: java.util.UUID,
    val quantity: BigDecimal,
)

data class StockOnHandReportResponse(
    val asOfDate: String?,
    val lines: List<StockOnHandLineResponse>,
)

data class MovementHistoryLineResponse(
    val id: java.util.UUID,
    val type: StockMovementType,
    val productId: java.util.UUID,
    val warehouseId: java.util.UUID,
    val transferToWarehouseId: java.util.UUID?,
    val quantity: BigDecimal,
    val unitCost: BigDecimal?,
    val occurredAt: String,
    val runningBalance: BigDecimal,
)

data class MovementHistoryResponse(
    val productId: java.util.UUID?,
    val warehouseId: java.util.UUID?,
    val from: String?,
    val to: String?,
    val lines: List<MovementHistoryLineResponse>,
)
