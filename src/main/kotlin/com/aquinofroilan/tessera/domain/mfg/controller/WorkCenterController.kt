package com.aquinofroilan.tessera.domain.mfg.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.domain.mfg.dto.CreateWorkCenterRequest
import com.aquinofroilan.tessera.domain.mfg.dto.UpdateWorkCenterRequest
import com.aquinofroilan.tessera.domain.mfg.dto.WorkCenterResponse
import com.aquinofroilan.tessera.domain.mfg.service.WorkCenterService
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.security.CurrentOrganizationId
import jakarta.validation.Valid
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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/mfg/work-centers")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class WorkCenterController(
    private val workCenterService: WorkCenterService,
    private val authContext: AuthenticationContext,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('mfg:write')")
    fun create(
        @CurrentOrganizationId orgId: UUID,
        @Valid @RequestBody request: CreateWorkCenterRequest,
    ): ResponseEntity<Any> {
        val wc = workCenterService.createWorkCenter(request, orgId)
        return ResponseEntity.status(HttpStatus.CREATED).body(WorkCenterResponse.from(wc))
    }

    @GetMapping
    @PreAuthorize("hasAuthority('mfg:read')")
    fun list(
        @CurrentOrganizationId orgId: UUID,
        @RequestParam(required = false, defaultValue = "true") activeOnly: Boolean,
    ): ResponseEntity<Any> = ResponseEntity.ok(workCenterService.listWorkCenters(orgId, activeOnly).map { WorkCenterResponse.from(it) })

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('mfg:read')")
    fun get(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> = ResponseEntity.ok(WorkCenterResponse.from(workCenterService.getWorkCenter(id, orgId)))

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('mfg:write')")
    fun update(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: java.util.UUID,
        @Valid @RequestBody request: UpdateWorkCenterRequest,
    ): ResponseEntity<Any> = ResponseEntity.ok(WorkCenterResponse.from(workCenterService.updateWorkCenter(id, request, orgId)))

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('mfg:write')")
    fun deactivate(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> = ResponseEntity.ok(WorkCenterResponse.from(workCenterService.deactivateWorkCenter(id, orgId)))
}
