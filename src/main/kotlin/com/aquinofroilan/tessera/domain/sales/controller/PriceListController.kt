package com.aquinofroilan.tessera.domain.sales.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.domain.sales.dto.BatchSetPriceListLinesRequest
import com.aquinofroilan.tessera.domain.sales.dto.CreatePriceListLineRequest
import com.aquinofroilan.tessera.domain.sales.dto.CreatePriceListRequest
import com.aquinofroilan.tessera.domain.sales.dto.PriceListLineDto
import com.aquinofroilan.tessera.domain.sales.dto.PriceListResponse
import com.aquinofroilan.tessera.domain.sales.dto.UpdatePriceListRequest
import com.aquinofroilan.tessera.domain.sales.model.CustomerSegment
import com.aquinofroilan.tessera.domain.sales.service.PriceListService
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
@RequestMapping("/api/v1/sales/price-lists")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class PriceListController(
    private val priceListService: PriceListService,
) {
    @GetMapping
    @PreAuthorize("hasAuthority('sales:read') or hasAuthority('organization:read') or hasRole('SUPER_ADMIN')")
    fun listPriceLists(
        @CurrentOrganizationId orgId: UUID,
        @RequestParam(required = false) currency: String?,
        @RequestParam(required = false) customerSegment: CustomerSegment?,
        @RequestParam(required = false) isActive: Boolean?,
    ): ResponseEntity<List<PriceListResponse>> =
        ResponseEntity.ok(priceListService.listPriceLists(orgId, currency, customerSegment, isActive))

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('sales:read') or hasAuthority('organization:read') or hasRole('SUPER_ADMIN')")
    fun getPriceList(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: UUID,
    ): ResponseEntity<PriceListResponse> = ResponseEntity.ok(priceListService.getPriceList(id, orgId))

    @PostMapping
    @PreAuthorize("hasAuthority('sales:create') or hasAuthority('organization:write') or hasRole('SUPER_ADMIN')")
    fun createPriceList(
        @CurrentOrganizationId orgId: UUID,
        @Valid @RequestBody request: CreatePriceListRequest,
    ): ResponseEntity<PriceListResponse> {
        val created = priceListService.createPriceList(orgId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('sales:create') or hasAuthority('organization:write') or hasRole('SUPER_ADMIN')")
    fun updatePriceList(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdatePriceListRequest,
    ): ResponseEntity<PriceListResponse> = ResponseEntity.ok(priceListService.updatePriceList(id, orgId, request))

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('sales:create') or hasAuthority('organization:write') or hasRole('SUPER_ADMIN')")
    fun deletePriceList(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: UUID,
    ): ResponseEntity<Void> {
        priceListService.deletePriceList(id, orgId)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{id}/lines")
    @PreAuthorize("hasAuthority('sales:create') or hasAuthority('organization:write') or hasRole('SUPER_ADMIN')")
    fun addOrUpdateLine(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: UUID,
        @Valid @RequestBody request: CreatePriceListLineRequest,
    ): ResponseEntity<PriceListLineDto> {
        val line = priceListService.addOrUpdateLine(id, orgId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(line)
    }

    @DeleteMapping("/{id}/lines/{lineId}")
    @PreAuthorize("hasAuthority('sales:create') or hasAuthority('organization:write') or hasRole('SUPER_ADMIN')")
    fun deleteLine(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: UUID,
        @PathVariable lineId: UUID,
    ): ResponseEntity<Void> {
        priceListService.deleteLine(id, orgId, lineId)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{id}/lines/batch")
    @PreAuthorize("hasAuthority('sales:create') or hasAuthority('organization:write') or hasRole('SUPER_ADMIN')")
    fun batchSetLines(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: UUID,
        @Valid @RequestBody request: BatchSetPriceListLinesRequest,
    ): ResponseEntity<PriceListResponse> = ResponseEntity.ok(priceListService.batchSetLines(id, orgId, request))
}
