package com.aquinofroilan.tessera.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.dto.CloseOpportunityRequest
import com.aquinofroilan.tessera.dto.CreateOpportunityRequest
import com.aquinofroilan.tessera.dto.OpportunityResponse
import com.aquinofroilan.tessera.dto.UpdateOpportunityRequest
import com.aquinofroilan.tessera.model.OpportunityStatus
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.service.OpportunityService
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

@RestController
@RequestMapping("/crm/opportunities")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class OpportunityController(
    private val opportunityService: OpportunityService,
    private val authContext: AuthenticationContext,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('crm:write')")
    fun create(
        @Valid @RequestBody request: CreateOpportunityRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val userId = authContext.userId() ?: "api-key"
        val opp = opportunityService.createOpportunity(request, orgId, userId)
        return ResponseEntity.status(HttpStatus.CREATED).body(OpportunityResponse.from(opp))
    }

    @GetMapping
    @PreAuthorize("hasAuthority('crm:read')")
    fun list(
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) customerId: String?,
        @RequestParam(required = false) stageId: String?,
        @RequestParam(required = false) ownerUserId: String?,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
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
        @PathVariable id: String,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(OpportunityResponse.from(opportunityService.getOpportunity(id, orgId)))
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('crm:write')")
    fun update(
        @PathVariable id: String,
        @Valid @RequestBody request: UpdateOpportunityRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(OpportunityResponse.from(opportunityService.updateOpportunity(id, request, orgId)))
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAuthority('crm:write')")
    fun close(
        @PathVariable id: String,
        @Valid @RequestBody request: CloseOpportunityRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val userId = authContext.userId() ?: "api-key"
        return ResponseEntity.ok(OpportunityResponse.from(opportunityService.closeOpportunity(id, request, orgId, userId)))
    }

    @PostMapping("/{id}/abandon")
    @PreAuthorize("hasAuthority('crm:write')")
    fun abandon(
        @PathVariable id: String,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val userId = authContext.userId() ?: "api-key"
        return ResponseEntity.ok(OpportunityResponse.from(opportunityService.abandonOpportunity(id, orgId, userId)))
    }
}
