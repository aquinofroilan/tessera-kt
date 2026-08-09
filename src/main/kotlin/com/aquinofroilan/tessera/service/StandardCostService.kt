package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.ManualStandardCostRequest
import com.aquinofroilan.tessera.dto.RollupRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import com.aquinofroilan.tessera.model.BomStatus
import com.aquinofroilan.tessera.model.ProductStandardCost
import com.aquinofroilan.tessera.model.RoutingStatus
import com.aquinofroilan.tessera.repository.ProductStandardCostRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime

/**
 * Single-level standard-cost rollup.
 *
 * Material cost = sum over BOM lines of (component standard cost x
 * line.quantity x (1 + scrap_pct/100)). Component standard costs come from
 * this same table; if a component has no row, callers must roll it up first.
 *
 * Labour cost = sum over routing operations of work_center.cost_per_hour x
 * (setup_minutes + run_minutes_per_unit) / 60.
 *
 * Overhead cost = material + labour times overheadRatePct/100.
 */
@Service
class StandardCostService(
    private val standardCostRepository: ProductStandardCostRepository,
    private val productService: ProductService,
    private val bomService: BillOfMaterialsService,
    private val routingService: RoutingService,
    private val workCenterService: WorkCenterService,
) {
    @Transactional
    fun rollup(
        productId: java.util.UUID,
        request: RollupRequest,
        organizationId: java.util.UUID,
        userId: String,
    ): ProductStandardCost {
        val product = productService.getProduct(productId, organizationId)
        if (!product.isActive) {
            throw BusinessRuleException("Product '${product.sku}' is inactive")
        }
        val bom = resolveBom(request.bomId, organizationId, product.id)
        val routing =
            request.routingId?.let { resolveRouting(it, organizationId, product.id) }
                ?: routingService.listRoutings(organizationId, RoutingStatus.ACTIVE, product.id).firstOrNull { it.isDefault }

        val materialCost =
            bom.lines
                .fold(BigDecimal.ZERO) { acc, line ->
                    val componentCost = lookupCost(organizationId, line.componentProductId)
                    val factor = BigDecimal.ONE.add(line.scrapPct.divide(BigDecimal(100), 6, RoundingMode.HALF_UP))
                    acc.add(componentCost.multiply(line.quantity).multiply(factor))
                }.setScale(4, RoundingMode.HALF_UP)

        val laborCost =
            (routing?.operations ?: emptyList())
                .fold(BigDecimal.ZERO) { acc, op ->
                    // We pull cost_per_hour off the work center via a lookup on the routing snapshot.
                    // Snapshot only stored code/id, so we have to resolve via WorkCenterService -- but to keep
                    // this service light, the routing operation carries enough for caller-provided overhead;
                    // labour at routing time is approximated as setup + run-per-unit minutes -- caller multiplies
                    // by the work center's cost_per_hour. To stay self-contained here, we read it back.
                    val minutes = op.setupMinutes.add(op.runMinutesPerUnit)
                    val hours = minutes.divide(BigDecimal(60), 6, RoundingMode.HALF_UP)
                    val costPerHour = workCenterCostPerHour(organizationId, op.workCenterId)
                    acc.add(hours.multiply(costPerHour))
                }.setScale(4, RoundingMode.HALF_UP)

        val overheadRate = request.overheadRatePct ?: BigDecimal.ZERO
        val overheadCost =
            materialCost
                .add(laborCost)
                .multiply(overheadRate.divide(BigDecimal(100), 6, RoundingMode.HALF_UP))
                .setScale(4, RoundingMode.HALF_UP)
        val total = materialCost.add(laborCost).add(overheadCost).setScale(4, RoundingMode.HALF_UP)

        val existing = standardCostRepository.findByOrganizationIdAndProductId(organizationId, product.id)
        val record =
            existing
                .map {
                    it.copy(
                        bomId = bom.id,
                        routingId = routing?.id,
                        materialCost = materialCost,
                        laborCost = laborCost,
                        overheadCost = overheadCost,
                        totalCost = total,
                        source = "rollup",
                        calculatedAt = LocalDateTime.now(),
                        calculatedBy = userId,
                        notes = request.notes,
                    )
                }.orElseGet {
                    ProductStandardCost(
                        organizationId = organizationId,
                        productId = product.id,
                        bomId = bom.id,
                        routingId = routing?.id,
                        materialCost = materialCost,
                        laborCost = laborCost,
                        overheadCost = overheadCost,
                        totalCost = total,
                        source = "rollup",
                        calculatedBy = userId,
                        notes = request.notes,
                    )
                }
        return standardCostRepository.save(record)
    }

    @Transactional
    fun setManual(
        productId: java.util.UUID,
        request: ManualStandardCostRequest,
        organizationId: java.util.UUID,
        userId: String,
    ): ProductStandardCost {
        val product = productService.getProduct(productId, organizationId)
        val material = request.materialCost ?: BigDecimal.ZERO
        val labor = request.laborCost ?: BigDecimal.ZERO
        val overhead = request.overheadCost ?: BigDecimal.ZERO
        val total = material.add(labor).add(overhead).setScale(4, RoundingMode.HALF_UP)
        val existing = standardCostRepository.findByOrganizationIdAndProductId(organizationId, product.id)
        val record =
            existing
                .map {
                    it.copy(
                        bomId = null,
                        routingId = null,
                        materialCost = material,
                        laborCost = labor,
                        overheadCost = overhead,
                        totalCost = total,
                        source = "manual",
                        calculatedAt = LocalDateTime.now(),
                        calculatedBy = userId,
                        notes = request.notes,
                    )
                }.orElseGet {
                    ProductStandardCost(
                        organizationId = organizationId,
                        productId = product.id,
                        materialCost = material,
                        laborCost = labor,
                        overheadCost = overhead,
                        totalCost = total,
                        source = "manual",
                        calculatedBy = userId,
                        notes = request.notes,
                    )
                }
        return standardCostRepository.save(record)
    }

    fun getStandardCost(
        productId: java.util.UUID,
        organizationId: java.util.UUID,
    ): ProductStandardCost =
        standardCostRepository.findByOrganizationIdAndProductId(organizationId, productId).orElseThrow {
            ResourceNotFoundException("No standard cost recorded for product $productId")
        }

    fun listStandardCosts(organizationId: java.util.UUID): List<ProductStandardCost> =
        standardCostRepository.findByOrganizationId(organizationId)

    private fun resolveBom(
        bomId: java.util.UUID?,
        organizationId: java.util.UUID,
        productId: java.util.UUID,
    ) = if (bomId != null) {
        bomService.getBom(bomId, organizationId).also {
            if (it.productId != productId) throw BusinessRuleException("BOM '${it.code}' is not for product $productId")
            if (it.status != BomStatus.ACTIVE) throw BusinessRuleException("BOM '${it.code}' is ${it.status}; only ACTIVE BOMs roll up")
        }
    } else {
        bomService.listBoms(organizationId, BomStatus.ACTIVE, productId).firstOrNull { it.isDefault }
            ?: throw BusinessRuleException("No default ACTIVE BOM exists for product $productId")
    }

    private fun resolveRouting(
        routingId: java.util.UUID,
        organizationId: java.util.UUID,
        productId: java.util.UUID,
    ) = routingService.getRouting(routingId, organizationId).also {
        if (it.productId != productId) throw BusinessRuleException("Routing '${it.code}' is not for product $productId")
        if (it.status != RoutingStatus.ACTIVE) throw BusinessRuleException("Routing '${it.code}' is ${it.status}")
    }

    private fun lookupCost(
        organizationId: java.util.UUID,
        componentProductId: java.util.UUID,
    ): BigDecimal =
        standardCostRepository
            .findByOrganizationIdAndProductId(organizationId, componentProductId)
            .map { it.totalCost }
            .orElseThrow {
                BusinessRuleException("Component $componentProductId has no standard cost; roll it up first")
            }

    private fun workCenterCostPerHour(
        organizationId: java.util.UUID,
        workCenterId: java.util.UUID,
    ): BigDecimal {
        val wc =
            try {
                workCenterService.getWorkCenter(workCenterId, organizationId)
            } catch (e: ResourceNotFoundException) {
                return BigDecimal.ZERO
            }
        return wc.costPerHour
    }
}
