package com.aquinofroilan.tessera.dto

import com.aquinofroilan.tessera.model.AssetStatus
import com.aquinofroilan.tessera.model.DepreciationMethod
import java.math.BigDecimal

data class AssetRegisterRow(
    val id: String,
    val assetNumber: String,
    val name: String,
    val categoryCode: String?,
    val categoryName: String?,
    val acquisitionDate: String,
    val acquisitionCost: BigDecimal,
    val salvageValue: BigDecimal,
    val accumulatedDepreciation: BigDecimal,
    val netBookValue: BigDecimal,
    val usefulLifeMonths: Int,
    val depreciationMethod: DepreciationMethod,
    val status: AssetStatus,
    val location: String?,
)

data class AssetRegisterResponse(
    val rows: List<AssetRegisterRow>,
    val totalAcquisitionCost: BigDecimal,
    val totalAccumulatedDepreciation: BigDecimal,
    val totalNetBookValue: BigDecimal,
)

data class DepreciationScheduleRow(
    val assetId: String,
    val assetNumber: String,
    val periodYear: Int,
    val periodMonth: Int,
    val depreciationAmount: BigDecimal,
    val cumulativeDepreciation: BigDecimal,
    val netBookValue: BigDecimal,
)

data class DepreciationScheduleResponse(
    val rows: List<DepreciationScheduleRow>,
    val months: Int,
    val assetCount: Int,
)
