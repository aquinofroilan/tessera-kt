package com.aquinofroilan.tessera.domain.hr.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.domain.hr.dto.EmployeeCompensationResponse
import com.aquinofroilan.tessera.domain.hr.dto.EmployeeResponse
import com.aquinofroilan.tessera.domain.hr.dto.LeaveRequestResponse
import com.aquinofroilan.tessera.domain.hr.service.SelfServiceService
import com.aquinofroilan.tessera.domain.platform.dto.SubmitSelfLeaveRequest
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.security.CurrentOrganizationId
import com.aquinofroilan.tessera.security.CurrentUserId
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

/**
 * Employee self-service surface. Open to any authenticated user; each endpoint
 * resolves the caller's own employee record, so a user can only access their
 * own profile, leave, and compensation.
 */
@RestController
@RequestMapping("/api/v1/hr/me")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class SelfServiceController(
    private val selfServiceService: SelfServiceService,
    private val authContext: AuthenticationContext,
) {
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    fun myProfile(
        @CurrentUserId userId: UUID,
        @CurrentOrganizationId orgId: UUID,
    ): ResponseEntity<Any> = ResponseEntity.ok(EmployeeResponse.from(selfServiceService.myProfile(userId, orgId)))

    @GetMapping("/leave-requests")
    @PreAuthorize("isAuthenticated()")
    fun myLeaveRequests(
        @CurrentUserId userId: UUID,
        @CurrentOrganizationId orgId: UUID,
    ): ResponseEntity<Any> = ResponseEntity.ok(selfServiceService.myLeaveRequests(userId, orgId).map { LeaveRequestResponse.from(it) })

    @PostMapping("/leave-requests")
    @PreAuthorize("isAuthenticated()")
    fun submitLeave(
        @CurrentUserId userId: UUID,
        @CurrentOrganizationId orgId: UUID,
        @Valid @RequestBody request: SubmitSelfLeaveRequest,
    ): ResponseEntity<Any> {
        val created = selfServiceService.submitLeave(userId, orgId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(LeaveRequestResponse.from(created))
    }

    @GetMapping("/leave-balance")
    @PreAuthorize("isAuthenticated()")
    fun myLeaveBalance(
        @CurrentUserId userId: UUID,
        @CurrentOrganizationId orgId: UUID,
        @RequestParam leaveTypeId: java.util.UUID,
        @RequestParam(required = false) year: Int?,
    ): ResponseEntity<Any> {
        val resolvedYear = year ?: LocalDate.now(ZoneOffset.UTC).year
        return ResponseEntity.ok(selfServiceService.myLeaveBalance(userId, orgId, leaveTypeId, resolvedYear))
    }

    @GetMapping("/compensation")
    @PreAuthorize("isAuthenticated()")
    fun myCompensationHistory(
        @CurrentUserId userId: UUID,
        @CurrentOrganizationId orgId: UUID,
    ): ResponseEntity<Any> =
        ResponseEntity.ok(
            selfServiceService.myCompensationHistory(userId, orgId).map { EmployeeCompensationResponse.from(it) },
        )

    @GetMapping("/compensation/current")
    @PreAuthorize("isAuthenticated()")
    fun myCurrentCompensation(
        @CurrentUserId userId: UUID,
        @CurrentOrganizationId orgId: UUID,
        @RequestParam(required = false) asOf: LocalDate?,
    ): ResponseEntity<Any> {
        val resolved = asOf ?: LocalDate.now(ZoneOffset.UTC)
        return ResponseEntity.ok(EmployeeCompensationResponse.from(selfServiceService.myCurrentCompensation(userId, orgId, resolved)))
    }
}
