package com.aquinofroilan.tessera.dto

import com.aquinofroilan.tessera.model.BillOfMaterials
import com.aquinofroilan.tessera.model.BomLine
import com.aquinofroilan.tessera.model.BomStatus
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDate

data class CreateBomLineRequest(
    @field:NotBlank(message = "Component product ID is required")
    val componentProductId: String,
    @field:NotNull(message = "Quantity is required")
    @field:Positive(message = "Quantity must be positive")
    val quantity: BigDecimal?,
    val uom: String? = null,
    @field:PositiveOrZero(message = "Scrap percentage cannot be negative")
    val scrapPct: BigDecimal? = null,
    val notes: String? = null,
)

data class CreateBomRequest(
    @field:NotBlank(message = "Product ID is required")
    val productId: String,
    @field:NotBlank(message = "BOM code is required")
    @field:Size(max = 64)
    val code: String,
    @field:NotBlank(message = "BOM name is required")
    val name: String,
    val version: Int? = null,
    val isDefault: Boolean = false,
    val effectiveFrom: LocalDate? = null,
    val effectiveTo: LocalDate? = null,
    val notes: String? = null,
    @field:NotEmpty(message = "At least one component line is required")
    @field:Valid
    val lines: List<CreateBomLineRequest>,
)

data class UpdateBomRequest(
    val name: String? = null,
    val effectiveFrom: LocalDate? = null,
    val effectiveTo: LocalDate? = null,
    val notes: String? = null,
    @field:Valid
    val lines: List<CreateBomLineRequest>? = null,
)

data class BomLineResponse(
    val id: String,
    val lineNumber: Int,
    val componentProductId: String,
    val componentSku: String,
    val componentName: String,
    val quantity: BigDecimal,
    val uom: String?,
    val scrapPct: BigDecimal,
    val notes: String?,
) {
    companion object {
        fun from(line: BomLine) =
            BomLineResponse(
                id = line.id,
                lineNumber = line.lineNumber,
                componentProductId = line.componentProductId,
                componentSku = line.componentSku,
                componentName = line.componentName,
                quantity = line.quantity,
                uom = line.uom,
                scrapPct = line.scrapPct,
                notes = line.notes,
            )
    }
}

data class BomResponse(
    val id: String,
    val productId: String,
    val code: String,
    val name: String,
    val version: Int,
    val status: BomStatus,
    val isDefault: Boolean,
    val effectiveFrom: LocalDate?,
    val effectiveTo: LocalDate?,
    val notes: String?,
    val lines: List<BomLineResponse>,
) {
    companion object {
        fun from(bom: BillOfMaterials) =
            BomResponse(
                id = bom.id,
                productId = bom.productId,
                code = bom.code,
                name = bom.name,
                version = bom.version,
                status = bom.status,
                isDefault = bom.isDefault,
                effectiveFrom = bom.effectiveFrom,
                effectiveTo = bom.effectiveTo,
                notes = bom.notes,
                lines = bom.lines.map { BomLineResponse.from(it) },
            )
    }
}
