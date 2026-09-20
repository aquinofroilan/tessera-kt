package com.aquinofroilan.tessera.domain.inventory.controller

import com.aquinofroilan.tessera.domain.inventory.model.ProductSerial
import com.aquinofroilan.tessera.domain.inventory.model.SerialStatus
import com.aquinofroilan.tessera.domain.inventory.repository.ProductSerialRepository
import com.aquinofroilan.tessera.security.CurrentOrganizationId
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/inventory/serials")
class ProductSerialController(
    private val productSerialRepository: ProductSerialRepository,
) {
    @GetMapping
    @PreAuthorize("hasAuthority('inventory:read')")
    fun listSerials(
        @CurrentOrganizationId orgId: UUID,
        @RequestParam productId: UUID,
        @RequestParam warehouseId: UUID,
    ): ResponseEntity<List<ProductSerialResponse>> {
        val serials =
            productSerialRepository.findByOrganizationIdAndProductIdAndStatusAndCurrentWarehouseId(
                orgId,
                productId,
                SerialStatus.IN_STOCK,
                warehouseId,
            )
        return ResponseEntity.ok(serials.map { it.toResponse() })
    }

    @GetMapping("/{serialNumber}")
    @PreAuthorize("hasAuthority('inventory:read')")
    fun getSerial(
        @CurrentOrganizationId orgId: UUID,
        @RequestParam productId: UUID,
        @PathVariable serialNumber: String,
    ): ResponseEntity<ProductSerialResponse> {
        val serial =
            productSerialRepository.findByOrganizationIdAndProductIdAndSerialNumber(orgId, productId, serialNumber)
                ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok(serial.toResponse())
    }

    private fun ProductSerial.toResponse() =
        ProductSerialResponse(
            id = id,
            productId = productId,
            serialNumber = serialNumber,
            status = status.name,
            currentWarehouseId = currentWarehouseId,
            lotNumber = lotNumber,
            createdAt = createdAt?.toString(),
            updatedAt = updatedAt?.toString(),
        )
}

data class ProductSerialResponse(
    val id: UUID,
    val productId: UUID,
    val serialNumber: String,
    val status: String,
    val currentWarehouseId: UUID?,
    val lotNumber: String?,
    val createdAt: String?,
    val updatedAt: String?,
)
