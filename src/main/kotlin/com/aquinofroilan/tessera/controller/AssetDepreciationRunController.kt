package com.aquinofroilan.tessera.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.dto.CreateDepreciationRunRequest
import com.aquinofroilan.tessera.dto.DepreciationRunResponse
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.service.asset.DepreciationRunService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
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
@RequestMapping("/assets/depreciation-runs")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class AssetDepreciationRunController(
    private val depreciationRunService: DepreciationRunService,
    private val authContext: AuthenticationContext,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('assets:write')")
    fun createRun(
        @Valid @RequestBody request: CreateDepreciationRunRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val year = request.periodYear ?: throw BusinessRuleException("Period year is required")
        val month = request.periodMonth ?: throw BusinessRuleException("Period month is required")
        val run = depreciationRunService.createRun(orgId, year, month)
        val lines = depreciationRunService.listLines(run.id)
        return ResponseEntity.status(HttpStatus.CREATED).body(DepreciationRunResponse.from(run, lines))
    }

    @GetMapping
    @PreAuthorize("hasAuthority('assets:read')")
    fun listRuns(): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(
            depreciationRunService.listRuns(orgId).map { DepreciationRunResponse.from(it) },
        )
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('assets:read')")
    fun getRun(
        @PathVariable id: String,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val runId =
            try {
                UUID.fromString(id)
            } catch (e: IllegalArgumentException) {
                return ResponseEntity.badRequest().body(mapOf("error" to "Invalid depreciation run ID"))
            }
        val run = depreciationRunService.getRun(runId, orgId)
        val lines = depreciationRunService.listLines(run.id)
        return ResponseEntity.ok(DepreciationRunResponse.from(run, lines))
    }

    @PostMapping("/{id}/post")
    @PreAuthorize("hasAuthority('assets:approve')")
    fun postRun(
        @PathVariable id: String,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val userId = authContext.userId() ?: return authContext.unauthorized()
        val runId =
            try {
                UUID.fromString(id)
            } catch (e: IllegalArgumentException) {
                return ResponseEntity.badRequest().body(mapOf("error" to "Invalid depreciation run ID"))
            }
        val posted = depreciationRunService.postRun(runId, orgId, userId)
        val lines = depreciationRunService.listLines(posted.id)
        return ResponseEntity.ok(DepreciationRunResponse.from(posted, lines))
    }
}
