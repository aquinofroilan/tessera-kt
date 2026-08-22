package com.aquinofroilan.tessera.domain.platform.dto

import com.aquinofroilan.tessera.domain.mfg.model.MpsEntry
import com.aquinofroilan.tessera.domain.mfg.model.MpsStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.math.BigDecimal
import java.time.LocalDate

data class CreateMpsEntryRequest(
    @field:NotBlank(message = "Product ID is required")
    val productId: java.util.UUID,
    @field:NotNull(message = "Quantity is required")
    @field:Positive(message = "Quantity must be positive")
    val quantity: BigDecimal?,
    @field:NotNull(message = "Required-by date is required")
    val requiredBy: LocalDate?,
    val status: MpsStatus = MpsStatus.PLANNED,
    val notes: String? = null,
)

data class UpdateMpsEntryRequest(
    @field:Positive
    val quantity: BigDecimal? = null,
    val requiredBy: LocalDate? = null,
    val status: MpsStatus? = null,
    val notes: String? = null,
)

data class MpsEntryResponse(
    val id: java.util.UUID,
    val productId: java.util.UUID,
    val productSku: String,
    val productName: String,
    val quantity: BigDecimal,
    val requiredBy: LocalDate,
    val status: MpsStatus,
    val notes: String?,
) {
    companion object {
        fun from(e: MpsEntry) =
            MpsEntryResponse(
                id = e.id,
                productId = e.productId,
                productSku = e.productSku,
                productName = e.productName,
                quantity = e.quantity,
                requiredBy = e.requiredBy,
                status = e.status,
                notes = e.notes,
            )
    }
}

data class MrpRequirementLine(
    val productId: java.util.UUID,
    val productSku: String,
    val productName: String,
    val grossRequirement: BigDecimal,
    val onHand: BigDecimal,
    val netRequirement: BigDecimal,
    val earliestRequiredBy: LocalDate,
)

data class MrpRunResponse(
    val horizonEnd: LocalDate?,
    val mpsEntriesConsidered: Int,
    val requirements: List<MrpRequirementLine>,
    val unresolved: List<String>,
)

data class CrpLoadLine(
    val workCenterId: java.util.UUID,
    val workCenterCode: String,
    val requiredMinutes: BigDecimal,
    val capacityMinutes: BigDecimal,
    val utilisationPct: BigDecimal,
    val overloaded: Boolean,
)

data class CrpRunResponse(
    val horizonEnd: LocalDate?,
    val mpsEntriesConsidered: Int,
    val capacityHoursPerWorkingDay: BigDecimal,
    val loads: List<CrpLoadLine>,
)
