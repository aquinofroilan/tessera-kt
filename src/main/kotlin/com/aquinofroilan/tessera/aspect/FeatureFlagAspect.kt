package com.aquinofroilan.tessera.aspect

import com.aquinofroilan.tessera.annotation.RequiresFeature
import com.aquinofroilan.tessera.domain.organization.service.OrganizationBillingFeatureService
import com.aquinofroilan.tessera.exception.FeatureNotEnabledException
import com.aquinofroilan.tessera.security.AuthenticationContext
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component

@Aspect
@Component
class FeatureFlagAspect(
    private val billingFeatureService: OrganizationBillingFeatureService,
    private val authContext: AuthenticationContext,
) {
    @Before("@annotation(requiresFeature) || @within(requiresFeature)")
    fun checkFeature(requiresFeature: RequiresFeature) {
        val auth = SecurityContextHolder.getContext().authentication
        val isSuperAdmin =
            auth?.authorities?.any {
                it.authority == "ROLE_SUPER_ADMIN"
            } ?: false

        if (isSuperAdmin) {
            return
        }

        val orgId = authContext.organizationId() ?: return
        val featureKey =
            if (requiresFeature.key.isNotBlank()) {
                requiresFeature.key
            } else {
                requiresFeature.value.name
            }

        if (!billingFeatureService.isFeatureEnabled(orgId, featureKey)) {
            throw FeatureNotEnabledException("Feature '$featureKey' is not enabled for your organization's billing plan")
        }
    }
}
