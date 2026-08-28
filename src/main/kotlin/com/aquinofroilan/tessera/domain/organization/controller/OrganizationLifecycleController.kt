package com.aquinofroilan.tessera.domain.organization.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.domain.organization.dto.OrganizationStatusResponse
import com.aquinofroilan.tessera.domain.organization.dto.TransitionOrganizationStatusRequest
import com.aquinofroilan.tessera.domain.organization.model.OrganizationStatus
import com.aquinofroilan.tessera.domain.organization.service.OrganizationLifecycleService
import com.aquinofroilan.tessera.security.CurrentOrganizationId
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/organization/status")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class OrganizationLifecycleController(
    private val lifecycleService: OrganizationLifecycleService,
) {
    @GetMapping
    @PreAuthorize("hasAuthority('organization:read') or hasRole('SUPER_ADMIN')")
    fun getStatus(
        @CurrentOrganizationId orgId: UUID,
    ): ResponseEntity<OrganizationStatusResponse> = ResponseEntity.ok(lifecycleService.getStatus(orgId))

    @PostMapping
    @PreAuthorize("hasAuthority('organization:write') or hasRole('SUPER_ADMIN')")
    fun transitionStatus(
        @CurrentOrganizationId orgId: UUID,
        @Valid @RequestBody request: TransitionOrganizationStatusRequest,
    ): ResponseEntity<OrganizationStatusResponse> =
        ResponseEntity.ok(lifecycleService.transitionStatus(orgId, request.targetStatus, request.reason))

    @PostMapping("/transition")
    @PreAuthorize("hasAuthority('organization:write') or hasRole('SUPER_ADMIN')")
    fun transitionStatusExplicit(
        @CurrentOrganizationId orgId: UUID,
        @Valid @RequestBody request: TransitionOrganizationStatusRequest,
    ): ResponseEntity<OrganizationStatusResponse> =
        ResponseEntity.ok(lifecycleService.transitionStatus(orgId, request.targetStatus, request.reason))

    @PostMapping("/suspend")
    @PreAuthorize("hasAuthority('organization:write') or hasRole('SUPER_ADMIN')")
    fun suspendOrganization(
        @CurrentOrganizationId orgId: UUID,
        @RequestParam(required = false) reason: String?,
    ): ResponseEntity<OrganizationStatusResponse> =
        ResponseEntity.ok(lifecycleService.transitionStatus(orgId, OrganizationStatus.SUSPENDED, reason))

    @PostMapping("/archive")
    @PreAuthorize("hasAuthority('organization:write') or hasRole('SUPER_ADMIN')")
    fun archiveOrganization(
        @CurrentOrganizationId orgId: UUID,
        @RequestParam(required = false) reason: String?,
    ): ResponseEntity<OrganizationStatusResponse> =
        ResponseEntity.ok(lifecycleService.transitionStatus(orgId, OrganizationStatus.ARCHIVED, reason))

    @PostMapping("/activate")
    @PreAuthorize("hasAuthority('organization:write') or hasRole('SUPER_ADMIN')")
    fun activateOrganization(
        @CurrentOrganizationId orgId: UUID,
        @RequestParam(required = false) reason: String?,
    ): ResponseEntity<OrganizationStatusResponse> =
        ResponseEntity.ok(lifecycleService.transitionStatus(orgId, OrganizationStatus.ACTIVE, reason))
}
