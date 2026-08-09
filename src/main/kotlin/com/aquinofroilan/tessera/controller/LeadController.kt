package com.aquinofroilan.tessera.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.dto.ConvertLeadRequest
import com.aquinofroilan.tessera.dto.CreateLeadRequest
import com.aquinofroilan.tessera.dto.LeadResponse
import com.aquinofroilan.tessera.dto.OpportunityResponse
import com.aquinofroilan.tessera.dto.UpdateLeadRequest
import com.aquinofroilan.tessera.model.LeadStatus
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.service.LeadService
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
@RequestMapping("/crm/leads")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class LeadController(
    private val leadService: LeadService,
    private val authContext: AuthenticationContext,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('crm:write')")
    fun create(
        @Valid @RequestBody request: CreateLeadRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val userId = authContext.userId() ?: java.util.UUID.fromString("00000000-0000-0000-0000-000000000000")
        val lead = leadService.createLead(request, orgId, userId)
        return ResponseEntity.status(HttpStatus.CREATED).body(LeadResponse.from(lead))
    }

    @GetMapping
    @PreAuthorize("hasAuthority('crm:read')")
    fun list(
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) ownerUserId: UUID?,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val parsed =
            if (status != null) {
                try {
                    LeadStatus.valueOf(status.uppercase(Locale.ROOT))
                } catch (e: IllegalArgumentException) {
                    return ResponseEntity.badRequest().body(mapOf("error" to "Invalid status '$status'"))
                }
            } else {
                null
            }
        return ResponseEntity.ok(leadService.listLeads(orgId, parsed, ownerUserId).map { LeadResponse.from(it) })
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('crm:read')")
    fun get(
        @PathVariable id: UUID,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(LeadResponse.from(leadService.getLead(id, orgId)))
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('crm:write')")
    fun update(
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateLeadRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(LeadResponse.from(leadService.updateLead(id, request, orgId)))
    }

    @PostMapping("/{id}/convert")
    @PreAuthorize("hasAuthority('crm:write')")
    fun convert(
        @PathVariable id: UUID,
        @Valid @RequestBody request: ConvertLeadRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val userId = authContext.userId() ?: java.util.UUID.fromString("00000000-0000-0000-0000-000000000000")
        val (lead, opportunity) = leadService.convertLead(id, request, orgId, userId)
        return ResponseEntity.ok(
            mapOf(
                "lead" to LeadResponse.from(lead),
                "opportunity" to OpportunityResponse.from(opportunity),
            ),
        )
    }

    @PostMapping("/{id}/disqualify")
    @PreAuthorize("hasAuthority('crm:write')")
    fun disqualify(
        @PathVariable id: UUID,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(LeadResponse.from(leadService.disqualifyLead(id, orgId)))
    }
}
