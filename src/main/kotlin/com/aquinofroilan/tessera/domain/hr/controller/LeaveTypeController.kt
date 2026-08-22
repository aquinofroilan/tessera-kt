package com.aquinofroilan.tessera.domain.hr.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.domain.hr.dto.CreateLeaveTypeRequest
import com.aquinofroilan.tessera.domain.hr.dto.LeaveTypeResponse
import com.aquinofroilan.tessera.domain.hr.dto.UpdateLeaveTypeRequest
import com.aquinofroilan.tessera.domain.hr.service.LeaveTypeService
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.security.CurrentOrganizationId
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
import java.util.UUID

@RestController
@RequestMapping("/api/v1/hr/leave-types")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class LeaveTypeController(
    private val leaveTypeService: LeaveTypeService,
    private val authContext: AuthenticationContext,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('hr:write')")
    fun createLeaveType(
        @CurrentOrganizationId orgId: UUID,
        @Valid @RequestBody request: CreateLeaveTypeRequest,
    ): ResponseEntity<Any> =
        ResponseEntity.status(HttpStatus.CREATED).body(LeaveTypeResponse.from(leaveTypeService.createLeaveType(request, orgId)))

    @GetMapping
    @PreAuthorize("hasAuthority('hr:read')")
    fun listLeaveTypes(
        @CurrentOrganizationId orgId: UUID,
        @RequestParam(required = false, defaultValue = "false") activeOnly: Boolean,
    ): ResponseEntity<Any> = ResponseEntity.ok(leaveTypeService.listLeaveTypes(orgId, activeOnly).map { LeaveTypeResponse.from(it) })

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('hr:read')")
    fun getLeaveType(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> = ResponseEntity.ok(LeaveTypeResponse.from(leaveTypeService.getLeaveType(id, orgId)))

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('hr:write')")
    fun updateLeaveType(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: java.util.UUID,
        @Valid @RequestBody request: UpdateLeaveTypeRequest,
    ): ResponseEntity<Any> = ResponseEntity.ok(LeaveTypeResponse.from(leaveTypeService.updateLeaveType(id, request, orgId)))

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('hr:write')")
    fun deactivateLeaveType(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> = ResponseEntity.ok(LeaveTypeResponse.from(leaveTypeService.deactivateLeaveType(id, orgId)))
}
