package com.aquinofroilan.tessera.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.dto.CreateEmployeeCompensationRequest
import com.aquinofroilan.tessera.dto.EmployeeCompensationResponse
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.service.EmployeeCompensationService
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

@RestController
@RequestMapping("/hr/employees/{employeeId}/compensation")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class EmployeeCompensationController(
    private val compensationService: EmployeeCompensationService,
    private val authContext: AuthenticationContext,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('hr:write')")
    fun addCompensation(
        @PathVariable employeeId: String,
        @Valid @RequestBody request: CreateEmployeeCompensationRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val createdBy = authContext.userId() ?: "api-key"
        val comp = compensationService.addCompensation(employeeId, request, orgId, createdBy)
        return ResponseEntity.status(HttpStatus.CREATED).body(EmployeeCompensationResponse.from(comp))
    }

    @GetMapping
    @PreAuthorize("hasAuthority('hr:read')")
    fun listCompensation(
        @PathVariable employeeId: String,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(compensationService.listCompensation(employeeId, orgId).map { EmployeeCompensationResponse.from(it) })
    }

    @GetMapping("/current")
    @PreAuthorize("hasAuthority('hr:read')")
    fun currentCompensation(
        @PathVariable employeeId: String,
        @RequestParam(required = false) asOf: LocalDate?,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val resolved = asOf ?: LocalDate.now(ZoneOffset.UTC)
        return ResponseEntity.ok(EmployeeCompensationResponse.from(compensationService.currentCompensation(employeeId, orgId, resolved)))
    }
}
