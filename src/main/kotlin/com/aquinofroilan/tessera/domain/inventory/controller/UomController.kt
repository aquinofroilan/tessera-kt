package com.aquinofroilan.tessera.domain.inventory.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.domain.inventory.dto.CreateUomRequest
import com.aquinofroilan.tessera.domain.inventory.dto.UomResponse
import com.aquinofroilan.tessera.domain.inventory.dto.UpdateUomRequest
import com.aquinofroilan.tessera.domain.inventory.service.UnitOfMeasureService
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
import java.math.BigDecimal
import java.util.UUID

@RestController
@RequestMapping("/api/v1/inventory/uoms")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class UomController(
    private val uomService: UnitOfMeasureService,
    private val authContext: AuthenticationContext,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('inventory:write')")
    fun create(
        @CurrentOrganizationId orgId: UUID,
        @Valid @RequestBody request: CreateUomRequest,
    ): ResponseEntity<Any> {
        val u = uomService.createUom(request, orgId)
        return ResponseEntity.status(HttpStatus.CREATED).body(UomResponse.from(u))
    }

    @GetMapping
    @PreAuthorize("hasAuthority('inventory:read')")
    fun list(
        @CurrentOrganizationId orgId: UUID,
        @RequestParam(required = false, defaultValue = "true") activeOnly: Boolean,
    ): ResponseEntity<Any> = ResponseEntity.ok(uomService.listUoms(orgId, activeOnly).map { UomResponse.from(it) })

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('inventory:read')")
    fun get(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: UUID,
    ): ResponseEntity<Any> = ResponseEntity.ok(UomResponse.from(uomService.getUom(id, orgId)))

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('inventory:write')")
    fun update(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateUomRequest,
    ): ResponseEntity<Any> = ResponseEntity.ok(UomResponse.from(uomService.updateUom(id, request, orgId)))

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('inventory:write')")
    fun deactivate(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: UUID,
    ): ResponseEntity<Any> = ResponseEntity.ok(UomResponse.from(uomService.deactivateUom(id, orgId)))

    @GetMapping("/convert")
    @PreAuthorize("hasAuthority('inventory:read')")
    fun convert(
        @CurrentOrganizationId orgId: UUID,
        @RequestParam quantity: BigDecimal,
        @RequestParam fromUomId: UUID,
        @RequestParam toUomId: UUID,
    ): ResponseEntity<Any> {
        val result = uomService.convert(quantity, fromUomId, toUomId, orgId)
        return ResponseEntity.ok(mapOf("converted" to result, "fromUomId" to fromUomId, "toUomId" to toUomId))
    }
}
