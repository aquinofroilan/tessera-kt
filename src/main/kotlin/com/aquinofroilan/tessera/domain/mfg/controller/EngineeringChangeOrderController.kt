package com.aquinofroilan.tessera.domain.mfg.controller

import com.aquinofroilan.tessera.domain.mfg.dto.AddEcoItemRequest
import com.aquinofroilan.tessera.domain.mfg.dto.CreateEcoRequest
import com.aquinofroilan.tessera.domain.mfg.dto.EngineeringChangeOrderDto
import com.aquinofroilan.tessera.domain.mfg.service.EngineeringChangeOrderService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/manufacturing/ecos")
class EngineeringChangeOrderController(
    private val ecoService: EngineeringChangeOrderService,
) {
    @PostMapping
    fun createEco(
        @PathVariable organizationId: UUID,
        @RequestBody request: CreateEcoRequest,
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<EngineeringChangeOrderDto> {
        val requestedBy = UUID.fromString(jwt.subject)
        val eco = ecoService.createEco(organizationId, requestedBy, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(eco)
    }

    @PostMapping("/{ecoId}/items")
    fun addAffectedItem(
        @PathVariable organizationId: UUID,
        @PathVariable ecoId: UUID,
        @RequestBody request: AddEcoItemRequest,
    ): ResponseEntity<EngineeringChangeOrderDto> {
        val eco = ecoService.addAffectedItem(organizationId, ecoId, request)
        return ResponseEntity.ok(eco)
    }

    @PostMapping("/{ecoId}/submit")
    fun submitForReview(
        @PathVariable organizationId: UUID,
        @PathVariable ecoId: UUID,
    ): ResponseEntity<EngineeringChangeOrderDto> {
        val eco = ecoService.submitForReview(organizationId, ecoId)
        return ResponseEntity.ok(eco)
    }

    @PostMapping("/{ecoId}/approve")
    fun approveEco(
        @PathVariable organizationId: UUID,
        @PathVariable ecoId: UUID,
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<EngineeringChangeOrderDto> {
        val approvedBy = UUID.fromString(jwt.subject)
        val eco = ecoService.approveEco(organizationId, ecoId, approvedBy)
        return ResponseEntity.ok(eco)
    }

    @PostMapping("/{ecoId}/apply")
    fun applyEco(
        @PathVariable organizationId: UUID,
        @PathVariable ecoId: UUID,
        @AuthenticationPrincipal jwt: Jwt,
    ): ResponseEntity<EngineeringChangeOrderDto> {
        val appliedBy = UUID.fromString(jwt.subject)
        val eco = ecoService.applyEco(organizationId, ecoId, appliedBy)
        return ResponseEntity.ok(eco)
    }
}
