package com.aquinofroilan.tessera.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.dto.CreateTimeEntryRequest
import com.aquinofroilan.tessera.dto.TimeEntryResponse
import com.aquinofroilan.tessera.dto.UpdateTimeEntryRequest
import com.aquinofroilan.tessera.model.TimeEntryStatus
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.service.TimeEntryService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.Locale

@RestController
@RequestMapping("/projects/time-entries")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class TimeEntryController(
    private val timeEntryService: TimeEntryService,
    private val authContext: AuthenticationContext,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('projects:write')")
    fun createTimeEntry(
        @Valid @RequestBody request: CreateTimeEntryRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val created = timeEntryService.createTimeEntry(request, orgId)
        return ResponseEntity.status(HttpStatus.CREATED).body(TimeEntryResponse.from(created))
    }

    @GetMapping
    @PreAuthorize("hasAuthority('projects:read')")
    fun listTimeEntries(
        @RequestParam(required = false) employeeId: java.util.UUID?,
        @RequestParam(required = false) projectId: java.util.UUID?,
        @RequestParam(required = false) status: String?,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val entryStatus =
            if (status != null) {
                try {
                    TimeEntryStatus.valueOf(status.uppercase(Locale.ROOT))
                } catch (e: IllegalArgumentException) {
                    return ResponseEntity.badRequest().body(mapOf("error" to "Invalid status '$status'"))
                }
            } else {
                null
            }
        return ResponseEntity.ok(
            timeEntryService.listTimeEntries(orgId, employeeId, projectId, entryStatus).map { TimeEntryResponse.from(it) },
        )
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('projects:read')")
    fun getTimeEntry(
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(TimeEntryResponse.from(timeEntryService.getTimeEntry(id, orgId)))
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('projects:write')")
    fun updateTimeEntry(
        @PathVariable id: java.util.UUID,
        @Valid @RequestBody request: UpdateTimeEntryRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(TimeEntryResponse.from(timeEntryService.updateTimeEntry(id, request, orgId)))
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('projects:write')")
    fun submitTimeEntry(
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(TimeEntryResponse.from(timeEntryService.submitTimeEntry(id, orgId)))
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('projects:approve')")
    fun approveTimeEntry(
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val approvedBy = authContext.userId() ?: java.util.UUID.fromString("00000000-0000-0000-0000-000000000000")
        return ResponseEntity.ok(TimeEntryResponse.from(timeEntryService.approveTimeEntry(id, orgId, approvedBy)))
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('projects:approve')")
    fun rejectTimeEntry(
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val decidedBy = authContext.userId() ?: java.util.UUID.fromString("00000000-0000-0000-0000-000000000000")
        return ResponseEntity.ok(TimeEntryResponse.from(timeEntryService.rejectTimeEntry(id, orgId, decidedBy)))
    }
}
