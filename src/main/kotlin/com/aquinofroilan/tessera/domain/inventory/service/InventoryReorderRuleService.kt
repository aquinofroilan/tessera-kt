package com.aquinofroilan.tessera.domain.inventory.service

import com.aquinofroilan.tessera.domain.inventory.dto.CreateReorderRuleRequest
import com.aquinofroilan.tessera.domain.inventory.dto.LowStockLineResponse
import com.aquinofroilan.tessera.domain.inventory.dto.LowStockReportResponse
import com.aquinofroilan.tessera.domain.inventory.dto.UpdateReorderRuleRequest
import com.aquinofroilan.tessera.domain.inventory.model.InventoryReorderRule
import com.aquinofroilan.tessera.domain.inventory.repository.InventoryReorderRuleRepository
import com.aquinofroilan.tessera.domain.inventory.repository.OnHandKey
import com.aquinofroilan.tessera.domain.inventory.repository.StockMovementRepository
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
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
        organizationId: java.util.UUID,
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

    fun listRules(organizationId: java.util.UUID): List<InventoryReorderRule> =
        reorderRuleRepository.findByOrganizationId(organizationId).sortedWith(
            compareBy({ it.productId }, { it.warehouseId }),
        )

    fun getRule(
        ruleId: java.util.UUID,
        organizationId: java.util.UUID,
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
        ruleId: java.util.UUID,
        request: UpdateReorderRuleRequest,
        organizationId: java.util.UUID,
    ): InventoryReorderRule {
        val existing = getRule(ruleId, organizationId)
        existing.apply {
            reorderPoint = request.reorderPoint ?: existing.reorderPoint
            safetyStock = request.safetyStock ?: existing.safetyStock
        }
        return reorderRuleRepository.save(existing)
    }

    @Transactional
    fun deleteRule(
        ruleId: java.util.UUID,
        organizationId: java.util.UUID,
    ) {
        val existing = getRule(ruleId, organizationId)
        reorderRuleRepository.delete(existing)
    }

    fun lowStockReport(organizationId: java.util.UUID): LowStockReportResponse {
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
