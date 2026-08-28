package com.aquinofroilan.tessera.domain.organization.dto

import com.aquinofroilan.tessera.domain.organization.model.BillingPlan
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import java.util.UUID

data class OrganizationPlanResponse(
    val organizationId: UUID,
    val orgSlug: String,
    val name: String,
    val billingPlan: BillingPlan,
    val effectiveFeatures: Map<String, Boolean>,
    val featureOverrides: Map<String, Boolean>,
)

data class UpdateBillingPlanRequest(
    @field:NotNull(message = "Billing plan is required")
    val plan: BillingPlan,
)

data class FeatureFlagDetailDto(
    val featureKey: String,
    val enabled: Boolean,
    val isOverride: Boolean,
    val planDefault: Boolean,
)

data class EffectiveFeaturesResponse(
    val organizationId: UUID,
    val billingPlan: BillingPlan,
    val features: List<FeatureFlagDetailDto>,
)

data class SetFeatureOverrideRequest(
    @field:NotBlank(message = "Feature key is required")
    val featureKey: String,
    @field:NotNull(message = "Enabled flag is required")
    val enabled: Boolean,
)

data class BatchSetFeatureOverridesRequest(
    @field:NotEmpty(message = "Overrides map cannot be empty")
    val overrides: Map<String, Boolean>,
)
