package com.aquinofroilan.tessera.domain.hr.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.domain.hr.dto.CreateEmployeeRequest
import com.aquinofroilan.tessera.domain.hr.dto.EmployeeResponse
import com.aquinofroilan.tessera.domain.hr.dto.TerminateEmployeeRequest
import com.aquinofroilan.tessera.domain.hr.dto.UpdateEmployeeRequest
import com.aquinofroilan.tessera.domain.hr.model.EmploymentStatus
import com.aquinofroilan.tessera.domain.hr.service.EmployeeService
import com.aquinofroilan.tessera.security.AuthenticationContext
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
@RequestMapping("/api/v1/hr/employees")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class EmployeeController(
    private val employeeService: EmployeeService,
    private val authContext: AuthenticationContext,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('hr:write')")
    fun createEmployee(
        @Valid @RequestBody request: CreateEmployeeRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val employee = employeeService.createEmployee(request, orgId)
        return ResponseEntity.status(HttpStatus.CREATED).body(EmployeeResponse.from(employee))
    }

    @GetMapping
    @PreAuthorize("hasAuthority('hr:read')")
    fun listEmployees(
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) departmentId: java.util.UUID?,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val employmentStatus =
            if (status != null) {
                try {
                    EmploymentStatus.valueOf(status.uppercase(Locale.ROOT))
                } catch (e: IllegalArgumentException) {
                    return ResponseEntity.badRequest().body(mapOf("error" to "Invalid status '$status'"))
                }
            } else {
                null
            }
        return ResponseEntity.ok(employeeService.listEmployees(orgId, employmentStatus, departmentId).map { EmployeeResponse.from(it) })
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('hr:read')")
    fun getEmployee(
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(EmployeeResponse.from(employeeService.getEmployee(id, orgId)))
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('hr:write')")
    fun updateEmployee(
        @PathVariable id: java.util.UUID,
        @Valid @RequestBody request: UpdateEmployeeRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(EmployeeResponse.from(employeeService.updateEmployee(id, request, orgId)))
    }

    @PostMapping("/{id}/assign-department")
    @PreAuthorize("hasAuthority('hr:write')")
    fun assignDepartment(
        @PathVariable id: java.util.UUID,
        @RequestParam(required = false) departmentId: java.util.UUID?,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(EmployeeResponse.from(employeeService.assignDepartment(id, departmentId, orgId)))
    }

    @PostMapping("/{id}/leave")
    @PreAuthorize("hasAuthority('hr:write')")
    fun placeOnLeave(
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(EmployeeResponse.from(employeeService.placeOnLeave(id, orgId)))
    }

    @PostMapping("/{id}/return")
    @PreAuthorize("hasAuthority('hr:write')")
    fun returnFromLeave(
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(EmployeeResponse.from(employeeService.returnFromLeave(id, orgId)))
    }

    @PostMapping("/{id}/terminate")
    @PreAuthorize("hasAuthority('hr:write')")
    fun terminate(
        @PathVariable id: java.util.UUID,
        @Valid @RequestBody request: TerminateEmployeeRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val date = request.terminationDate ?: return ResponseEntity.badRequest().body(mapOf("error" to "terminationDate is required"))
        return ResponseEntity.ok(EmployeeResponse.from(employeeService.terminate(id, date, orgId)))
    }
}
