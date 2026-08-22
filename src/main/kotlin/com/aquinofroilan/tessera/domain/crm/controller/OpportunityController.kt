package com.aquinofroilan.tessera.domain.crm.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.domain.crm.dto.CloseOpportunityRequest
import com.aquinofroilan.tessera.domain.crm.dto.CreateOpportunityRequest
import com.aquinofroilan.tessera.domain.crm.dto.OpportunityResponse
import com.aquinofroilan.tessera.domain.crm.dto.UpdateOpportunityRequest
import com.aquinofroilan.tessera.domain.crm.model.OpportunityStatus
import com.aquinofroilan.tessera.domain.crm.service.OpportunityService
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.security.CurrentOrganizationId
import com.aquinofroilan.tessera.security.CurrentUserId
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.Locale
import java.util.UUID

@RestController
@RequestMapping("/api/v1/crm/opportunities")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class OpportunityController(
    private val opportunityService: OpportunityService,
    private val authContext: AuthenticationContext,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('crm:write')")
    fun create(
        @CurrentUserId userId: UUID,
        @CurrentOrganizationId orgId: UUID,
        @Valid @RequestBody request: CreateOpportunityRequest,
    ): ResponseEntity<Any> {
        val opp = opportunityService.createOpportunity(request, orgId, userId)
        return ResponseEntity.status(HttpStatus.CREATED).body(OpportunityResponse.from(opp))
    }

    @GetMapping
    @PreAuthorize("hasAuthority('crm:read')")
    fun list(
        @CurrentOrganizationId orgId: UUID,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) customerId: UUID?,
        @RequestParam(required = false) stageId: UUID?,
        @RequestParam(required = false) ownerUserId: UUID?,
    ): ResponseEntity<Any> {
        val parsed =
            if (status != null) {
                try {
                    OpportunityStatus.valueOf(status.uppercase(Locale.ROOT))
                } catch (e: IllegalArgumentException) {
                    return ResponseEntity.badRequest().body(mapOf("error" to "Invalid status '$status'"))
                }
            } else {
                null
            }
        return ResponseEntity.ok(
            opportunityService
                .listOpportunities(orgId, parsed, customerId, stageId, ownerUserId)
                .map { OpportunityResponse.from(it) },
        )
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('crm:read')")
    fun get(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: UUID,
    ): ResponseEntity<Any> = ResponseEntity.ok(OpportunityResponse.from(opportunityService.getOpportunity(id, orgId)))

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('crm:write')")
    fun update(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateOpportunityRequest,
    ): ResponseEntity<Any> = ResponseEntity.ok(OpportunityResponse.from(opportunityService.updateOpportunity(id, request, orgId)))

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAuthority('crm:write')")
    fun close(
        @CurrentUserId userId: UUID,
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: UUID,
        @Valid @RequestBody request: CloseOpportunityRequest,
    ): ResponseEntity<Any> = ResponseEntity.ok(OpportunityResponse.from(opportunityService.closeOpportunity(id, request, orgId, userId)))

    @PostMapping("/{id}/abandon")
    @PreAuthorize("hasAuthority('crm:write')")
    fun abandon(
        @CurrentUserId userId: UUID,
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: UUID,
    ): ResponseEntity<Any> = ResponseEntity.ok(OpportunityResponse.from(opportunityService.abandonOpportunity(id, orgId, userId)))
}
