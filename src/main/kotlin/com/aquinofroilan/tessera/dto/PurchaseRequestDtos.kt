package com.aquinofroilan.tessera.dto

import com.aquinofroilan.tessera.model.PurchaseRequest
import com.aquinofroilan.tessera.model.PurchaseRequestStatus
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.math.BigDecimal
import java.time.LocalDate

data class CreatePurchaseRequestLineRequest(
    @field:NotBlank(message = "Product ID is required")
    val productId: String,
    @field:NotNull(message = "Quantity is required")
    @field:Positive(message = "Quantity must be positive")
    val quantity: BigDecimal?,
    val estimatedUnitCost: BigDecimal? = null,
    val description: String? = null,
)

data class CreatePurchaseRequestRequest(
    val suggestedVendorId: String? = null,
    val warehouseId: String? = null,
    val justification: String? = null,
    @field:NotEmpty(message = "At least one line item is required")
    @field:Valid
    val lines: List<CreatePurchaseRequestLineRequest>,
)

data class RejectPurchaseRequestRequest(
    val reason: String? = null,
)

/**
 * Per-line unit-cost override applied when converting a request into a PO.
 * Lines without an override fall back to their estimated unit cost.
 */
data class ConvertPurchaseRequestLineCost(
    @field:NotBlank(message = "Line ID is required")
    val lineId: String,
    @field:NotNull(message = "Unit cost is required")
    val unitCost: BigDecimal?,
)

data class ConvertPurchaseRequestRequest(
    val vendorId: String? = null,
    val warehouseId: String? = null,
    val orderDate: LocalDate? = null,
    val expectedDate: LocalDate? = null,
    @field:Valid
    val lineCosts: List<ConvertPurchaseRequestLineCost>? = null,
)

data class PurchaseRequestLineResponse(
    val id: String,
    val lineNumber: Int,
    val productId: String,
    val productSku: String,
    val productName: String,
    val quantity: BigDecimal,
    val estimatedUnitCost: BigDecimal?,
    val description: String?,
)

data class PurchaseRequestResponse(
    val id: String,
    val prNumber: String,
    val organizationId: String,
    val status: PurchaseRequestStatus,
    val suggestedVendorId: String?,
    val warehouseId: String?,
    val justification: String?,
    val lines: List<PurchaseRequestLineResponse>,
    val requestedBy: String,
    val decidedBy: String?,
    val decidedAt: String?,
    val decisionReason: String?,
    val convertedPurchaseOrderId: String?,
    val createdAt: String?,
    val updatedAt: String?,
) {
    companion object {
        fun from(request: PurchaseRequest) =
            PurchaseRequestResponse(
                id = request.id,
                prNumber = request.prNumber,
                organizationId = request.organizationId,
                status = request.status,
                suggestedVendorId = request.suggestedVendorId,
                warehouseId = request.warehouseId,
                justification = request.justification,
                lines =
                    request.lines.map { line ->
                        PurchaseRequestLineResponse(
                            id = line.id,
                            lineNumber = line.lineNumber,
                            productId = line.productId,
                            productSku = line.productSku,
                            productName = line.productName,
                            quantity = line.quantity,
                            estimatedUnitCost = line.estimatedUnitCost,
                            description = line.description,
                        )
                    },
                requestedBy = request.requestedBy,
                decidedBy = request.decidedBy,
                decidedAt = request.decidedAt?.toString(),
                decisionReason = request.decisionReason,
                convertedPurchaseOrderId = request.convertedPurchaseOrderId,
                createdAt = request.createdAt?.toString(),
                updatedAt = request.updatedAt?.toString(),
            )
    }
}
