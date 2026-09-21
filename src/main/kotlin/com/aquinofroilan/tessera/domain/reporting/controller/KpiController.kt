package com.aquinofroilan.tessera.domain.reporting.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.domain.reporting.dto.KpiDefinitionRequest
import com.aquinofroilan.tessera.domain.reporting.dto.KpiTrendResponse
import com.aquinofroilan.tessera.domain.reporting.dto.KpiValueRequest
import com.aquinofroilan.tessera.domain.reporting.model.KpiDefinition
import com.aquinofroilan.tessera.domain.reporting.model.KpiValue
import com.aquinofroilan.tessera.domain.reporting.service.KpiService
import com.aquinofroilan.tessera.security.CurrentOrganizationId
import org.springframework.format.annotation.DateTimeFormat
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
import java.util.UUID

@RestController
@RequestMapping("/api/v1/reporting/kpis")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class KpiController(
    private val kpiService: KpiService,
) {
    @GetMapping
    @PreAuthorize("hasAuthority('reporting:read')")
    fun listDefinitions(
        @CurrentOrganizationId orgId: UUID,
    ): ResponseEntity<List<KpiDefinition>> = ResponseEntity.ok(kpiService.listDefinitions(orgId))

    @PostMapping
    @PreAuthorize("hasAuthority('reporting:write')")
    fun createDefinition(
        @CurrentOrganizationId orgId: UUID,
        @RequestBody request: KpiDefinitionRequest,
    ): ResponseEntity<KpiDefinition> = ResponseEntity.ok(kpiService.createDefinition(orgId, request))

    @PostMapping("/{id}/values")
    @PreAuthorize("hasAuthority('reporting:write')")
    fun recordValue(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: UUID,
        @RequestBody request: KpiValueRequest,
    ): ResponseEntity<KpiValue> = ResponseEntity.ok(kpiService.recordValue(orgId, id, request))

    @GetMapping("/{id}/trend")
    @PreAuthorize("hasAuthority('reporting:read')")
    fun getTrend(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: UUID,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate,
    ): ResponseEntity<KpiTrendResponse> = ResponseEntity.ok(kpiService.getKpiTrend(orgId, id, startDate, endDate))
}
