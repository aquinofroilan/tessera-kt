package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.CreateReorderRuleRequest
import com.aquinofroilan.tessera.dto.LowStockLineResponse
import com.aquinofroilan.tessera.dto.LowStockReportResponse
import com.aquinofroilan.tessera.dto.UpdateReorderRuleRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import com.aquinofroilan.tessera.model.InventoryReorderRule
import com.aquinofroilan.tessera.repository.InventoryReorderRuleRepository
import com.aquinofroilan.tessera.repository.OnHandKey
import com.aquinofroilan.tessera.repository.StockMovementRepository
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
class InventoryReorderRuleService(
    private val reorderRuleRepository: InventoryReorderRuleRepository,
    private val stockMovementRepository: StockMovementRepository,
) {
    @Transactional
    fun createRule(
        request: CreateReorderRuleRequest,
        organizationId: String,
    ): InventoryReorderRule {
        val reorderPoint = request.reorderPoint ?: throw BusinessRuleException("Reorder point is required")
        val rule =
            InventoryReorderRule(
                organizationId = organizationId,
                productId = request.productId,
                warehouseId = request.warehouseId,
                reorderPoint = reorderPoint,
                safetyStock = request.safetyStock ?: BigDecimal.ZERO,
            )
        return try {
            reorderRuleRepository.save(rule)
        } catch (e: DuplicateKeyException) {
            throw BusinessRuleException(
                "Reorder rule already exists for product '${request.productId}' " +
                    "in warehouse '${request.warehouseId}'",
                e,
            )
        }
    }

    fun listRules(organizationId: String): List<InventoryReorderRule> =
        reorderRuleRepository.findByOrganizationId(organizationId).sortedWith(
            compareBy({ it.productId }, { it.warehouseId }),
        )

    fun getRule(
        ruleId: String,
        organizationId: String,
    ): InventoryReorderRule {
        val rule =
            reorderRuleRepository.findById(ruleId).orElseThrow {
                ResourceNotFoundException("Reorder rule not found")
            }
        if (rule.organizationId != organizationId) {
            throw ResourceNotFoundException("Reorder rule not found")
        }
        return rule
    }

    @Transactional
    fun updateRule(
        ruleId: String,
        request: UpdateReorderRuleRequest,
        organizationId: String,
    ): InventoryReorderRule {
        val existing = getRule(ruleId, organizationId)
        val updated =
            existing.copy(
                reorderPoint = request.reorderPoint ?: existing.reorderPoint,
                safetyStock = request.safetyStock ?: existing.safetyStock,
            )
        return reorderRuleRepository.save(updated)
    }

    @Transactional
    fun deleteRule(
        ruleId: String,
        organizationId: String,
    ) {
        val existing = getRule(ruleId, organizationId)
        reorderRuleRepository.delete(existing)
    }

    fun lowStockReport(organizationId: String): LowStockReportResponse {
        val rules = reorderRuleRepository.findByOrganizationId(organizationId)
        val onHand: Map<OnHandKey, BigDecimal> =
            stockMovementRepository.onHandByProductWarehouse(organizationId)
        val lines =
            rules
                .mapNotNull { rule ->
                    val qty = onHand[OnHandKey(rule.productId, rule.warehouseId)] ?: BigDecimal.ZERO
                    if (qty < rule.reorderPoint) {
                        LowStockLineResponse(
                            productId = rule.productId,
                            warehouseId = rule.warehouseId,
                            onHand = qty,
                            reorderPoint = rule.reorderPoint,
                            safetyStock = rule.safetyStock,
                            shortfall = rule.reorderPoint - qty,
                        )
                    } else {
                        null
                    }
                }.sortedWith(compareBy({ it.productId }, { it.warehouseId }))
        return LowStockReportResponse(lines = lines)
    }
}
