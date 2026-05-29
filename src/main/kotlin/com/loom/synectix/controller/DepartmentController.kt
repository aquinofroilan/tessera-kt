package com.loom.synectix.controller

import com.loom.synectix.annotation.LogLevel
import com.loom.synectix.annotation.Loggable
import com.loom.synectix.dto.CreateDepartmentRequest
import com.loom.synectix.dto.DepartmentResponse
import com.loom.synectix.dto.UpdateDepartmentRequest
import com.loom.synectix.security.AuthenticationContext
import com.loom.synectix.service.DepartmentService
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

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('hr:read')")
    fun getDepartment(
        @PathVariable id: String,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(DepartmentResponse.from(departmentService.getDepartment(id, orgId)))
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('hr:write')")
    fun updateDepartment(
        @PathVariable id: String,
        @Valid @RequestBody request: UpdateDepartmentRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(DepartmentResponse.from(departmentService.updateDepartment(id, request, orgId)))
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('hr:write')")
    fun deactivateDepartment(
        @PathVariable id: String,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(DepartmentResponse.from(departmentService.deactivateDepartment(id, orgId)))
    }
}
