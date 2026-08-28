package com.aquinofroilan.tessera.domain.organization.service

import com.aquinofroilan.tessera.domain.organization.dto.BatchSetFeatureOverridesRequest
import com.aquinofroilan.tessera.domain.organization.dto.EffectiveFeaturesResponse
import com.aquinofroilan.tessera.domain.organization.dto.FeatureFlagDetailDto
import com.aquinofroilan.tessera.domain.organization.dto.OrganizationPlanResponse
import com.aquinofroilan.tessera.domain.organization.model.BillingPlan
import com.aquinofroilan.tessera.domain.organization.model.FeatureFlag
import com.aquinofroilan.tessera.domain.organization.model.OrganizationFeatureFlag
import com.aquinofroilan.tessera.domain.organization.repository.OrganizationFeatureFlagRepository
import com.aquinofroilan.tessera.domain.organization.repository.OrganizationRepository
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import com.github.benmanes.caffeine.cache.Caffeine
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID
import java.util.concurrent.TimeUnit

@Service
class OrganizationBillingFeatureService(
    private val organizationRepository: OrganizationRepository,
    private val featureFlagRepository: OrganizationFeatureFlagRepository,
) {
    private val log = LoggerFactory.getLogger(OrganizationBillingFeatureService::class.java)

    private val effectiveFeatureCache =
        Caffeine
            .newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .maximumSize(10_000)
            .build<UUID, Map<String, Boolean>>()

    companion object {
        fun getDefaultPlanFeatures(plan: BillingPlan): Map<String, Boolean> {
            val defaults =
                FeatureFlag.entries
                    .filter { it != FeatureFlag.CUSTOM }
                    .associate { it.name to false }
                    .toMutableMap()

            when (plan) {
                BillingPlan.FREE -> {
                    // All gated features false by default
                }
                BillingPlan.STARTER -> {
                    defaults[FeatureFlag.API_KEYS.name] = true
                    defaults[FeatureFlag.MULTI_CURRENCY.name] = true
                    defaults[FeatureFlag.CUSTOM_ROLES.name] = true
                }
                BillingPlan.ENTERPRISE -> {
                    defaults.keys.forEach { defaults[it] = true }
                }
            }
            return defaults
        }
    }

    @Transactional(readOnly = true)
    fun getPlan(organizationId: UUID): OrganizationPlanResponse {
        val org =
            organizationRepository.findById(organizationId).orElseThrow {
                ResourceNotFoundException("Organization $organizationId not found")
            }

        val overrides =
            featureFlagRepository
                .findByOrganizationId(organizationId)
                .associate { it.featureKey to it.enabled }

        val effective = computeEffectiveFeatures(org.billingPlan, overrides)
        effectiveFeatureCache.put(organizationId, effective)

        return OrganizationPlanResponse(
            organizationId = org.uuid,
            orgSlug = org.orgSlug,
            name = org.name,
            billingPlan = org.billingPlan,
            effectiveFeatures = effective,
            featureOverrides = overrides,
        )
    }

    @Transactional
    fun updatePlan(
        organizationId: UUID,
        newPlan: BillingPlan,
    ): OrganizationPlanResponse {
        val org =
            organizationRepository.findById(organizationId).orElseThrow {
                ResourceNotFoundException("Organization $organizationId not found")
            }

        log.info("Updating organization {} billing plan from {} to {}", org.uuid, org.billingPlan, newPlan)
        org.billingPlan = newPlan
        organizationRepository.save(org)

        effectiveFeatureCache.invalidate(organizationId)
        return getPlan(organizationId)
    }

    fun isFeatureEnabled(
        organizationId: UUID,
        featureKey: String,
    ): Boolean {
        val effective =
            effectiveFeatureCache.get(organizationId) { id ->
                val org = organizationRepository.findById(id).orElse(null) ?: return@get emptyMap()
                val overrides =
                    featureFlagRepository
                        .findByOrganizationId(id)
                        .associate { it.featureKey to it.enabled }
                computeEffectiveFeatures(org.billingPlan, overrides)
            } ?: emptyMap()

        return effective[featureKey] ?: false
    }

    @Transactional(readOnly = true)
    fun getEffectiveFeatures(organizationId: UUID): EffectiveFeaturesResponse {
        val org =
            organizationRepository.findById(organizationId).orElseThrow {
                ResourceNotFoundException("Organization $organizationId not found")
            }

        val planDefaults = getDefaultPlanFeatures(org.billingPlan)
        val overrides =
            featureFlagRepository
                .findByOrganizationId(organizationId)
                .associate { it.featureKey to it.enabled }

        val allKeys = (planDefaults.keys + overrides.keys).distinct().sorted()

        val featureDetails =
            allKeys.map { key ->
                val defaultVal = planDefaults[key] ?: false
                val overrideVal = overrides[key]
                FeatureFlagDetailDto(
                    featureKey = key,
                    enabled = overrideVal ?: defaultVal,
                    isOverride = overrideVal != null,
                    planDefault = defaultVal,
                )
            }

        return EffectiveFeaturesResponse(
            organizationId = org.uuid,
            billingPlan = org.billingPlan,
            features = featureDetails,
        )
    }

    @Transactional
    fun setFeatureOverride(
        organizationId: UUID,
        featureKey: String,
        enabled: Boolean,
    ): FeatureFlagDetailDto {
        val org =
            organizationRepository.findById(organizationId).orElseThrow {
                ResourceNotFoundException("Organization $organizationId not found")
            }

        val existingOpt = featureFlagRepository.findByOrganizationIdAndFeatureKey(organizationId, featureKey)
        val flag =
            if (existingOpt.isPresent) {
                val existing = existingOpt.get()
                existing.enabled = enabled
                existing.updatedAt = LocalDateTime.now(ZoneOffset.UTC)
                existing
            } else {
                OrganizationFeatureFlag(
                    organizationId = organizationId,
                    featureKey = featureKey,
                    enabled = enabled,
                )
            }

        featureFlagRepository.save(flag)
        effectiveFeatureCache.invalidate(organizationId)

        val planDefault = getDefaultPlanFeatures(org.billingPlan)[featureKey] ?: false
        return FeatureFlagDetailDto(
            featureKey = featureKey,
            enabled = enabled,
            isOverride = true,
            planDefault = planDefault,
        )
    }

    @Transactional
    fun removeFeatureOverride(
        organizationId: UUID,
        featureKey: String,
    ): FeatureFlagDetailDto {
        val org =
            organizationRepository.findById(organizationId).orElseThrow {
                ResourceNotFoundException("Organization $organizationId not found")
            }

        featureFlagRepository.deleteByOrganizationIdAndFeatureKey(organizationId, featureKey)
        effectiveFeatureCache.invalidate(organizationId)

        val planDefault = getDefaultPlanFeatures(org.billingPlan)[featureKey] ?: false
        return FeatureFlagDetailDto(
            featureKey = featureKey,
            enabled = planDefault,
            isOverride = false,
            planDefault = planDefault,
        )
    }

    @Transactional
    fun batchSetOverrides(
        organizationId: UUID,
        request: BatchSetFeatureOverridesRequest,
    ): EffectiveFeaturesResponse {
        val org =
            organizationRepository.findById(organizationId).orElseThrow {
                ResourceNotFoundException("Organization $organizationId not found")
            }

        val existingFlags =
            featureFlagRepository
                .findByOrganizationId(organizationId)
                .associateBy { it.featureKey }

        val toSave = mutableListOf<OrganizationFeatureFlag>()
        request.overrides.forEach { (key, enabled) ->
            val existing = existingFlags[key]
            if (existing != null) {
                existing.enabled = enabled
                existing.updatedAt = LocalDateTime.now(ZoneOffset.UTC)
                toSave.add(existing)
            } else {
                toSave.add(
                    OrganizationFeatureFlag(
                        organizationId = organizationId,
                        featureKey = key,
                        enabled = enabled,
                    ),
                )
            }
        }

        featureFlagRepository.saveAll(toSave)
        effectiveFeatureCache.invalidate(organizationId)

        return getEffectiveFeatures(organizationId)
    }

    private fun computeEffectiveFeatures(
        plan: BillingPlan,
        overrides: Map<String, Boolean>,
    ): Map<String, Boolean> {
        val defaults = getDefaultPlanFeatures(plan).toMutableMap()
        overrides.forEach { (k, v) -> defaults[k] = v }
        return defaults
    }
}
