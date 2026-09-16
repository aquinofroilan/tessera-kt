package com.aquinofroilan.tessera.domain.project.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.domain.project.dto.ResourceUtilizationReport
import com.aquinofroilan.tessera.domain.project.service.ResourceAllocationService
import com.aquinofroilan.tessera.security.CurrentOrganizationId
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/api/v1/projects/utilization")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class ResourceUtilizationController(
    private val allocationService: ResourceAllocationService,
) {
    @GetMapping
    @PreAuthorize("hasAuthority('projects:read')")
    fun getUtilizationReport(
        @CurrentOrganizationId orgId: UUID,
        @RequestParam employeeId: UUID,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate,
    ): ResponseEntity<ResourceUtilizationReport> {
        val report = allocationService.generateUtilizationReport(employeeId, startDate, endDate, orgId)
        return ResponseEntity.ok(report)
    }
}
