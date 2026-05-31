package com.aquinofroilan.tessera.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.dto.CreateLeaveTypeRequest
import com.aquinofroilan.tessera.dto.LeaveTypeResponse
import com.aquinofroilan.tessera.dto.UpdateLeaveTypeRequest
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.service.LeaveTypeService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/hr/leave-types")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class LeaveTypeController(
    private val leaveTypeService: LeaveTypeService,
    private val authContext: AuthenticationContext,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('hr:write')")
    fun createLeaveType(
        @Valid @RequestBody request: CreateLeaveTypeRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.status(HttpStatus.CREATED).body(LeaveTypeResponse.from(leaveTypeService.createLeaveType(request, orgId)))
    }

    @GetMapping
    @PreAuthorize("hasAuthority('hr:read')")
    fun listLeaveTypes(
        @RequestParam(required = false, defaultValue = "false") activeOnly: Boolean,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(leaveTypeService.listLeaveTypes(orgId, activeOnly).map { LeaveTypeResponse.from(it) })
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('hr:read')")
    fun getLeaveType(
        @PathVariable id: String,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(LeaveTypeResponse.from(leaveTypeService.getLeaveType(id, orgId)))
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('hr:write')")
    fun updateLeaveType(
        @PathVariable id: String,
        @Valid @RequestBody request: UpdateLeaveTypeRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(LeaveTypeResponse.from(leaveTypeService.updateLeaveType(id, request, orgId)))
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('hr:write')")
    fun deactivateLeaveType(
        @PathVariable id: String,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(LeaveTypeResponse.from(leaveTypeService.deactivateLeaveType(id, orgId)))
    }
}
