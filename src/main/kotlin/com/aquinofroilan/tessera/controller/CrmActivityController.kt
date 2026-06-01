package com.aquinofroilan.tessera.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.dto.ActivityResponse
import com.aquinofroilan.tessera.dto.CreateActivityRequest
import com.aquinofroilan.tessera.dto.UpdateActivityRequest
import com.aquinofroilan.tessera.model.CrmActivityType
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.service.CrmActivityService
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

@RestController
@RequestMapping("/crm/activities")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class CrmActivityController(
    private val activityService: CrmActivityService,
    private val authContext: AuthenticationContext,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('crm:write')")
    fun create(
        @Valid @RequestBody request: CreateActivityRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val userId = authContext.userId() ?: "api-key"
        val activity = activityService.createActivity(request, orgId, userId)
        return ResponseEntity.status(HttpStatus.CREATED).body(ActivityResponse.from(activity))
    }

    @GetMapping
    @PreAuthorize("hasAuthority('crm:read')")
    fun list(
        @RequestParam(required = false) type: String?,
        @RequestParam(required = false) leadId: String?,
        @RequestParam(required = false) opportunityId: String?,
        @RequestParam(required = false) contactId: String?,
        @RequestParam(required = false) customerId: String?,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
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
    fun listMyTasks(): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val userId = authContext.userId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(activityService.listMyOpenTasks(orgId, userId).map { ActivityResponse.from(it) })
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('crm:read')")
    fun get(
        @PathVariable id: String,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(ActivityResponse.from(activityService.getActivity(id, orgId)))
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('crm:write')")
    fun update(
        @PathVariable id: String,
        @Valid @RequestBody request: UpdateActivityRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(ActivityResponse.from(activityService.updateActivity(id, request, orgId)))
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('crm:write')")
    fun complete(
        @PathVariable id: String,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val userId = authContext.userId() ?: "api-key"
        return ResponseEntity.ok(ActivityResponse.from(activityService.completeActivity(id, orgId, userId)))
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('crm:write')")
    fun delete(
        @PathVariable id: String,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        activityService.deleteActivity(id, orgId)
        return ResponseEntity.noContent().build()
    }
}
