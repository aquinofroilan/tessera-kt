package com.aquinofroilan.tessera.domain.inventory.service

import com.aquinofroilan.tessera.domain.inventory.model.Product
import com.aquinofroilan.tessera.domain.inventory.model.WarehouseLocation
import com.aquinofroilan.tessera.domain.inventory.repository.ProductRepository
import com.aquinofroilan.tessera.domain.inventory.repository.StockOnHandRepository
import com.aquinofroilan.tessera.exception.BusinessRuleException
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.UUID

@Service
class WarehouseLocationCapacityService(
    private val stockOnHandRepository: StockOnHandRepository,
    private val productRepository: ProductRepository,
) {
    fun validateCurrentStock(location: WarehouseLocation, organizationId: UUID) {
        if (location.maxWeightKg == null && location.maxVolumeCbm == null) return
        val currentLoad = currentLoad(location, organizationId)
        validateLimit("weight", currentLoad.weightKg, location.maxWeightKg)
        validateLimit("volume", currentLoad.volumeCbm, location.maxVolumeCbm)
    }

    fun validateIncomingStock(
        location: WarehouseLocation,
        product: Product,
        quantity: BigDecimal,
        organizationId: UUID,
    ) {
        if (quantity.signum() <= 0 || (location.maxWeightKg == null && location.maxVolumeCbm == null)) return

        val currentLoad = currentLoad(location, organizationId)
        val addedWeight =
            if (location.maxWeightKg != null) {
                product.unitWeightKg
                    ?: throw BusinessRuleException("Product requires unitWeightKg for this location capacity")
            } else {
                BigDecimal.ZERO
            }
        val addedVolume =
            if (location.maxVolumeCbm != null) {
                product.unitVolumeCbm
                    ?: throw BusinessRuleException("Product requires unitVolumeCbm for this location capacity")
            } else {
                BigDecimal.ZERO
            }

        validateLimit("weight", currentLoad.weightKg + addedWeight * quantity, location.maxWeightKg)
        validateLimit("volume", currentLoad.volumeCbm + addedVolume * quantity, location.maxVolumeCbm)
    }

    private fun currentLoad(
        location: WarehouseLocation,
        organizationId: UUID,
    ): LocationLoad {
        val rows =
            stockOnHandRepository.findByOrganizationIdAndWarehouseIdAndLocationId(
                organizationId,
                location.warehouseId,
                location.id,
            )
        if (rows.isEmpty()) return LocationLoad()

        val products = productRepository.findAllById(rows.map { it.productId }.distinct()).associateBy { it.id }
        var weightKg = BigDecimal.ZERO
        var volumeCbm = BigDecimal.ZERO
        rows.filter { it.quantity.signum() > 0 }.forEach { row ->
            val product =
                products[row.productId]
                    ?: throw BusinessRuleException("Cannot validate capacity because an existing product was not found")
            if (location.maxWeightKg != null) {
                val unitWeight =
                    product.unitWeightKg
                        ?: throw BusinessRuleException("Existing product requires unitWeightKg for this location capacity")
                weightKg += unitWeight * row.quantity
            }
            if (location.maxVolumeCbm != null) {
                val unitVolume =
                    product.unitVolumeCbm
                        ?: throw BusinessRuleException("Existing product requires unitVolumeCbm for this location capacity")
                volumeCbm += unitVolume * row.quantity
            }
        }
        return LocationLoad(weightKg, volumeCbm)
    }

    private fun validateLimit(
        dimension: String,
        load: BigDecimal,
        maximum: BigDecimal?,
    ) {
        if (maximum != null && load > maximum) {
            throw BusinessRuleException("Location $dimension capacity exceeded (maximum $maximum, projected $load)")
        }
    }

    private data class LocationLoad(
        val weightKg: BigDecimal = BigDecimal.ZERO,
        val volumeCbm: BigDecimal = BigDecimal.ZERO,
    )
}
