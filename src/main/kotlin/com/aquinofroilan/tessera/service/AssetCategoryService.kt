package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.CreateAssetCategoryRequest
import com.aquinofroilan.tessera.dto.UpdateAssetCategoryRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import com.aquinofroilan.tessera.model.AssetCategory
import com.aquinofroilan.tessera.repository.AssetCategoryRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AssetCategoryService(
    private val assetCategoryRepository: AssetCategoryRepository,
) {
    @Transactional
    fun createCategory(
        request: CreateAssetCategoryRequest,
        organizationId: String,
    ): AssetCategory {
        val code = request.code.trim()
        if (assetCategoryRepository.findByOrganizationIdAndCode(organizationId, code).isPresent) {
            throw BusinessRuleException("Asset category code '$code' already exists in this organization")
        }
        return assetCategoryRepository.save(
            AssetCategory(
                organizationId = organizationId,
                code = code,
                name = request.name.trim(),
                description = request.description?.trim()?.takeIf { it.isNotEmpty() },
                defaultUsefulLifeMonths = request.defaultUsefulLifeMonths,
                defaultDepreciationMethod = request.defaultDepreciationMethod,
                defaultSalvageValue = request.defaultSalvageValue,
            ),
        )
    }

    fun listCategories(
        organizationId: String,
        activeOnly: Boolean = false,
    ): List<AssetCategory> =
        if (activeOnly) {
            assetCategoryRepository.findByOrganizationIdAndIsActiveTrue(organizationId)
        } else {
            assetCategoryRepository.findByOrganizationId(organizationId)
        }

    fun getCategory(
        id: String,
        organizationId: String,
    ): AssetCategory {
        val category =
            assetCategoryRepository.findById(id).orElseThrow {
                ResourceNotFoundException("Asset category $id not found")
            }
        if (category.organizationId != organizationId) {
            throw ResourceNotFoundException("Asset category $id not found")
        }
        return category
    }

    @Transactional
    fun updateCategory(
        id: String,
        request: UpdateAssetCategoryRequest,
        organizationId: String,
    ): AssetCategory {
        val existing = getCategory(id, organizationId)
        return assetCategoryRepository.save(
            existing.copy(
                name = request.name?.trim()?.takeIf { it.isNotEmpty() } ?: existing.name,
                description =
                    when (val d = request.description) {
                        null -> existing.description
                        else -> d.trim().takeIf { it.isNotEmpty() }
                    },
                defaultUsefulLifeMonths = request.defaultUsefulLifeMonths ?: existing.defaultUsefulLifeMonths,
                defaultDepreciationMethod = request.defaultDepreciationMethod ?: existing.defaultDepreciationMethod,
                defaultSalvageValue = request.defaultSalvageValue ?: existing.defaultSalvageValue,
                isActive = request.isActive ?: existing.isActive,
            ),
        )
    }
}
