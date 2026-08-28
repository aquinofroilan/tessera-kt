package com.aquinofroilan.tessera.domain.hr.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.domain.hr.dto.CreateLeaveRequestRequest
import com.aquinofroilan.tessera.domain.hr.dto.LeaveRequestResponse
import com.aquinofroilan.tessera.domain.hr.dto.RejectLeaveRequestRequest
import com.aquinofroilan.tessera.domain.hr.model.LeaveRequestStatus
import com.aquinofroilan.tessera.domain.hr.service.LeaveRequestService
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
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale
import java.util.UUID

@RestController
@RequestMapping("/api/v1/hr/leave-requests")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class LeaveRequestController(
    private val leaveRequestService: LeaveRequestService,
    private val authContext: AuthenticationContext,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('hr:write')")
    fun createLeaveRequest(
        @CurrentOrganizationId orgId: UUID,
        @CurrentUserId userId: UUID,
        @Valid @RequestBody request: CreateLeaveRequestRequest,
    ): ResponseEntity<Any> {
        val created = leaveRequestService.createLeaveRequest(request, orgId, userId)
        return ResponseEntity.status(HttpStatus.CREATED).body(LeaveRequestResponse.from(created))
    }

    @GetMapping
    @PreAuthorize("hasAuthority('hr:read')")
    fun listLeaveRequests(
        @CurrentOrganizationId orgId: UUID,
        @RequestParam(required = false) employeeId: java.util.UUID?,
        @RequestParam(required = false) status: String?,
    ): ResponseEntity<Any> {
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
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> = ResponseEntity.ok(LeaveRequestResponse.from(leaveRequestService.getLeaveRequest(id, orgId)))

    @GetMapping("/balance")
    @PreAuthorize("hasAuthority('hr:read')")
    fun balance(
        @CurrentOrganizationId orgId: UUID,
        @RequestParam employeeId: java.util.UUID,
        @RequestParam leaveTypeId: java.util.UUID,
        @RequestParam(required = false) year: Int?,
    ): ResponseEntity<Any> {
        val resolvedYear = year ?: LocalDate.now(ZoneOffset.UTC).year
        return ResponseEntity.ok(leaveRequestService.balance(employeeId, leaveTypeId, resolvedYear, orgId))
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('hr:approve')")
    fun approveLeaveRequest(
        @CurrentOrganizationId orgId: UUID,
        @CurrentUserId userId: UUID,
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> = ResponseEntity.ok(LeaveRequestResponse.from(leaveRequestService.approveLeaveRequest(id, orgId, userId)))

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('hr:approve')")
    fun rejectLeaveRequest(
        @CurrentOrganizationId orgId: UUID,
        @CurrentUserId userId: UUID,
        @PathVariable id: java.util.UUID,
        @RequestBody(required = false) request: RejectLeaveRequestRequest?,
    ): ResponseEntity<Any> =
        ResponseEntity.ok(
            LeaveRequestResponse.from(leaveRequestService.rejectLeaveRequest(id, request?.reason, orgId, userId)),
        )

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('hr:write')")
    fun cancelLeaveRequest(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> = ResponseEntity.ok(LeaveRequestResponse.from(leaveRequestService.cancelLeaveRequest(id, orgId)))
}
