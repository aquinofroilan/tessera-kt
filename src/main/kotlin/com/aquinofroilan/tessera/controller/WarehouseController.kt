package com.aquinofroilan.tessera.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.dto.CreateWarehouseRequest
import com.aquinofroilan.tessera.dto.UpdateWarehouseRequest
import com.aquinofroilan.tessera.dto.WarehouseResponse
import com.aquinofroilan.tessera.model.Warehouse
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.service.WarehouseService
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

@RestController
@RequestMapping("/inventory/warehouses")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class WarehouseController(
    private val warehouseService: WarehouseService,
    private val authContext: AuthenticationContext,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('inventory:write')")
    fun createWarehouse(
        @Valid @RequestBody request: CreateWarehouseRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val warehouse = warehouseService.createWarehouse(request, orgId)
        return ResponseEntity.status(HttpStatus.CREATED).body(warehouse.toResponse())
    }

    @GetMapping
    @PreAuthorize("hasAuthority('inventory:read')")
    fun listWarehouses(
        @RequestParam(required = false, defaultValue = "true") isActive: Boolean,
        @RequestParam(required = false) search: String?,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val warehouses = warehouseService.listWarehouses(orgId, isActive, search)
        return ResponseEntity.ok(warehouses.map { it.toResponse() })
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('inventory:read')")
    fun getWarehouse(
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val warehouse = warehouseService.getWarehouse(id, orgId)
        return ResponseEntity.ok(warehouse.toResponse())
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('inventory:write')")
    fun updateWarehouse(
        @PathVariable id: java.util.UUID,
        @Valid @RequestBody request: UpdateWarehouseRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val warehouse = warehouseService.updateWarehouse(id, request, orgId)
        return ResponseEntity.ok(warehouse.toResponse())
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('inventory:write')")
    fun deleteWarehouse(
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val warehouse = warehouseService.deleteWarehouse(id, orgId)
        return ResponseEntity.ok(warehouse.toResponse())
    }

    private fun Warehouse.toResponse() =
        WarehouseResponse(
            id = id,
            code = code,
            name = name,
            description = description,
            addressLine = addressLine,
            city = city,
            country = country,
            allowNegativeStock = allowNegativeStock,
            organizationId = organizationId,
            isActive = isActive,
            createdAt = createdAt?.toString(),
            updatedAt = updatedAt?.toString(),
        )
}
