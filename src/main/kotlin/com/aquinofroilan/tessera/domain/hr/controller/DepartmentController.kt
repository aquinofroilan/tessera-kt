package com.aquinofroilan.tessera.domain.hr.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.domain.hr.dto.CreateDepartmentRequest
import com.aquinofroilan.tessera.domain.hr.dto.DepartmentResponse
import com.aquinofroilan.tessera.domain.hr.dto.SetDepartmentParentRequest
import com.aquinofroilan.tessera.domain.hr.dto.UpdateDepartmentRequest
import com.aquinofroilan.tessera.domain.hr.service.DepartmentService
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
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/hr/departments")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class DepartmentController(
    private val departmentService: DepartmentService,
    private val authContext: AuthenticationContext,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('hr:write')")
    fun createDepartment(
        @CurrentOrganizationId orgId: UUID,
        @Valid @RequestBody request: CreateDepartmentRequest,
    ): ResponseEntity<Any> {
        val department = departmentService.createDepartment(request, orgId)
        return ResponseEntity.status(HttpStatus.CREATED).body(DepartmentResponse.from(department))
    }

    @GetMapping
    @PreAuthorize("hasAuthority('hr:read')")
    fun listDepartments(
        @CurrentOrganizationId orgId: UUID,
        @RequestParam(required = false, defaultValue = "false") activeOnly: Boolean,
    ): ResponseEntity<Any> = ResponseEntity.ok(departmentService.listDepartments(orgId, activeOnly).map { DepartmentResponse.from(it) })

    @GetMapping("/org-chart")
    @PreAuthorize("hasAuthority('hr:read')")
    fun getOrgChart(
        @CurrentOrganizationId orgId: UUID,
    ): ResponseEntity<Any> = ResponseEntity.ok(departmentService.getOrgChart(orgId))

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('hr:read')")
    fun getDepartment(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> = ResponseEntity.ok(DepartmentResponse.from(departmentService.getDepartment(id, orgId)))

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('hr:write')")
    fun updateDepartment(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: java.util.UUID,
        @Valid @RequestBody request: UpdateDepartmentRequest,
    ): ResponseEntity<Any> = ResponseEntity.ok(DepartmentResponse.from(departmentService.updateDepartment(id, request, orgId)))

    @PutMapping("/{id}/parent")
    @PreAuthorize("hasAuthority('hr:write')")
    fun setParent(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: java.util.UUID,
        @Valid @RequestBody request: SetDepartmentParentRequest,
    ): ResponseEntity<Any> = ResponseEntity.ok(DepartmentResponse.from(departmentService.setParent(id, request.parentId, orgId)))

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('hr:write')")
    fun deactivateDepartment(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> = ResponseEntity.ok(DepartmentResponse.from(departmentService.deactivateDepartment(id, orgId)))
}
