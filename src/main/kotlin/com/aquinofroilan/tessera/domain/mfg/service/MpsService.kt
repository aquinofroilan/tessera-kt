package com.aquinofroilan.tessera.domain.mfg.service

import com.aquinofroilan.tessera.domain.inventory.service.ProductService
import com.aquinofroilan.tessera.domain.mfg.model.MpsEntry
import com.aquinofroilan.tessera.domain.mfg.model.MpsStatus
import com.aquinofroilan.tessera.domain.mfg.repository.MpsEntryRepository
import com.aquinofroilan.tessera.domain.platform.dto.CreateMpsEntryRequest
import com.aquinofroilan.tessera.domain.platform.dto.UpdateMpsEntryRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MpsService(
    private val mpsRepository: MpsEntryRepository,
    private val productService: ProductService,
) {
    @Transactional
    fun create(
        request: CreateMpsEntryRequest,
        organizationId: java.util.UUID,
        userId: String,
    ): MpsEntry {
        val product = productService.getProduct(request.productId, organizationId)
        if (!product.isActive) {
            throw BusinessRuleException("Product '${product.sku}' is inactive")
        }
        val quantity = request.quantity ?: throw BusinessRuleException("Quantity is required")
        val requiredBy = request.requiredBy ?: throw BusinessRuleException("requiredBy is required")
        return mpsRepository.save(
            MpsEntry(
                organizationId = organizationId,
                productId = product.id,
                productSku = product.sku,
                productName = product.name,
                quantity = quantity,
                requiredBy = requiredBy,
                status = request.status,
                notes = request.notes,
                createdBy = userId,
            ),
        )
    }

    fun list(
        organizationId: java.util.UUID,
        status: MpsStatus?,
    ): List<MpsEntry> =
        if (status != null) {
            mpsRepository.findByOrganizationIdAndStatusOrderByRequiredByAsc(organizationId, status)
        } else {
            mpsRepository.findByOrganizationIdOrderByRequiredByAsc(organizationId)
        }

    fun get(
        id: java.util.UUID,
        organizationId: java.util.UUID,
    ): MpsEntry {
        val e = mpsRepository.findById(id).orElseThrow { ResourceNotFoundException("MPS entry not found: $id") }
        if (e.organizationId != organizationId) {
            throw ResourceNotFoundException("MPS entry not found: $id")
        }
        return e
    }

    @Transactional
    fun update(
        id: java.util.UUID,
        request: UpdateMpsEntryRequest,
        organizationId: java.util.UUID,
    ): MpsEntry {
        val e = get(id, organizationId)
        if (e.status == MpsStatus.RELEASED || e.status == MpsStatus.CANCELLED) {
            throw BusinessRuleException("Cannot edit a ${e.status} MPS entry")
        }
        return mpsRepository.save(
            e.copy(
                quantity = request.quantity ?: e.quantity,
                requiredBy = request.requiredBy ?: e.requiredBy,
                status = request.status ?: e.status,
                notes = request.notes ?: e.notes,
            ),
        )
    }

    @Transactional
    fun delete(
        id: java.util.UUID,
        organizationId: java.util.UUID,
    ) {
        val e = get(id, organizationId)
        if (e.status == MpsStatus.RELEASED) {
            throw BusinessRuleException("Cannot delete a RELEASED MPS entry")
        }
        mpsRepository.delete(e)
    }
}
