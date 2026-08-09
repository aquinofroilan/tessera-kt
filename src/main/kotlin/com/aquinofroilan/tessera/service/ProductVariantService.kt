package com.aquinofroilan.tessera.service

import java.util.UUID

import com.aquinofroilan.tessera.dto.CreateProductVariantRequest
import com.aquinofroilan.tessera.dto.UpdateProductVariantRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import com.aquinofroilan.tessera.model.ProductVariant
import com.aquinofroilan.tessera.repository.ProductVariantRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProductVariantService(
    private val variantRepository: ProductVariantRepository,
    private val productService: ProductService,
) {
    @Transactional
    fun createVariant(
        productId: UUID,
        request: CreateProductVariantRequest,
        organizationId: UUID,
    ): ProductVariant {
        val product = productService.getProduct(productId, organizationId)
        val code = request.code.trim().uppercase()
        if (code.isBlank()) throw BusinessRuleException("Code cannot be blank")
        variantRepository.findByProductIdAndCode(product.id, code).ifPresent {
            throw BusinessRuleException("Variant with code '$code' already exists on this product")
        }
        val variant =
            ProductVariant(
                organizationId = organizationId,
                productId = product.id,
                code = code,
                name = request.name.trim(),
                skuSuffix = request.skuSuffix,
                attributes = request.attributes,
            )
        return try {
            variantRepository.save(variant)
        } catch (e: DataIntegrityViolationException) {
            throw BusinessRuleException("Variant with code '$code' already exists on this product")
        }
    }

    fun getVariant(
        id: UUID,
        organizationId: UUID,
    ): ProductVariant {
        val v =
            variantRepository.findById(id).orElseThrow {
                ResourceNotFoundException("Variant not found: $id")
            }
        if (v.organizationId != organizationId) {
            throw ResourceNotFoundException("Variant not found: $id")
        }
        return v
    }

    fun listVariants(
        productId: UUID,
        organizationId: UUID,
    ): List<ProductVariant> {
        productService.getProduct(productId, organizationId)
        return variantRepository.findByOrganizationIdAndProductId(organizationId, productId)
    }

    @Transactional
    fun updateVariant(
        id: UUID,
        request: UpdateProductVariantRequest,
        organizationId: UUID,
    ): ProductVariant {
        val v = getVariant(id, organizationId)
        return variantRepository.save(
            v.copy(
                name = request.name?.trim() ?: v.name,
                skuSuffix = request.skuSuffix ?: v.skuSuffix,
                attributes = request.attributes ?: v.attributes,
                isActive = request.isActive ?: v.isActive,
            ),
        )
    }

    @Transactional
    fun deactivateVariant(
        id: UUID,
        organizationId: UUID,
    ): ProductVariant {
        val v = getVariant(id, organizationId)
        if (!v.isActive) throw BusinessRuleException("Variant '${v.code}' is already inactive")
        return variantRepository.save(v.copy(isActive = false))
    }
}
