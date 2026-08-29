package com.aquinofroilan.tessera.domain.procurement.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.domain.procurement.dto.CreateVendorEvaluationRequest
import com.aquinofroilan.tessera.domain.procurement.dto.VendorEvaluationResponse
import com.aquinofroilan.tessera.domain.procurement.dto.VendorPerformanceSummaryResponse
import com.aquinofroilan.tessera.domain.procurement.service.VendorPerformanceService
import com.aquinofroilan.tessera.security.CurrentOrganizationId
import com.aquinofroilan.tessera.security.CurrentUserId
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/api/v1/procurement/vendors")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class VendorPerformanceController(
    private val vendorPerformanceService: VendorPerformanceService,
) {
    @GetMapping("/performance")
    @PreAuthorize("hasAuthority('ap:read') or hasAuthority('organization:read') or hasRole('SUPER_ADMIN')")
    fun listAllVendorPerformance(
        @CurrentOrganizationId orgId: UUID,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) fromDate: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) toDate: LocalDate?,
    ): ResponseEntity<List<VendorPerformanceSummaryResponse>> =
        ResponseEntity.ok(vendorPerformanceService.listAllVendorPerformance(orgId, fromDate, toDate))

    @GetMapping("/{vendorId}/performance")
    @PreAuthorize("hasAuthority('ap:read') or hasAuthority('organization:read') or hasRole('SUPER_ADMIN')")
    fun getVendorPerformance(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable vendorId: UUID,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) fromDate: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) toDate: LocalDate?,
    ): ResponseEntity<VendorPerformanceSummaryResponse> =
        ResponseEntity.ok(vendorPerformanceService.getVendorPerformance(vendorId, orgId, fromDate, toDate))

    @PostMapping("/{vendorId}/evaluations")
    @PreAuthorize("hasAuthority('ap:create') or hasAuthority('organization:write') or hasRole('SUPER_ADMIN')")
    fun createEvaluation(
        @CurrentOrganizationId orgId: UUID,
        @CurrentUserId userId: UUID,
        @PathVariable vendorId: UUID,
        @Valid @RequestBody request: CreateVendorEvaluationRequest,
    ): ResponseEntity<VendorEvaluationResponse> {
        val evaluation = vendorPerformanceService.recordEvaluation(vendorId, orgId, userId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(evaluation)
    }

    @GetMapping("/{vendorId}/evaluations")
    @PreAuthorize("hasAuthority('ap:read') or hasAuthority('organization:read') or hasRole('SUPER_ADMIN')")
    fun listEvaluations(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable vendorId: UUID,
    ): ResponseEntity<List<VendorEvaluationResponse>> = ResponseEntity.ok(vendorPerformanceService.listEvaluations(vendorId, orgId))

    @GetMapping("/{vendorId}/evaluations/{id}")
    @PreAuthorize("hasAuthority('ap:read') or hasAuthority('organization:read') or hasRole('SUPER_ADMIN')")
    fun getEvaluation(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable vendorId: UUID,
        @PathVariable id: UUID,
    ): ResponseEntity<VendorEvaluationResponse> = ResponseEntity.ok(vendorPerformanceService.getEvaluation(vendorId, orgId, id))

    @DeleteMapping("/{vendorId}/evaluations/{id}")
    @PreAuthorize("hasAuthority('ap:create') or hasAuthority('organization:write') or hasRole('SUPER_ADMIN')")
    fun deleteEvaluation(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable vendorId: UUID,
        @PathVariable id: UUID,
    ): ResponseEntity<Void> {
        vendorPerformanceService.deleteEvaluation(vendorId, orgId, id)
        return ResponseEntity.noContent().build()
    }
}
