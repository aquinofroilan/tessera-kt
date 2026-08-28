package com.aquinofroilan.tessera.domain.procurement.service

import com.aquinofroilan.tessera.domain.procurement.dto.CreateVendorEvaluationRequest
import com.aquinofroilan.tessera.domain.procurement.dto.VendorEvaluationResponse
import com.aquinofroilan.tessera.domain.procurement.dto.VendorPerformanceSummaryResponse
import com.aquinofroilan.tessera.domain.procurement.model.PurchaseOrderStatus
import com.aquinofroilan.tessera.domain.procurement.model.VendorEvaluation
import com.aquinofroilan.tessera.domain.procurement.repository.PurchaseOrderRepository
import com.aquinofroilan.tessera.domain.procurement.repository.VendorEvaluationRepository
import com.aquinofroilan.tessera.domain.procurement.repository.VendorRepository
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
class VendorPerformanceService(
    private val vendorRepository: VendorRepository,
    private val purchaseOrderRepository: PurchaseOrderRepository,
    private val vendorEvaluationRepository: VendorEvaluationRepository,
) {
    @Transactional(readOnly = true)
    fun getVendorPerformance(
        vendorId: UUID,
        organizationId: UUID,
        fromDate: LocalDate? = null,
        toDate: LocalDate? = null,
    ): VendorPerformanceSummaryResponse {
        val vendor =
            vendorRepository.findByIdAndOrganizationId(vendorId, organizationId).orElseThrow {
                ResourceNotFoundException("Vendor $vendorId not found")
            }

        val allPos = purchaseOrderRepository.findByOrganizationIdAndVendorId(organizationId, vendorId)
        val filteredPos =
            allPos.filter { po ->
                val matchesFrom = fromDate?.let { !po.orderDate.isBefore(it) } ?: true
                val matchesTo = toDate?.let { !po.orderDate.isAfter(it) } ?: true
                matchesFrom && matchesTo
            }

        val nonDraftPos = filteredPos.filter { it.status != PurchaseOrderStatus.DRAFT }
        val totalOrders = nonDraftPos.size
        val completedPos = nonDraftPos.filter { it.status == PurchaseOrderStatus.RECEIVED || it.status == PurchaseOrderStatus.CLOSED }
        val completedOrders = completedPos.size

        val totalSpend =
            nonDraftPos
                .filter { it.status != PurchaseOrderStatus.CANCELLED }
                .fold(BigDecimal.ZERO) { acc, po -> acc.add(po.totalAmount) }

        // Delivery metric
        val deliveredPos = nonDraftPos.filter { it.receivedAt != null }
        var onTimeCount = 0
        var totalDelayDays = 0L
        var lateCount = 0

        for (po in deliveredPos) {
            val receivedDate = po.receivedAt!!.toLocalDate()
            val targetDate = po.expectedDate ?: po.orderDate
            if (!receivedDate.isAfter(targetDate)) {
                onTimeCount++
            } else {
                lateCount++
                totalDelayDays += ChronoUnit.DAYS.between(targetDate, receivedDate).coerceAtLeast(0)
            }
        }

        val onTimeDeliveryRate =
            if (deliveredPos.isNotEmpty()) {
                round((onTimeCount.toDouble() / deliveredPos.size.toDouble()) * 100.0)
            } else {
                100.0
            }

        val averageDeliveryDelayDays =
            if (lateCount > 0) {
                round(totalDelayDays.toDouble() / lateCount.toDouble())
            } else {
                0.0
            }

        val autoDeliveryScore = (onTimeDeliveryRate - (averageDeliveryDelayDays * 2.0)).coerceIn(0.0, 100.0)

        // Quality / Fulfillment metric
        var totalOrderedQty = BigDecimal.ZERO
        var totalReceivedQty = BigDecimal.ZERO

        for (po in nonDraftPos) {
            for (line in po.lines) {
                totalOrderedQty = totalOrderedQty.add(line.quantity)
                totalReceivedQty = totalReceivedQty.add(line.receivedQuantity)
            }
        }

        val qualityFulfillmentRate =
            if (totalOrderedQty > BigDecimal.ZERO) {
                val rate = totalReceivedQty.toDouble() / totalOrderedQty.toDouble() * 100.0
                round(rate.coerceIn(0.0, 100.0))
            } else {
                100.0
            }

        val autoQualityScore = qualityFulfillmentRate

        // Price accuracy metric
        var totalExpectedBilled = BigDecimal.ZERO
        var totalActualBilled = BigDecimal.ZERO
        var billedLinesCount = 0
        var accurateLinesCount = 0

        for (po in nonDraftPos) {
            for (line in po.lines) {
                if (line.billedQuantity > BigDecimal.ZERO) {
                    billedLinesCount++
                    if (line.billedQuantity.compareTo(line.receivedQuantity) <= 0) {
                        accurateLinesCount++
                    }
                }
            }
        }

        val priceAccuracyRate =
            if (billedLinesCount > 0) {
                round((accurateLinesCount.toDouble() / billedLinesCount.toDouble()) * 100.0)
            } else {
                100.0
            }

        val autoPriceAccuracyScore = priceAccuracyRate

        // Evaluations
        val evaluations =
            if (fromDate != null && toDate != null) {
                vendorEvaluationRepository.findByOrganizationIdAndVendorIdAndEvaluationDateBetweenOrderByEvaluationDateDesc(
                    organizationId,
                    vendorId,
                    fromDate,
                    toDate,
                )
            } else {
                vendorEvaluationRepository.findByOrganizationIdAndVendorIdOrderByEvaluationDateDesc(
                    organizationId,
                    vendorId,
                )
            }

        val totalEvaluations = evaluations.size
        val evaluationAverageScore =
            if (evaluations.isNotEmpty()) {
                round(evaluations.map { it.overallScore.toDouble() }.average())
            } else {
                null
            }

        val deliveryScore: Double
        val qualityScore: Double
        val priceAccuracyScore: Double

        if (evaluations.isNotEmpty()) {
            val evalDeliveryAvg = evaluations.map { it.deliveryScore.toDouble() }.average()
            val evalQualityAvg = evaluations.map { it.qualityScore.toDouble() }.average()
            val evalPriceAvg = evaluations.map { it.priceAccuracyScore.toDouble() }.average()

            deliveryScore = round(0.6 * autoDeliveryScore + 0.4 * evalDeliveryAvg)
            qualityScore = round(0.6 * autoQualityScore + 0.4 * evalQualityAvg)
            priceAccuracyScore = round(0.6 * autoPriceAccuracyScore + 0.4 * evalPriceAvg)
        } else {
            deliveryScore = round(autoDeliveryScore)
            qualityScore = round(autoQualityScore)
            priceAccuracyScore = round(autoPriceAccuracyScore)
        }

        val overallScore =
            round(0.40 * deliveryScore + 0.35 * qualityScore + 0.25 * priceAccuracyScore)

        val ratingTier =
            when {
                overallScore >= 90.0 -> "EXCELLENT"
                overallScore >= 75.0 -> "GOOD"
                overallScore >= 60.0 -> "FAIR"
                else -> "POOR"
            }

        return VendorPerformanceSummaryResponse(
            vendorId = vendor.id,
            vendorName = vendor.name,
            totalOrders = totalOrders,
            completedOrders = completedOrders,
            totalSpend = totalSpend,
            onTimeDeliveryRate = onTimeDeliveryRate,
            averageDeliveryDelayDays = averageDeliveryDelayDays,
            qualityFulfillmentRate = qualityFulfillmentRate,
            priceAccuracyRate = priceAccuracyRate,
            deliveryScore = deliveryScore,
            qualityScore = qualityScore,
            priceAccuracyScore = priceAccuracyScore,
            overallScore = overallScore,
            ratingTier = ratingTier,
            totalEvaluations = totalEvaluations,
            evaluationAverageScore = evaluationAverageScore,
        )
    }

    @Transactional(readOnly = true)
    fun listAllVendorPerformance(
        organizationId: UUID,
        fromDate: LocalDate? = null,
        toDate: LocalDate? = null,
    ): List<VendorPerformanceSummaryResponse> {
        val vendors = vendorRepository.findByOrganizationId(organizationId)
        return vendors
            .map { vendor ->
                getVendorPerformance(vendor.id, organizationId, fromDate, toDate)
            }.sortedByDescending { it.overallScore }
    }

    @Transactional
    fun recordEvaluation(
        vendorId: UUID,
        organizationId: UUID,
        userId: UUID,
        request: CreateVendorEvaluationRequest,
    ): VendorEvaluationResponse {
        val vendor =
            vendorRepository.findByIdAndOrganizationId(vendorId, organizationId).orElseThrow {
                ResourceNotFoundException("Vendor $vendorId not found")
            }

        val overall =
            (
                request.deliveryScore.multiply(BigDecimal("0.40")) +
                    request.qualityScore.multiply(BigDecimal("0.35")) +
                    request.priceAccuracyScore.multiply(BigDecimal("0.25"))
            ).setScale(2, RoundingMode.HALF_UP)

        val evaluation =
            VendorEvaluation(
                vendorId = vendor.id,
                organizationId = organizationId,
                purchaseOrderId = request.purchaseOrderId,
                evaluationDate = request.evaluationDate ?: LocalDate.now(),
                deliveryScore = request.deliveryScore.setScale(2, RoundingMode.HALF_UP),
                qualityScore = request.qualityScore.setScale(2, RoundingMode.HALF_UP),
                priceAccuracyScore = request.priceAccuracyScore.setScale(2, RoundingMode.HALF_UP),
                overallScore = overall,
                comments = request.comments,
                evaluatedBy = userId,
            )

        val saved = vendorEvaluationRepository.save(evaluation)
        return VendorEvaluationResponse.from(saved)
    }

    @Transactional(readOnly = true)
    fun listEvaluations(
        vendorId: UUID,
        organizationId: UUID,
    ): List<VendorEvaluationResponse> {
        vendorRepository.findByIdAndOrganizationId(vendorId, organizationId).orElseThrow {
            ResourceNotFoundException("Vendor $vendorId not found")
        }

        return vendorEvaluationRepository
            .findByOrganizationIdAndVendorIdOrderByEvaluationDateDesc(organizationId, vendorId)
            .map { VendorEvaluationResponse.from(it) }
    }

    @Transactional(readOnly = true)
    fun getEvaluation(
        vendorId: UUID,
        organizationId: UUID,
        evaluationId: UUID,
    ): VendorEvaluationResponse {
        val evaluation =
            vendorEvaluationRepository.findByOrganizationIdAndId(organizationId, evaluationId).orElseThrow {
                ResourceNotFoundException("Evaluation $evaluationId not found")
            }

        if (evaluation.vendorId != vendorId) {
            throw ResourceNotFoundException("Evaluation $evaluationId does not belong to vendor $vendorId")
        }

        return VendorEvaluationResponse.from(evaluation)
    }

    @Transactional
    fun deleteEvaluation(
        vendorId: UUID,
        organizationId: UUID,
        evaluationId: UUID,
    ) {
        val evaluation =
            vendorEvaluationRepository.findByOrganizationIdAndId(organizationId, evaluationId).orElseThrow {
                ResourceNotFoundException("Evaluation $evaluationId not found")
            }

        if (evaluation.vendorId != vendorId) {
            throw ResourceNotFoundException("Evaluation $evaluationId does not belong to vendor $vendorId")
        }

        vendorEvaluationRepository.delete(evaluation)
    }

    private fun round(value: Double): Double = BigDecimal(value).setScale(2, RoundingMode.HALF_UP).toDouble()
}
