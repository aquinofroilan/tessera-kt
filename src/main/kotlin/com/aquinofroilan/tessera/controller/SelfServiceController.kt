package com.aquinofroilan.tessera.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.dto.EmployeeCompensationResponse
import com.aquinofroilan.tessera.dto.EmployeeResponse
import com.aquinofroilan.tessera.dto.LeaveRequestResponse
import com.aquinofroilan.tessera.dto.SubmitSelfLeaveRequest
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.service.SelfServiceService
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

/**
 * Employee self-service surface. Open to any authenticated user; each endpoint
 * resolves the caller's own employee record, so a user can only access their
 * own profile, leave, and compensation.
 */
@RestController
@RequestMapping("/hr/me")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class SelfServiceController(
    private val selfServiceService: SelfServiceService,
    private val authContext: AuthenticationContext,
) {
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    fun myProfile(): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val userId = authContext.userId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(EmployeeResponse.from(selfServiceService.myProfile(userId, orgId)))
    }

    @GetMapping("/leave-requests")
    @PreAuthorize("isAuthenticated()")
    fun myLeaveRequests(): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val userId = authContext.userId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(selfServiceService.myLeaveRequests(userId, orgId).map { LeaveRequestResponse.from(it) })
    }

    @PostMapping("/leave-requests")
    @PreAuthorize("isAuthenticated()")
    fun submitLeave(
        @Valid @RequestBody request: SubmitSelfLeaveRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val userId = authContext.userId() ?: return authContext.unauthorized()
        val created = selfServiceService.submitLeave(userId, orgId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(LeaveRequestResponse.from(created))
    }

    @GetMapping("/leave-balance")
    @PreAuthorize("isAuthenticated()")
    fun myLeaveBalance(
        @RequestParam leaveTypeId: String,
        @RequestParam(required = false) year: Int?,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val userId = authContext.userId() ?: return authContext.unauthorized()
        val resolvedYear = year ?: LocalDate.now(ZoneOffset.UTC).year
        return ResponseEntity.ok(selfServiceService.myLeaveBalance(userId, orgId, leaveTypeId, resolvedYear))
    }

    @GetMapping("/compensation")
    @PreAuthorize("isAuthenticated()")
    fun myCompensationHistory(): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val userId = authContext.userId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(
            selfServiceService.myCompensationHistory(userId, orgId).map { EmployeeCompensationResponse.from(it) },
        )
    }

    @GetMapping("/compensation/current")
    @PreAuthorize("isAuthenticated()")
    fun myCurrentCompensation(
        @RequestParam(required = false) asOf: LocalDate?,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val userId = authContext.userId() ?: return authContext.unauthorized()
        val resolved = asOf ?: LocalDate.now(ZoneOffset.UTC)
        return ResponseEntity.ok(EmployeeCompensationResponse.from(selfServiceService.myCurrentCompensation(userId, orgId, resolved)))
    }
}
