package com.froilan.synectix.dto

import com.froilan.synectix.model.StockMovementType
import java.math.BigDecimal

data class StockOnHandLineResponse(
    val productId: String,
    val warehouseId: String,
    val quantity: BigDecimal,
)

data class StockOnHandReportResponse(
    val asOfDate: String?,
    val lines: List<StockOnHandLineResponse>,
)

data class MovementHistoryLineResponse(
    val id: String,
    val type: StockMovementType,
    val productId: String,
    val warehouseId: String,
    val transferToWarehouseId: String?,
    val quantity: BigDecimal,
    val unitCost: BigDecimal?,
    val occurredAt: String,
    val runningBalance: BigDecimal,
)

data class MovementHistoryResponse(
    val productId: String?,
    val warehouseId: String?,
    val from: String?,
    val to: String?,
    val lines: List<MovementHistoryLineResponse>,
)
