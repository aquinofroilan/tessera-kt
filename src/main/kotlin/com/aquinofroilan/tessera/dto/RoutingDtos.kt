package com.aquinofroilan.tessera.dto

import com.aquinofroilan.tessera.model.Routing
import com.aquinofroilan.tessera.model.RoutingOperation
import com.aquinofroilan.tessera.model.RoutingStatus
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDate

data class CreateRoutingOperationRequest(
    @field:NotBlank(message = "Work center ID is required")
    val workCenterId: String,
    @field:NotBlank(message = "Description is required")
    val description: String,
    @field:PositiveOrZero(message = "Setup minutes cannot be negative")
    val setupMinutes: BigDecimal? = null,
    @field:PositiveOrZero(message = "Run minutes cannot be negative")
    val runMinutesPerUnit: BigDecimal? = null,
    @field:PositiveOrZero(message = "Queue minutes cannot be negative")
    val queueMinutes: BigDecimal? = null,
    val instructions: String? = null,
)

data class CreateRoutingRequest(
    @field:NotBlank(message = "Product ID is required")
    val productId: String,
    @field:NotBlank(message = "Routing code is required")
    @field:Size(max = 64)
    val code: String,
    @field:NotBlank(message = "Routing name is required")
    val name: String,
    val version: Int? = null,
    val effectiveFrom: LocalDate? = null,
    val effectiveTo: LocalDate? = null,
    val notes: String? = null,
    @field:NotEmpty(message = "At least one operation is required")
    @field:Valid
    val operations: List<CreateRoutingOperationRequest>,
)

data class UpdateRoutingRequest(
    val name: String? = null,
    val effectiveFrom: LocalDate? = null,
    val effectiveTo: LocalDate? = null,
    val notes: String? = null,
    @field:Valid
    val operations: List<CreateRoutingOperationRequest>? = null,
)

data class RoutingOperationResponse(
    val id: String,
    val operationNumber: Int,
    val workCenterId: String,
    val workCenterCode: String,
    val description: String,
    val setupMinutes: BigDecimal,
    val runMinutesPerUnit: BigDecimal,
    val queueMinutes: BigDecimal,
    val instructions: String?,
) {
    companion object {
        fun from(op: RoutingOperation) =
            RoutingOperationResponse(
                id = op.id,
                operationNumber = op.operationNumber,
                workCenterId = op.workCenterId,
                workCenterCode = op.workCenterCode,
                description = op.description,
                setupMinutes = op.setupMinutes,
                runMinutesPerUnit = op.runMinutesPerUnit,
                queueMinutes = op.queueMinutes,
                instructions = op.instructions,
            )
    }
}

data class RoutingResponse(
    val id: String,
    val productId: String,
    val code: String,
    val name: String,
    val version: Int,
    val status: RoutingStatus,
    val isDefault: Boolean,
    val effectiveFrom: LocalDate?,
    val effectiveTo: LocalDate?,
    val notes: String?,
    val operations: List<RoutingOperationResponse>,
) {
    companion object {
        fun from(r: Routing) =
            RoutingResponse(
                id = r.id,
                productId = r.productId,
                code = r.code,
                name = r.name,
                version = r.version,
                status = r.status,
                isDefault = r.isDefault,
                effectiveFrom = r.effectiveFrom,
                effectiveTo = r.effectiveTo,
                notes = r.notes,
                operations = r.operations.map { RoutingOperationResponse.from(it) },
            )
    }
}
