package com.aquinofroilan.tessera.domain.inventory.service

import com.aquinofroilan.tessera.domain.inventory.dto.CreateProductCategoryRequest
import com.aquinofroilan.tessera.domain.inventory.dto.UpdateProductCategoryRequest
import com.aquinofroilan.tessera.domain.inventory.model.ProductCategory
import com.aquinofroilan.tessera.domain.inventory.repository.ProductCategoryRepository
import com.aquinofroilan.tessera.domain.organization.model.AuditAction
import com.aquinofroilan.tessera.domain.organization.model.AuditCategory
import com.aquinofroilan.tessera.domain.organization.service.OrganizationAuditService
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ProductCategoryService(
    private val repository: ProductCategoryRepository,
    private val auditService: OrganizationAuditService,
) {
    @Transactional
    fun create(
        request: CreateProductCategoryRequest,
        organizationId: UUID,
    ): ProductCategory {
        val name = request.name.trim()
        ensureUnique(name, organizationId)
        val category =
            repository.save(
                ProductCategory(organizationId = organizationId, name = name, description = request.description),
            )
        auditService.logEvent(
            organizationId = organizationId,
            action = AuditAction.PRODUCT_CATEGORY_CREATED.name,
            category = AuditCategory.INVENTORY,
            entityType = "PRODUCT_CATEGORY",
            entityId = category.id.toString(),
            newValue = category.toAuditSnapshot(),
        )
        return category
    }

    fun list(
        organizationId: UUID,
        activeOnly: Boolean,
    ): List<ProductCategory> =
        if (activeOnly) {
            repository.findByOrganizationIdAndIsActiveOrderByNameAscIdAsc(organizationId, true)
        } else {
            repository.findByOrganizationIdOrderByNameAscIdAsc(organizationId)
        }

    fun list(
        organizationId: UUID,
        activeOnly: Boolean,
        pageable: Pageable,
    ): Page<ProductCategory> =
        if (activeOnly) {
            repository.findByOrganizationIdAndIsActive(organizationId, true, pageable)
        } else {
            repository.findByOrganizationId(organizationId, pageable)
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
        val oldValue = category.toAuditSnapshot()
        val name = request.name?.trim() ?: category.name
        if (name != category.name) ensureUnique(name, organizationId)
        category.name = name
        category.description = request.description ?: category.description
        category.isActive = request.isActive ?: category.isActive
        val updated = repository.save(category)
        auditService.logEvent(
            organizationId = organizationId,
            action = AuditAction.PRODUCT_CATEGORY_UPDATED.name,
            category = AuditCategory.INVENTORY,
            entityType = "PRODUCT_CATEGORY",
            entityId = updated.id.toString(),
            oldValue = oldValue,
            newValue = updated.toAuditSnapshot(),
        )
        return updated
    }

    @Transactional
    fun deactivate(
        id: UUID,
        organizationId: UUID,
    ): ProductCategory {
        val category = get(id, organizationId)
        if (!category.isActive) throw BusinessRuleException("Product category '${category.name}' is already inactive")
        val oldValue = category.toAuditSnapshot()
        category.isActive = false
        val deactivated = repository.save(category)
        auditService.logEvent(
            organizationId = organizationId,
            action = AuditAction.PRODUCT_CATEGORY_DEACTIVATED.name,
            category = AuditCategory.INVENTORY,
            entityType = "PRODUCT_CATEGORY",
            entityId = deactivated.id.toString(),
            oldValue = oldValue,
            newValue = deactivated.toAuditSnapshot(),
        )
        return deactivated
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

    private fun ProductCategory.toAuditSnapshot() =
        mapOf(
            "id" to id,
            "name" to name,
            "description" to description,
            "isActive" to isActive,
        )
}
