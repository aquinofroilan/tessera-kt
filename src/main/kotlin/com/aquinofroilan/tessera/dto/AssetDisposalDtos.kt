package com.aquinofroilan.tessera.dto

import com.aquinofroilan.tessera.model.AssetDisposal
import com.aquinofroilan.tessera.model.DisposalStatus
import com.aquinofroilan.tessera.model.DisposalType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDate

data class CreateAssetDisposalRequest(
    @field:NotBlank(message = "Asset id is required")
    val assetId: String,
    @field:NotNull(message = "Disposal date is required")
    val disposalDate: LocalDate?,
    @field:NotNull(message = "Disposal type is required")
    val disposalType: DisposalType?,
    @field:PositiveOrZero(message = "Proceeds must be zero or positive")
    val proceeds: BigDecimal = BigDecimal.ZERO,
    val gainLossAccountId: String? = null,
    val cashAccountId: String? = null,
    @field:Size(max = 1000)
    val notes: String? = null,
)

data class AssetDisposalResponse(
    val id: String,
    val assetId: String,
    val disposalDate: String,
    val disposalType: DisposalType,
    val proceeds: BigDecimal,
    val status: DisposalStatus,
    val journalEntryId: String?,
    val gainLossAccountId: String?,
    val cashAccountId: String?,
    val notes: String?,
    val postedAt: String?,
    val postedBy: String?,
    val organizationId: String,
    val createdAt: String?,
    val updatedAt: String?,
) {
    companion object {
        fun from(disposal: AssetDisposal) =
            AssetDisposalResponse(
                id = disposal.id.toString(),
                assetId = disposal.assetId.toString(),
                disposalDate = disposal.disposalDate.toString(),
                disposalType = disposal.disposalType,
                proceeds = disposal.proceeds,
                status = disposal.status,
                journalEntryId = disposal.journalEntryId?.toString(),
                gainLossAccountId = disposal.gainLossAccountId?.toString(),
                cashAccountId = disposal.cashAccountId?.toString(),
                notes = disposal.notes,
                postedAt = disposal.postedAt?.toString(),
                postedBy = disposal.postedBy?.toString(),
                organizationId = disposal.organizationId.toString(),
                createdAt = disposal.createdAt?.toString(),
                updatedAt = (disposal.updatedAt ?: disposal.createdAt)?.toString(),
            )
    }
}
