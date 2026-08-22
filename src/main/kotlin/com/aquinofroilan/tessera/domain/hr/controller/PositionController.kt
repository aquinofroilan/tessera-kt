package com.aquinofroilan.tessera.domain.hr.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.domain.hr.dto.CreatePositionRequest
import com.aquinofroilan.tessera.domain.hr.dto.PositionResponse
import com.aquinofroilan.tessera.domain.hr.dto.UpdatePositionRequest
import com.aquinofroilan.tessera.domain.hr.service.PositionService
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.security.CurrentOrganizationId
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/hr/positions")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class PositionController(
    private val positionService: PositionService,
    private val authContext: AuthenticationContext,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('hr:write')")
    fun createPosition(
        @CurrentOrganizationId orgId: UUID,
        @Valid @RequestBody request: CreatePositionRequest,
    ): ResponseEntity<Any> =
        ResponseEntity.status(HttpStatus.CREATED).body(PositionResponse.from(positionService.createPosition(request, orgId)))

    @GetMapping
    @PreAuthorize("hasAuthority('hr:read')")
    fun listPositions(
        @CurrentOrganizationId orgId: UUID,
        @RequestParam(required = false, defaultValue = "false") activeOnly: Boolean,
    ): ResponseEntity<Any> = ResponseEntity.ok(positionService.listPositions(orgId, activeOnly).map { PositionResponse.from(it) })

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('hr:read')")
    fun getPosition(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> = ResponseEntity.ok(PositionResponse.from(positionService.getPosition(id, orgId)))

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('hr:write')")
    fun updatePosition(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: java.util.UUID,
        @Valid @RequestBody request: UpdatePositionRequest,
    ): ResponseEntity<Any> = ResponseEntity.ok(PositionResponse.from(positionService.updatePosition(id, request, orgId)))

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('hr:write')")
    fun deactivatePosition(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> = ResponseEntity.ok(PositionResponse.from(positionService.deactivatePosition(id, orgId)))
}
