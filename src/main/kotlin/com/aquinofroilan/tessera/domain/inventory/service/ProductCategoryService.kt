package com.aquinofroilan.tessera.domain.inventory.service

import com.aquinofroilan.tessera.domain.inventory.dto.CreateProductCategoryRequest
import com.aquinofroilan.tessera.domain.inventory.dto.UpdateProductCategoryRequest
import com.aquinofroilan.tessera.domain.inventory.model.ProductCategory
import com.aquinofroilan.tessera.domain.inventory.repository.ProductCategoryRepository
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ProductCategoryService(
    private val repository: ProductCategoryRepository,
) {
    @Transactional
    fun create(
        request: CreateProductCategoryRequest,
        organizationId: UUID,
    ): ProductCategory {
        val name = request.name.trim()
        ensureUnique(name, organizationId)
        return repository.save(ProductCategory(organizationId = organizationId, name = name, description = request.description))
    }

    fun list(
        organizationId: UUID,
        activeOnly: Boolean,
    ): List<ProductCategory> =
        if (activeOnly) {
            repository.findByOrganizationIdAndIsActive(organizationId, true)
        } else {
            repository.findByOrganizationId(organizationId)
        }

    fun get(
        id: UUID,
        organizationId: UUID,
    ): ProductCategory =
        repository
            .findById(id)
            .orElseThrow {
                ResourceNotFoundException("Product category not found")
            }.also {
                if (it.organizationId != organizationId) throw ResourceNotFoundException("Product category not found")
            }

    @Transactional
    fun update(
        id: UUID,
        request: UpdateProductCategoryRequest,
        organizationId: UUID,
    ): ProductCategory {
        val category = get(id, organizationId)
        val name = request.name?.trim() ?: category.name
        if (name != category.name) ensureUnique(name, organizationId)
        category.name = name
        category.description = request.description ?: category.description
        category.isActive = request.isActive ?: category.isActive
        return repository.save(category)
    }

    @Transactional
    fun deactivate(
        id: UUID,
        organizationId: UUID,
    ): ProductCategory {
        val category = get(id, organizationId)
        if (!category.isActive) throw BusinessRuleException("Product category '${category.name}' is already inactive")
        category.isActive = false
        return repository.save(category)
    }

    private fun ensureUnique(
        name: String,
        organizationId: UUID,
    ) {
        if (name.isBlank()) throw BusinessRuleException("Product category name cannot be blank")
        if (repository.findByOrganizationIdAndNameIgnoreCase(organizationId, name) != null) {
            throw BusinessRuleException("Product category '$name' already exists in this organization")
        }
    }
}
