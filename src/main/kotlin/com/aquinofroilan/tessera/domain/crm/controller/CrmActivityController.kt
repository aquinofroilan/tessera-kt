package com.aquinofroilan.tessera.domain.crm.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.domain.crm.dto.ActivityResponse
import com.aquinofroilan.tessera.domain.crm.dto.CreateActivityRequest
import com.aquinofroilan.tessera.domain.crm.dto.UpdateActivityRequest
import com.aquinofroilan.tessera.domain.crm.model.CrmActivityType
import com.aquinofroilan.tessera.domain.crm.service.CrmActivityService
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.security.CurrentOrganizationId
import com.aquinofroilan.tessera.security.CurrentUserId
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
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
import java.util.Locale
import java.util.UUID

@RestController
@RequestMapping("/api/v1/crm/activities")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class CrmActivityController(
    private val activityService: CrmActivityService,
    private val authContext: AuthenticationContext,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('crm:write')")
    fun create(
        @CurrentUserId userId: UUID,
        @CurrentOrganizationId orgId: UUID,
        @Valid @RequestBody request: CreateActivityRequest,
    ): ResponseEntity<Any> {
        val activity = activityService.createActivity(request, orgId, userId)
        return ResponseEntity.status(HttpStatus.CREATED).body(ActivityResponse.from(activity))
    }

    @GetMapping
    @PreAuthorize("hasAuthority('crm:read')")
    fun list(
        @CurrentOrganizationId orgId: UUID,
        @RequestParam(required = false) type: String?,
        @RequestParam(required = false) leadId: java.util.UUID?,
        @RequestParam(required = false) opportunityId: java.util.UUID?,
        @RequestParam(required = false) contactId: java.util.UUID?,
        @RequestParam(required = false) customerId: java.util.UUID?,
    ): ResponseEntity<Any> {
        val parsed =
            if (type != null) {
                try {
                    CrmActivityType.valueOf(type.uppercase(Locale.ROOT))
                } catch (e: IllegalArgumentException) {
                    return ResponseEntity.badRequest().body(mapOf("error" to "Invalid type '$type'"))
                }
            } else {
                null
            }
        return ResponseEntity.ok(
            activityService
                .listActivities(orgId, parsed, leadId, opportunityId, contactId, customerId)
                .map { ActivityResponse.from(it) },
        )
    }

    @GetMapping("/my-tasks")
    @PreAuthorize("hasAuthority('crm:read')")
    fun listMyTasks(
        @CurrentUserId userId: UUID,
        @CurrentOrganizationId orgId: UUID,
    ): ResponseEntity<Any> = ResponseEntity.ok(activityService.listMyOpenTasks(orgId, userId).map { ActivityResponse.from(it) })

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('crm:read')")
    fun get(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> = ResponseEntity.ok(ActivityResponse.from(activityService.getActivity(id, orgId)))

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('crm:write')")
    fun update(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: java.util.UUID,
        @Valid @RequestBody request: UpdateActivityRequest,
    ): ResponseEntity<Any> = ResponseEntity.ok(ActivityResponse.from(activityService.updateActivity(id, request, orgId)))

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('crm:write')")
    fun complete(
        @CurrentUserId userId: UUID,
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> = ResponseEntity.ok(ActivityResponse.from(activityService.completeActivity(id, orgId, userId)))

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('crm:write')")
    fun delete(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> {
        activityService.deleteActivity(id, orgId)
        return ResponseEntity.noContent().build()
    }
}
