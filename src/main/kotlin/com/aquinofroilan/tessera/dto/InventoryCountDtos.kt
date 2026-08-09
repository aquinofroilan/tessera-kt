package com.aquinofroilan.tessera.dto

import java.util.UUID

import com.aquinofroilan.tessera.model.InventoryCountLine
import com.aquinofroilan.tessera.model.InventoryCountSession
import com.aquinofroilan.tessera.model.InventoryCountStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.PositiveOrZero
import java.math.BigDecimal
import java.time.LocalDate

data class CreateCountSessionRequest(
    @field:NotBlank(message = "Code is required")
    val code: String,
    @field:NotBlank(message = "Warehouse ID is required")
    val warehouseId: UUID,
    val scheduledFor: LocalDate? = null,
    val notes: String? = null,
)

data class RecordCountRequest(
    @field:NotNull(message = "Counted quantity is required")
    @field:PositiveOrZero(message = "Counted quantity cannot be negative")
    val countedQuantity: BigDecimal?,
    val notes: String? = null,
)

data class CountLineResponse(
    val id: UUID,
    val lineNumber: Int,
    val productId: UUID,
    val productSku: String,
    val productName: String,
    val expectedQuantity: BigDecimal,
    val countedQuantity: BigDecimal?,
    val varianceQuantity: BigDecimal?,
    val adjustmentMovementId: UUID?,
    val notes: String?,
) {
    companion object {
        fun from(l: InventoryCountLine) =
            CountLineResponse(
                id = l.id,
                lineNumber = l.lineNumber,
                productId = l.productId,
                productSku = l.productSku,
                productName = l.productName,
                expectedQuantity = l.expectedQuantity,
                countedQuantity = l.countedQuantity,
                varianceQuantity = l.varianceQuantity,
                adjustmentMovementId = l.adjustmentMovementId,
                notes = l.notes,
            )
    }
}

data class CountSessionResponse(
    val id: UUID,
    val code: String,
    val warehouseId: UUID,
    val status: InventoryCountStatus,
    val scheduledFor: LocalDate?,
    val startedAt: String?,
    val postedAt: String?,
    val notes: String?,
    val lines: List<CountLineResponse>,
) {
    companion object {
        fun from(s: InventoryCountSession) =
            CountSessionResponse(
                id = s.id,
                code = s.code,
                warehouseId = s.warehouseId,
                status = s.status,
                scheduledFor = s.scheduledFor,
                startedAt = s.startedAt?.toString(),
                postedAt = s.postedAt?.toString(),
                notes = s.notes,
                lines = s.lines.map { CountLineResponse.from(it) },
            )
    }
}
