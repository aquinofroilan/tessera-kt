package com.aquinofroilan.tessera.domain.organization.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.domain.organization.dto.BatchSetFeatureOverridesRequest
import com.aquinofroilan.tessera.domain.organization.dto.EffectiveFeaturesResponse
import com.aquinofroilan.tessera.domain.organization.dto.FeatureFlagDetailDto
import com.aquinofroilan.tessera.domain.organization.dto.OrganizationPlanResponse
import com.aquinofroilan.tessera.domain.organization.dto.SetFeatureOverrideRequest
import com.aquinofroilan.tessera.domain.organization.dto.UpdateBillingPlanRequest
import com.aquinofroilan.tessera.domain.organization.service.OrganizationBillingFeatureService
import com.aquinofroilan.tessera.security.CurrentOrganizationId
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/organization/billing")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class OrganizationBillingFeatureController(
    private val billingFeatureService: OrganizationBillingFeatureService,
) {
    @GetMapping("/plan")
    @PreAuthorize("hasAuthority('organization:read') or hasRole('SUPER_ADMIN')")
    fun getPlan(
        @CurrentOrganizationId orgId: UUID,
    ): ResponseEntity<OrganizationPlanResponse> = ResponseEntity.ok(billingFeatureService.getPlan(orgId))

    @PutMapping("/plan")
    @PreAuthorize("hasAuthority('organization:write') or hasRole('SUPER_ADMIN')")
    fun updatePlan(
        @CurrentOrganizationId orgId: UUID,
        @Valid @RequestBody request: UpdateBillingPlanRequest,
    ): ResponseEntity<OrganizationPlanResponse> = ResponseEntity.ok(billingFeatureService.updatePlan(orgId, request.plan))

    @GetMapping("/features")
    @PreAuthorize("hasAuthority('organization:read') or hasRole('SUPER_ADMIN')")
    fun getEffectiveFeatures(
        @CurrentOrganizationId orgId: UUID,
    ): ResponseEntity<EffectiveFeaturesResponse> = ResponseEntity.ok(billingFeatureService.getEffectiveFeatures(orgId))

    @PutMapping("/features/{featureKey}")
    @PreAuthorize("hasAuthority('organization:write') or hasRole('SUPER_ADMIN')")
    fun setFeatureOverride(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable featureKey: String,
        @RequestParam enabled: Boolean,
    ): ResponseEntity<FeatureFlagDetailDto> = ResponseEntity.ok(billingFeatureService.setFeatureOverride(orgId, featureKey, enabled))

    @PostMapping("/features/override")
    @PreAuthorize("hasAuthority('organization:write') or hasRole('SUPER_ADMIN')")
    fun setFeatureOverrideWithBody(
        @CurrentOrganizationId orgId: UUID,
        @Valid @RequestBody request: SetFeatureOverrideRequest,
    ): ResponseEntity<FeatureFlagDetailDto> =
        ResponseEntity.ok(billingFeatureService.setFeatureOverride(orgId, request.featureKey, request.enabled))

    @DeleteMapping("/features/{featureKey}")
    @PreAuthorize("hasAuthority('organization:write') or hasRole('SUPER_ADMIN')")
    fun removeFeatureOverride(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable featureKey: String,
    ): ResponseEntity<FeatureFlagDetailDto> = ResponseEntity.ok(billingFeatureService.removeFeatureOverride(orgId, featureKey))

    @PostMapping("/features/batch")
    @PreAuthorize("hasAuthority('organization:write') or hasRole('SUPER_ADMIN')")
    fun batchSetOverrides(
        @CurrentOrganizationId orgId: UUID,
        @Valid @RequestBody request: BatchSetFeatureOverridesRequest,
    ): ResponseEntity<EffectiveFeaturesResponse> = ResponseEntity.ok(billingFeatureService.batchSetOverrides(orgId, request))
}
