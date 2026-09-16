package com.aquinofroilan.tessera.domain.project.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.domain.project.dto.ProjectProfitabilityReport
import com.aquinofroilan.tessera.domain.project.dto.ProjectRevenueRecognitionResponse
import com.aquinofroilan.tessera.domain.project.dto.RecognizeRevenueRequest
import com.aquinofroilan.tessera.domain.project.service.ProjectProfitabilityService
import com.aquinofroilan.tessera.security.CurrentOrganizationId
import com.aquinofroilan.tessera.security.CurrentUserId
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/projects/{projectId}/profitability")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class ProjectProfitabilityController(
    private val profitabilityService: ProjectProfitabilityService,
) {
    @GetMapping
    @PreAuthorize("hasAuthority('projects:read')")
    fun getProfitabilityReport(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable projectId: UUID,
    ): ResponseEntity<ProjectProfitabilityReport> {
        val report = profitabilityService.getProfitabilityReport(projectId, orgId)
        return ResponseEntity.ok(report)
    }

    @PostMapping("/recognize-revenue")
    @PreAuthorize("hasAuthority('projects:write')")
    fun recognizeRevenue(
        @CurrentOrganizationId orgId: UUID,
        @CurrentUserId userId: UUID,
        @PathVariable projectId: UUID,
        @Valid @RequestBody request: RecognizeRevenueRequest,
    ): ResponseEntity<ProjectRevenueRecognitionResponse> {
        val response = profitabilityService.recognizeRevenue(projectId, request, orgId, userId)
        return ResponseEntity.ok(response)
    }
}
