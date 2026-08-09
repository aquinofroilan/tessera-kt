package com.aquinofroilan.tessera.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.dto.CreateLeaveRequestRequest
import com.aquinofroilan.tessera.dto.LeaveRequestResponse
import com.aquinofroilan.tessera.dto.RejectLeaveRequestRequest
import com.aquinofroilan.tessera.model.LeaveRequestStatus
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.service.LeaveRequestService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale

@RestController
@RequestMapping("/hr/leave-requests")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class LeaveRequestController(
    private val leaveRequestService: LeaveRequestService,
    private val authContext: AuthenticationContext,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('hr:write')")
    fun createLeaveRequest(
        @Valid @RequestBody request: CreateLeaveRequestRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val requestedBy = authContext.userId() ?: java.util.UUID.fromString("00000000-0000-0000-0000-000000000000")
        val created = leaveRequestService.createLeaveRequest(request, orgId, requestedBy)
        return ResponseEntity.status(HttpStatus.CREATED).body(LeaveRequestResponse.from(created))
    }

    @GetMapping
    @PreAuthorize("hasAuthority('hr:read')")
    fun listLeaveRequests(
        @RequestParam(required = false) employeeId: java.util.UUID?,
        @RequestParam(required = false) status: String?,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val leaveStatus =
            if (status != null) {
                try {
                    LeaveRequestStatus.valueOf(status.uppercase(Locale.ROOT))
                } catch (e: IllegalArgumentException) {
                    return ResponseEntity.badRequest().body(mapOf("error" to "Invalid status '$status'"))
                }
            } else {
                null
            }
        return ResponseEntity.ok(
            leaveRequestService.listLeaveRequests(orgId, employeeId, leaveStatus).map { LeaveRequestResponse.from(it) },
        )
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('hr:read')")
    fun getLeaveRequest(
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(LeaveRequestResponse.from(leaveRequestService.getLeaveRequest(id, orgId)))
    }

    @GetMapping("/balance")
    @PreAuthorize("hasAuthority('hr:read')")
    fun balance(
        @RequestParam employeeId: java.util.UUID,
        @RequestParam leaveTypeId: java.util.UUID,
        @RequestParam(required = false) year: Int?,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val resolvedYear = year ?: LocalDate.now(ZoneOffset.UTC).year
        return ResponseEntity.ok(leaveRequestService.balance(employeeId, leaveTypeId, resolvedYear, orgId))
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('hr:approve')")
    fun approveLeaveRequest(
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val decidedBy = authContext.userId() ?: java.util.UUID.fromString("00000000-0000-0000-0000-000000000000")
        return ResponseEntity.ok(LeaveRequestResponse.from(leaveRequestService.approveLeaveRequest(id, orgId, decidedBy)))
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('hr:approve')")
    fun rejectLeaveRequest(
        @PathVariable id: java.util.UUID,
        @RequestBody(required = false) request: RejectLeaveRequestRequest?,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val decidedBy = authContext.userId() ?: java.util.UUID.fromString("00000000-0000-0000-0000-000000000000")
        return ResponseEntity.ok(
            LeaveRequestResponse.from(leaveRequestService.rejectLeaveRequest(id, request?.reason, orgId, decidedBy)),
        )
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('hr:write')")
    fun cancelLeaveRequest(
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(LeaveRequestResponse.from(leaveRequestService.cancelLeaveRequest(id, orgId)))
    }
}
