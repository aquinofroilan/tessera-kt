package com.aquinofroilan.tessera.dto

import com.aquinofroilan.tessera.model.AssetDepreciationRun
import com.aquinofroilan.tessera.model.AssetDepreciationRunLine
import com.aquinofroilan.tessera.model.DepreciationRunStatus
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal

data class CreateDepreciationRunRequest(
    @field:NotNull(message = "Period year is required")
    @field:Min(value = 1900, message = "Invalid year")
    val periodYear: Int?,
    @field:NotNull(message = "Period month is required")
    @field:Min(1)
    @field:Max(12)
    val periodMonth: Int?,
)

data class DepreciationRunLineResponse(
    val id: String,
    val assetId: String,
    val depreciationAmount: BigDecimal,
    val debitAccountId: String?,
    val creditAccountId: String?,
) {
    companion object {
        fun from(line: AssetDepreciationRunLine) =
            DepreciationRunLineResponse(
                id = line.id.toString(),
                assetId = line.assetId.toString(),
                depreciationAmount = line.depreciationAmount,
                debitAccountId = line.debitAccountId?.toString(),
                creditAccountId = line.creditAccountId?.toString(),
            )
    }
}

data class DepreciationRunResponse(
    val id: String,
    val periodYear: Int,
    val periodMonth: Int,
    val status: DepreciationRunStatus,
    val totalDepreciation: BigDecimal,
    val journalEntryId: String?,
    val postedAt: String?,
    val postedBy: String?,
    val organizationId: String,
    val createdAt: String?,
    val updatedAt: String?,
    val lines: List<DepreciationRunLineResponse>? = null,
) {
    companion object {
        fun from(
            run: AssetDepreciationRun,
            lines: List<AssetDepreciationRunLine>? = null,
        ) = DepreciationRunResponse(
            id = run.id.toString(),
            periodYear = run.periodYear,
            periodMonth = run.periodMonth,
            status = run.status,
            totalDepreciation = run.totalDepreciation,
            journalEntryId = run.journalEntryId?.toString(),
            postedAt = run.postedAt?.toString(),
            postedBy = run.postedBy?.toString(),
            organizationId = run.organizationId.toString(),
            createdAt = run.createdAt?.toString(),
            updatedAt = (run.updatedAt ?: run.createdAt)?.toString(),
            lines = lines?.map { DepreciationRunLineResponse.from(it) },
        )
    }
}
