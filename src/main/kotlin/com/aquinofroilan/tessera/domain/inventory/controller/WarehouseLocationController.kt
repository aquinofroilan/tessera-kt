package com.aquinofroilan.tessera.domain.inventory.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.domain.inventory.dto.CreateWarehouseLocationRequest
import com.aquinofroilan.tessera.domain.inventory.dto.WarehouseLocationResponse
import com.aquinofroilan.tessera.domain.inventory.model.WarehouseLocation
import com.aquinofroilan.tessera.domain.inventory.service.WarehouseLocationService
import com.aquinofroilan.tessera.security.CurrentOrganizationId
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/inventory/locations")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class WarehouseLocationController(
    private val warehouseLocationService: WarehouseLocationService,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('inventory:write')")
    fun createLocation(
        @CurrentOrganizationId orgId: UUID,
        @Valid @RequestBody request: CreateWarehouseLocationRequest,
    ): ResponseEntity<WarehouseLocationResponse> {
        val location = warehouseLocationService.createLocation(request, orgId)
        return ResponseEntity.status(HttpStatus.CREATED).body(location.toResponse())
    }

    @GetMapping
    @PreAuthorize("hasAuthority('inventory:read')")
    fun listLocations(
        @CurrentOrganizationId orgId: UUID,
        @RequestParam warehouseId: UUID,
    ): ResponseEntity<List<WarehouseLocationResponse>> {
        val locations = warehouseLocationService.listLocations(orgId, warehouseId)
        return ResponseEntity.ok(locations.map { it.toResponse() })
    }

    private fun WarehouseLocation.toResponse() =
        WarehouseLocationResponse(
            id = id,
            warehouseId = warehouseId,
            parentLocationId = parentLocationId,
            code = code,
            type = type,
            barcode = barcode,
        )
}
