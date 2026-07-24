package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.ValuationLineResponse
import com.aquinofroilan.tessera.dto.ValuationReportResponse
import com.aquinofroilan.tessera.repository.StockMovementRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class InventoryValuationService(
    private val stockMovementRepository: StockMovementRepository,
    private val inventoryCostingService: InventoryCostingService,
) {
    fun valuation(organizationId: java.util.UUID): ValuationReportResponse {
        val method = inventoryCostingService.costingMethodFor(organizationId)
        val onHand = stockMovementRepository.onHandByProductWarehouse(organizationId)
        val lines =
            onHand
                .filter { it.value.signum() != 0 }
                .map { (key, qty) ->
                    val value = inventoryCostingService.valuationCost(organizationId, key.productId, key.warehouseId)
                    ValuationLineResponse(
                        productId = key.productId,
                        warehouseId = key.warehouseId,
                        quantity = qty,
                        totalValue = value,
                    )
                }.sortedWith(compareBy({ it.productId }, { it.warehouseId }))
        val total = lines.fold(BigDecimal.ZERO) { acc, l -> acc + l.totalValue }
        return ValuationReportResponse(
            costingMethod = method,
            lines = lines,
            totalValue = total,
        )
    }
}
