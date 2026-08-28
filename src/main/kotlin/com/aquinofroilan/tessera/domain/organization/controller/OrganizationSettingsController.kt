package com.aquinofroilan.tessera.domain.organization.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.domain.organization.dto.OrganizationSettingsResponse
import com.aquinofroilan.tessera.domain.organization.dto.UpdateOrganizationSettingsRequest
import com.aquinofroilan.tessera.domain.organization.service.OrganizationSettingsService
import com.aquinofroilan.tessera.security.CurrentOrganizationId
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/organization/settings")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class OrganizationSettingsController(
    private val settingsService: OrganizationSettingsService,
) {
    @GetMapping
    @PreAuthorize("hasAuthority('organization:read')")
    fun getSettings(
        @CurrentOrganizationId orgId: UUID,
    ): ResponseEntity<OrganizationSettingsResponse> = ResponseEntity.ok(settingsService.getSettings(orgId))

    @PutMapping
    @PreAuthorize("hasAuthority('organization:write')")
    fun updateSettingsPut(
        @CurrentOrganizationId orgId: UUID,
        @Valid @RequestBody request: UpdateOrganizationSettingsRequest,
    ): ResponseEntity<OrganizationSettingsResponse> = ResponseEntity.ok(settingsService.updateSettings(orgId, request))

    @PatchMapping
    @PreAuthorize("hasAuthority('organization:write')")
    fun updateSettingsPatch(
        @CurrentOrganizationId orgId: UUID,
        @Valid @RequestBody request: UpdateOrganizationSettingsRequest,
    ): ResponseEntity<OrganizationSettingsResponse> = ResponseEntity.ok(settingsService.updateSettings(orgId, request))
}
