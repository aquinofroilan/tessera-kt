package com.aquinofroilan.tessera.domain.inventory.controller

import com.aquinofroilan.tessera.domain.inventory.dto.PlaceHoldRequest
import com.aquinofroilan.tessera.domain.inventory.model.InventoryHold
import com.aquinofroilan.tessera.domain.inventory.repository.InventoryHoldRepository
import com.aquinofroilan.tessera.domain.inventory.service.InventoryHoldService
import com.aquinofroilan.tessera.security.CurrentOrganizationId
import com.aquinofroilan.tessera.security.CurrentUserId
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/inventory/holds")
class InventoryHoldController(
    private val inventoryHoldService: InventoryHoldService,
    private val inventoryHoldRepository: InventoryHoldRepository,
) {
    @PostMapping
    fun placeHold(
        @RequestBody request: PlaceHoldRequest,
        @CurrentOrganizationId organizationId: UUID,
        @CurrentUserId userId: UUID,
    ): InventoryHold = inventoryHoldService.placeOnHold(request, organizationId, userId)

    @PostMapping("/{id}/release")
    fun releaseHold(
        @PathVariable id: UUID,
        @CurrentOrganizationId organizationId: UUID,
        @CurrentUserId userId: UUID,
    ): InventoryHold = inventoryHoldService.releaseHold(id, organizationId, userId)

    @GetMapping
    fun listHolds(
        @RequestParam(required = false) productId: UUID?,
        @CurrentOrganizationId organizationId: UUID,
        pageable: Pageable,
    ): Page<InventoryHold> =
        if (productId != null) {
            inventoryHoldRepository.findByOrganizationIdAndProductId(organizationId, productId, pageable)
        } else {
            inventoryHoldRepository.findByOrganizationId(organizationId, pageable)
        }
}
