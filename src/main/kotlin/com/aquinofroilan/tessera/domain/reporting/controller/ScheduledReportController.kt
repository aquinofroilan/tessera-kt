package com.aquinofroilan.tessera.domain.reporting.controller

import com.aquinofroilan.tessera.domain.reporting.dto.CreateScheduledReportRequest
import com.aquinofroilan.tessera.domain.reporting.dto.ScheduledReportResponse
import com.aquinofroilan.tessera.domain.reporting.dto.UpdateScheduledReportRequest
import com.aquinofroilan.tessera.domain.reporting.service.ScheduledReportService
import com.aquinofroilan.tessera.security.CurrentOrganizationId
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/scheduled-reports")
class ScheduledReportController(
    private val scheduledReportService: ScheduledReportService,
) {
    @GetMapping
    @PreAuthorize("hasAuthority('reports:read')")
    fun listReports(
        @CurrentOrganizationId orgId: UUID,
        pageable: Pageable,
    ): ResponseEntity<Page<ScheduledReportResponse>> = ResponseEntity.ok(scheduledReportService.listReports(orgId, pageable))

    @GetMapping("/{reportId}")
    @PreAuthorize("hasAuthority('reports:read')")
    fun getReport(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable reportId: UUID,
    ): ResponseEntity<ScheduledReportResponse> = ResponseEntity.ok(scheduledReportService.getReport(orgId, reportId))

    @PostMapping
    @PreAuthorize("hasAuthority('reports:write')")
    fun createReport(
        @CurrentOrganizationId orgId: UUID,
        @Valid @RequestBody request: CreateScheduledReportRequest,
    ): ResponseEntity<ScheduledReportResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(scheduledReportService.createReport(orgId, request))

    @PutMapping("/{reportId}")
    @PreAuthorize("hasAuthority('reports:write')")
    fun updateReport(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable reportId: UUID,
        @Valid @RequestBody request: UpdateScheduledReportRequest,
    ): ResponseEntity<ScheduledReportResponse> = ResponseEntity.ok(scheduledReportService.updateReport(orgId, reportId, request))

    @DeleteMapping("/{reportId}")
    @PreAuthorize("hasAuthority('reports:write')")
    fun deleteReport(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable reportId: UUID,
    ): ResponseEntity<Void> {
        scheduledReportService.deleteReport(orgId, reportId)
        return ResponseEntity.noContent().build()
    }
}
