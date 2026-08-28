package com.aquinofroilan.tessera.domain.procurement.dto

import com.aquinofroilan.tessera.domain.procurement.model.VendorEvaluation
import jakarta.validation.constraints.DecimalMax
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class VendorPerformanceSummaryResponse(
    val vendorId: UUID,
    val vendorName: String,
    val totalOrders: Int,
    val completedOrders: Int,
    val totalSpend: BigDecimal,
    val onTimeDeliveryRate: Double,
    val averageDeliveryDelayDays: Double,
    val qualityFulfillmentRate: Double,
    val priceAccuracyRate: Double,
    val deliveryScore: Double,
    val qualityScore: Double,
    val priceAccuracyScore: Double,
    val overallScore: Double,
    val ratingTier: String,
    val totalEvaluations: Int,
    val evaluationAverageScore: Double?,
)

data class CreateVendorEvaluationRequest(
    val evaluationDate: LocalDate? = null,
    val purchaseOrderId: UUID? = null,
    @field:NotNull(message = "Delivery score is required")
    @field:DecimalMin(value = "0.0", message = "Delivery score must be between 0 and 100")
    @field:DecimalMax(value = "100.0", message = "Delivery score must be between 0 and 100")
    val deliveryScore: BigDecimal,
    @field:NotNull(message = "Quality score is required")
    @field:DecimalMin(value = "0.0", message = "Quality score must be between 0 and 100")
    @field:DecimalMax(value = "100.0", message = "Quality score must be between 0 and 100")
    val qualityScore: BigDecimal,
    @field:NotNull(message = "Price accuracy score is required")
    @field:DecimalMin(value = "0.0", message = "Price accuracy score must be between 0 and 100")
    @field:DecimalMax(value = "100.0", message = "Price accuracy score must be between 0 and 100")
    val priceAccuracyScore: BigDecimal,
    val comments: String? = null,
)

data class VendorEvaluationResponse(
    val id: UUID,
    val vendorId: UUID,
    val organizationId: UUID,
    val purchaseOrderId: UUID?,
    val evaluationDate: LocalDate,
    val deliveryScore: BigDecimal,
    val qualityScore: BigDecimal,
    val priceAccuracyScore: BigDecimal,
    val overallScore: BigDecimal,
    val comments: String?,
    val evaluatedBy: UUID,
    val createdAt: LocalDateTime,
) {
    companion object {
        fun from(evaluation: VendorEvaluation): VendorEvaluationResponse =
            VendorEvaluationResponse(
                id = evaluation.id,
                vendorId = evaluation.vendorId,
                organizationId = evaluation.organizationId,
                purchaseOrderId = evaluation.purchaseOrderId,
                evaluationDate = evaluation.evaluationDate,
                deliveryScore = evaluation.deliveryScore,
                qualityScore = evaluation.qualityScore,
                priceAccuracyScore = evaluation.priceAccuracyScore,
                overallScore = evaluation.overallScore,
                comments = evaluation.comments,
                evaluatedBy = evaluation.evaluatedBy,
                createdAt = evaluation.createdAt,
            )
    }
}
