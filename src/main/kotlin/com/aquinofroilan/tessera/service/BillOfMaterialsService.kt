package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.CreateBomLineRequest
import com.aquinofroilan.tessera.dto.CreateBomRequest
import com.aquinofroilan.tessera.dto.UpdateBomRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import com.aquinofroilan.tessera.model.BillOfMaterials
import com.aquinofroilan.tessera.model.BomLine
import com.aquinofroilan.tessera.model.BomStatus
import com.aquinofroilan.tessera.repository.BillOfMaterialsRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
class BillOfMaterialsService(
    private val bomRepository: BillOfMaterialsRepository,
    private val productService: ProductService,
) {
    @Transactional
    fun createBom(
        request: CreateBomRequest,
        organizationId: java.util.UUID,
        createdBy: java.util.UUID,
    ): BillOfMaterials {
        val parent = productService.getProduct(request.productId, organizationId)
        if (!parent.isActive) {
            throw BusinessRuleException("Product '${parent.sku}' is inactive")
        }
        validateDateWindow(request.effectiveFrom, request.effectiveTo)
        bomRepository.findByOrganizationIdAndCode(organizationId, request.code).ifPresent {
            throw BusinessRuleException("BOM with code '${request.code}' already exists")
        }
        val lines = buildLines(request.lines, organizationId, parent.id)
        val bom =
            BillOfMaterials(
                organizationId = organizationId,
                productId = parent.id,
                code = request.code,
                name = request.name,
                version = request.version ?: nextVersion(organizationId, parent.id),
                status = BomStatus.DRAFT,
                isDefault = false,
                effectiveFrom = request.effectiveFrom,
                effectiveTo = request.effectiveTo,
                notes = request.notes,
                lines = lines,
                createdBy = createdBy,
            )
        return try {
            bomRepository.save(bom)
        } catch (e: DataIntegrityViolationException) {
            throw BusinessRuleException("BOM with code '${request.code}' already exists")
        }
    }

    fun getBom(
        id: java.util.UUID,
        organizationId: java.util.UUID,
    ): BillOfMaterials {
        val bom =
            bomRepository.findById(id).orElseThrow {
                ResourceNotFoundException("BOM not found: $id")
            }
        if (bom.organizationId != organizationId) {
            throw ResourceNotFoundException("BOM not found: $id")
        }
        return bom
    }

    fun listBoms(
        organizationId: java.util.UUID,
        status: BomStatus?,
        productId: java.util.UUID?,
    ): List<BillOfMaterials> =
        when {
            status != null && productId != null ->
                bomRepository.findByOrganizationIdAndProductIdAndStatus(organizationId, productId, status)
            status != null -> bomRepository.findByOrganizationIdAndStatus(organizationId, status)
            productId != null -> bomRepository.findByOrganizationIdAndProductId(organizationId, productId)
            else -> bomRepository.findByOrganizationId(organizationId)
        }

    @Transactional
    fun updateBom(
        id: java.util.UUID,
        request: UpdateBomRequest,
        organizationId: java.util.UUID,
    ): BillOfMaterials {
        val bom = getBom(id, organizationId)
        if (bom.status != BomStatus.DRAFT) {
            throw BusinessRuleException("Only DRAFT BOMs can be edited; this BOM is ${bom.status}")
        }
        validateDateWindow(
            request.effectiveFrom ?: bom.effectiveFrom,
            request.effectiveTo ?: bom.effectiveTo,
        )
        val updatedLines =
            request.lines?.let { buildLines(it, organizationId, bom.productId) } ?: bom.lines
        return bomRepository.save(
            bom.copy(
                name = request.name ?: bom.name,
                effectiveFrom = request.effectiveFrom ?: bom.effectiveFrom,
                effectiveTo = request.effectiveTo ?: bom.effectiveTo,
                notes = request.notes ?: bom.notes,
                lines = updatedLines,
            ),
        )
    }

    @Transactional
    fun activateBom(
        id: java.util.UUID,
        organizationId: java.util.UUID,
        userId: java.util.UUID,
        makeDefault: Boolean,
    ): BillOfMaterials {
        val bom = getBom(id, organizationId)
        if (bom.status == BomStatus.OBSOLETE) {
            throw BusinessRuleException("Cannot activate an OBSOLETE BOM")
        }
        if (bom.status == BomStatus.ACTIVE && !makeDefault) {
            return bom
        }
        if (makeDefault) {
            bomRepository
                .findByOrganizationIdAndProductIdAndIsDefaultTrue(organizationId, bom.productId)
                .ifPresent {
                    if (it.id != bom.id) {
                        bomRepository.save(it.copy(isDefault = false))
                    }
                }
        }
        return bomRepository.save(
            bom.copy(
                status = BomStatus.ACTIVE,
                isDefault = if (makeDefault) true else bom.isDefault,
                activatedAt = bom.activatedAt ?: LocalDateTime.now(),
                activatedBy = bom.activatedBy ?: userId,
            ),
        )
    }

    @Transactional
    fun obsoleteBom(
        id: java.util.UUID,
        organizationId: java.util.UUID,
        userId: java.util.UUID,
    ): BillOfMaterials {
        val bom = getBom(id, organizationId)
        if (bom.status == BomStatus.OBSOLETE) {
            return bom
        }
        return bomRepository.save(
            bom.copy(
                status = BomStatus.OBSOLETE,
                isDefault = false,
                obsoletedAt = LocalDateTime.now(),
                obsoletedBy = userId,
            ),
        )
    }

    @Transactional
    fun deleteBom(
        id: java.util.UUID,
        organizationId: java.util.UUID,
    ) {
        val bom = getBom(id, organizationId)
        if (bom.status != BomStatus.DRAFT) {
            throw BusinessRuleException("Only DRAFT BOMs can be deleted")
        }
        bomRepository.delete(bom)
    }

    private fun buildLines(
        requests: List<CreateBomLineRequest>,
        organizationId: java.util.UUID,
        parentProductId: java.util.UUID,
    ): List<BomLine> {
        if (requests.isEmpty()) {
            throw BusinessRuleException("BOM must have at least one component line")
        }
        val seen = mutableSetOf<java.util.UUID>()
        return requests.mapIndexed { index, lineReq ->
            val quantity = lineReq.quantity ?: throw BusinessRuleException("Line quantity is required")
            if (quantity.signum() <= 0) {
                throw BusinessRuleException("Line quantity must be positive")
            }
            val scrap = lineReq.scrapPct ?: BigDecimal.ZERO
            if (scrap.signum() < 0 || scrap >= BigDecimal(100)) {
                throw BusinessRuleException("Scrap percentage must be in [0, 100)")
            }
            if (lineReq.componentProductId == parentProductId) {
                throw BusinessRuleException("A BOM cannot list its own product as a component")
            }
            if (!seen.add(lineReq.componentProductId)) {
                throw BusinessRuleException("Duplicate component product: ${lineReq.componentProductId}")
            }
            val component = productService.getProduct(lineReq.componentProductId, organizationId)
            if (!component.isActive) {
                throw BusinessRuleException("Component '${component.sku}' is inactive")
            }
            BomLine(
                lineNumber = index + 1,
                componentProductId = component.id,
                componentSku = component.sku,
                componentName = component.name,
                quantity = quantity,
                uom = lineReq.uom,
                scrapPct = scrap,
                notes = lineReq.notes,
            )
        }
    }

    private fun nextVersion(
        organizationId: java.util.UUID,
        productId: java.util.UUID,
    ): Int {
        val existing = bomRepository.findByOrganizationIdAndProductId(organizationId, productId)
        return (existing.maxOfOrNull { it.version } ?: 0) + 1
    }

    private fun validateDateWindow(
        from: java.time.LocalDate?,
        to: java.time.LocalDate?,
    ) {
        if (from != null && to != null && to.isBefore(from)) {
            throw BusinessRuleException("Effective-to date must be on or after effective-from date")
        }
    }
}
