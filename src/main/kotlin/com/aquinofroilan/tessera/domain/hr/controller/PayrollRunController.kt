package com.aquinofroilan.tessera.domain.hr.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.domain.hr.dto.CreatePayrollRunRequest
import com.aquinofroilan.tessera.domain.hr.dto.PayrollRunResponse
import com.aquinofroilan.tessera.domain.hr.model.PayrollRunStatus
import com.aquinofroilan.tessera.domain.hr.service.PayrollRunService
import com.aquinofroilan.tessera.security.AuthenticationContext
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
import java.util.Locale

@RestController
@RequestMapping("/api/v1/hr/payroll-runs")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class PayrollRunController(
    private val payrollRunService: PayrollRunService,
    private val authContext: AuthenticationContext,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('hr:write')")
    fun createPayrollRun(
        @Valid @RequestBody request: CreatePayrollRunRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val createdBy = authContext.userId() ?: java.util.UUID.fromString("00000000-0000-0000-0000-000000000000")
        val run = payrollRunService.createPayrollRun(request, orgId, createdBy)
        return ResponseEntity.status(HttpStatus.CREATED).body(PayrollRunResponse.from(run))
    }

    @GetMapping
    @PreAuthorize("hasAuthority('hr:read')")
    fun listPayrollRuns(
        @RequestParam(required = false) status: String?,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val runStatus =
            if (status != null) {
                try {
                    PayrollRunStatus.valueOf(status.uppercase(Locale.ROOT))
                } catch (e: IllegalArgumentException) {
                    return ResponseEntity.badRequest().body(mapOf("error" to "Invalid status '$status'"))
                }
            } else {
                null
            }
        return ResponseEntity.ok(payrollRunService.listPayrollRuns(orgId, runStatus).map { PayrollRunResponse.from(it) })
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('hr:read')")
    fun getPayrollRun(
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(PayrollRunResponse.from(payrollRunService.getPayrollRun(id, orgId)))
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('hr:approve')")
    fun approvePayrollRun(
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val approvedBy = authContext.userId() ?: java.util.UUID.fromString("00000000-0000-0000-0000-000000000000")
        return ResponseEntity.ok(PayrollRunResponse.from(payrollRunService.approvePayrollRun(id, orgId, approvedBy)))
    }

    @PostMapping("/{id}/pay")
    @PreAuthorize("hasAuthority('hr:approve')")
    fun payPayrollRun(
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val paidBy = authContext.userId() ?: java.util.UUID.fromString("00000000-0000-0000-0000-000000000000")
        return ResponseEntity.ok(PayrollRunResponse.from(payrollRunService.payPayrollRun(id, orgId, paidBy)))
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('hr:write')")
    fun cancelPayrollRun(
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(PayrollRunResponse.from(payrollRunService.cancelPayrollRun(id, orgId)))
    }
}
