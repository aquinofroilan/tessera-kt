package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.CreateFixedAssetRequest
import com.aquinofroilan.tessera.dto.UpdateFixedAssetRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import com.aquinofroilan.tessera.model.AssetStatus
import com.aquinofroilan.tessera.model.FixedAsset
import com.aquinofroilan.tessera.repository.FixedAssetRepository
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class FixedAssetService(
    private val fixedAssetRepository: FixedAssetRepository,
    private val assetCategoryService: AssetCategoryService,
) {
    @Transactional
    fun createAsset(
        request: CreateFixedAssetRequest,
        organizationId: UUID,
    ): FixedAsset {
        val acquisitionDate = request.acquisitionDate ?: throw BusinessRuleException("Acquisition date is required")
        val acquisitionCost = request.acquisitionCost ?: throw BusinessRuleException("Acquisition cost is required")
        val usefulLifeMonths = request.usefulLifeMonths ?: throw BusinessRuleException("Useful life is required")
        if (request.salvageValue > acquisitionCost) {
            throw BusinessRuleException("Salvage value cannot exceed acquisition cost")
        }
        request.categoryId?.let { assetCategoryService.getCategory(it, organizationId) }

        return saveWithRetry(organizationId) { number ->
            FixedAsset(
                assetNumber = number,
                organizationId = organizationId,
                name = request.name.trim(),
                description = request.description?.trim()?.takeIf { it.isNotEmpty() },
                categoryId = request.categoryId,
                acquisitionDate = acquisitionDate,
                acquisitionCost = acquisitionCost,
                salvageValue = request.salvageValue,
                usefulLifeMonths = usefulLifeMonths,
                depreciationMethod = request.depreciationMethod,
                location = request.location?.trim()?.takeIf { it.isNotEmpty() },
                serialNumber = request.serialNumber?.trim()?.takeIf { it.isNotEmpty() },
                assetAccountId = request.assetAccountId,
                accumulatedDepreciationAccountId = request.accumulatedDepreciationAccountId,
                depreciationExpenseAccountId = request.depreciationExpenseAccountId,
            )
        }
    }

    fun listAssets(
        organizationId: UUID,
        status: AssetStatus? = null,
        categoryId: String? = null,
    ): List<FixedAsset> =
        when {
            status != null -> fixedAssetRepository.findByOrganizationIdAndStatus(organizationId, status)
            categoryId != null -> fixedAssetRepository.findByOrganizationIdAndCategoryId(organizationId, categoryId)
            else -> fixedAssetRepository.findByOrganizationId(organizationId)
        }

    fun getAsset(
        id: String,
        organizationId: UUID,
    ): FixedAsset {
        val asset =
            fixedAssetRepository.findById(id).orElseThrow {
                ResourceNotFoundException("Fixed asset $id not found")
            }
        if (asset.organizationId != organizationId) {
            throw ResourceNotFoundException("Fixed asset $id not found")
        }
        return asset
    }

    @Transactional
    fun updateAsset(
        id: String,
        request: UpdateFixedAssetRequest,
        organizationId: UUID,
    ): FixedAsset {
        val existing = getAsset(id, organizationId)
        request.categoryId?.let { assetCategoryService.getCategory(it, organizationId) }
        return fixedAssetRepository.save(
            existing.copy(
                name = request.name?.trim()?.takeIf { it.isNotEmpty() } ?: existing.name,
                description =
                    when (val d = request.description) {
                        null -> existing.description
                        else -> d.trim().takeIf { it.isNotEmpty() }
                    },
                categoryId = request.categoryId ?: existing.categoryId,
                location =
                    when (val l = request.location) {
                        null -> existing.location
                        else -> l.trim().takeIf { it.isNotEmpty() }
                    },
                serialNumber =
                    when (val s = request.serialNumber) {
                        null -> existing.serialNumber
                        else -> s.trim().takeIf { it.isNotEmpty() }
                    },
                assetAccountId = request.assetAccountId ?: existing.assetAccountId,
                accumulatedDepreciationAccountId =
                    request.accumulatedDepreciationAccountId ?: existing.accumulatedDepreciationAccountId,
                depreciationExpenseAccountId =
                    request.depreciationExpenseAccountId ?: existing.depreciationExpenseAccountId,
            ),
        )
    }

    private fun saveWithRetry(
        organizationId: String,
        build: (assetNumber: String) -> FixedAsset,
    ): FixedAsset {
        var attempts = 0
        while (attempts < MAX_NUMBER_RETRIES) {
            val nextNumber = nextAssetNumber(organizationId)
            try {
                return fixedAssetRepository.save(build(nextNumber))
            } catch (_: DuplicateKeyException) {
                attempts++
            }
        }
        throw BusinessRuleException("Couldn't allocate an asset number after $MAX_NUMBER_RETRIES attempts")
    }

    private fun nextAssetNumber(organizationId: String): String {
        val count = fixedAssetRepository.countByOrganizationId(organizationId)
        return "FA-%05d".format(count + 1)
    }

    companion object {
        private const val MAX_NUMBER_RETRIES = 5
    }
}
