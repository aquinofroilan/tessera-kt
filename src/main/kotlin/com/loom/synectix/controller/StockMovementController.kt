package com.loom.synectix.controller

import com.loom.synectix.annotation.LogLevel
import com.loom.synectix.annotation.Loggable
import com.loom.synectix.dto.CreateStockMovementRequest
import com.loom.synectix.dto.OnHandResponse
import com.loom.synectix.dto.StockMovementResponse
import com.loom.synectix.model.StockMovement
import com.loom.synectix.model.StockMovementType
import com.loom.synectix.security.AuthenticationContext
import com.loom.synectix.service.StockMovementService
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
@RequestMapping("/inventory")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class StockMovementController(
    private val stockMovementService: StockMovementService,
    private val authContext: AuthenticationContext,
) {
    @PostMapping("/movements")
    @PreAuthorize("hasAuthority('inventory:write')")
    fun createMovement(
        @Valid @RequestBody request: CreateStockMovementRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val userId = authContext.userId() ?: return authContext.unauthorized()
        val movement = stockMovementService.createMovement(request, orgId, userId)
        return ResponseEntity.status(HttpStatus.CREATED).body(movement.toResponse())
    }

    @GetMapping("/movements")
    @PreAuthorize("hasAuthority('inventory:read')")
    fun listMovements(
        @RequestParam(required = false) productId: String?,
        @RequestParam(required = false) warehouseId: String?,
        @RequestParam(required = false) type: StockMovementType?,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        from: LocalDateTime?,
        @RequestParam(required = false)
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        to: LocalDateTime?,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val movements = stockMovementService.listMovements(orgId, productId, warehouseId, type, from, to)
        return ResponseEntity.ok(movements.map { it.toResponse() })
    }

    @GetMapping("/stock-on-hand")
    @PreAuthorize("hasAuthority('inventory:read')")
    fun onHand(
        @RequestParam productId: String,
        @RequestParam warehouseId: String,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val qty = stockMovementService.onHand(orgId, productId, warehouseId)
        return ResponseEntity.ok(OnHandResponse(productId = productId, warehouseId = warehouseId, quantity = qty))
    }

    private fun StockMovement.toResponse() =
        StockMovementResponse(
            id = id,
            type = type,
            productId = productId,
            warehouseId = warehouseId,
            transferToWarehouseId = transferToWarehouseId,
            quantity = quantity,
            unitCost = unitCost,
            reference = reference,
            notes = notes,
            occurredAt = occurredAt.toString(),
            organizationId = organizationId,
            createdBy = createdBy,
            createdAt = createdAt?.toString(),
        )
}
