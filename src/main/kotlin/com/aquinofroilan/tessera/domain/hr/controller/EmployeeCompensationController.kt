package com.aquinofroilan.tessera.domain.hr.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.domain.hr.dto.CreateEmployeeCompensationRequest
import com.aquinofroilan.tessera.domain.hr.dto.EmployeeCompensationResponse
import com.aquinofroilan.tessera.domain.hr.service.EmployeeCompensationService
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
import java.util.UUID

@RestController
@RequestMapping("/api/v1/hr/employees/{employeeId}/compensation")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class EmployeeCompensationController(
    private val compensationService: EmployeeCompensationService,
    private val authContext: AuthenticationContext,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('hr:write')")
    fun addCompensation(
        @CurrentOrganizationId orgId: UUID,
        @CurrentUserId userId: UUID,
        @PathVariable employeeId: java.util.UUID,
        @Valid @RequestBody request: CreateEmployeeCompensationRequest,
    ): ResponseEntity<Any> {
        val comp = compensationService.addCompensation(employeeId, request, orgId, userId)
        return ResponseEntity.status(HttpStatus.CREATED).body(EmployeeCompensationResponse.from(comp))
    }

    @GetMapping
    @PreAuthorize("hasAuthority('hr:read')")
    fun listCompensation(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable employeeId: java.util.UUID,
    ): ResponseEntity<Any> =
        ResponseEntity.ok(
            compensationService.listCompensation(employeeId, orgId).map {
                EmployeeCompensationResponse.from(it)
            },
        )

    @GetMapping("/current")
    @PreAuthorize("hasAuthority('hr:read')")
    fun currentCompensation(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable employeeId: java.util.UUID,
        @RequestParam(required = false) asOf: LocalDate?,
    ): ResponseEntity<Any> {
        val resolved = asOf ?: LocalDate.now(ZoneOffset.UTC)
        return ResponseEntity.ok(EmployeeCompensationResponse.from(compensationService.currentCompensation(employeeId, orgId, resolved)))
    }
}
