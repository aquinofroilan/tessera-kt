package com.aquinofroilan.tessera.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.dto.CreateDepartmentRequest
import com.aquinofroilan.tessera.dto.DepartmentResponse
import com.aquinofroilan.tessera.dto.SetDepartmentParentRequest
import com.aquinofroilan.tessera.dto.UpdateDepartmentRequest
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.service.DepartmentService
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

@RestController
@RequestMapping("/hr/departments")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class DepartmentController(
    private val departmentService: DepartmentService,
    private val authContext: AuthenticationContext,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('hr:write')")
    fun createDepartment(
        @Valid @RequestBody request: CreateDepartmentRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val department = departmentService.createDepartment(request, orgId)
        return ResponseEntity.status(HttpStatus.CREATED).body(DepartmentResponse.from(department))
    }

    @GetMapping
    @PreAuthorize("hasAuthority('hr:read')")
    fun listDepartments(
        @RequestParam(required = false, defaultValue = "false") activeOnly: Boolean,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(departmentService.listDepartments(orgId, activeOnly).map { DepartmentResponse.from(it) })
    }

    @GetMapping("/org-chart")
    @PreAuthorize("hasAuthority('hr:read')")
    fun getOrgChart(): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(departmentService.getOrgChart(orgId))
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('hr:read')")
    fun getDepartment(
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(DepartmentResponse.from(departmentService.getDepartment(id, orgId)))
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('hr:write')")
    fun updateDepartment(
        @PathVariable id: java.util.UUID,
        @Valid @RequestBody request: UpdateDepartmentRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(DepartmentResponse.from(departmentService.updateDepartment(id, request, orgId)))
    }

    @PutMapping("/{id}/parent")
    @PreAuthorize("hasAuthority('hr:write')")
    fun setParent(
        @PathVariable id: java.util.UUID,
        @Valid @RequestBody request: SetDepartmentParentRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(DepartmentResponse.from(departmentService.setParent(id, request.parentId, orgId)))
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('hr:write')")
    fun deactivateDepartment(
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(DepartmentResponse.from(departmentService.deactivateDepartment(id, orgId)))
    }
}
