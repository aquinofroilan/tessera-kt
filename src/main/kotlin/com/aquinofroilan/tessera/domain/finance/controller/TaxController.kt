package com.aquinofroilan.tessera.domain.finance.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.domain.finance.dto.CreateTaxGroupRequest
import com.aquinofroilan.tessera.domain.finance.dto.CreateTaxRateRequest
import com.aquinofroilan.tessera.domain.finance.dto.TaxGroupResponse
import com.aquinofroilan.tessera.domain.finance.dto.TaxRateResponse
import com.aquinofroilan.tessera.domain.finance.dto.UpdateTaxGroupRequest
import com.aquinofroilan.tessera.domain.finance.dto.UpdateTaxRateRequest
import com.aquinofroilan.tessera.domain.finance.model.TaxGroup
import com.aquinofroilan.tessera.domain.finance.model.TaxRate
import com.aquinofroilan.tessera.domain.finance.service.TaxGroupService
import com.aquinofroilan.tessera.domain.finance.service.TaxRateService
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
@RequestMapping("/api/v1/finance/tax")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class TaxController(
    private val taxRateService: TaxRateService,
    private val taxGroupService: TaxGroupService,
    private val authContext: AuthenticationContext,
) {
    @PostMapping("/rates")
    @PreAuthorize("hasAuthority('tax:create')")
    fun createTaxRate(
        @CurrentOrganizationId orgId: UUID,
        @Valid @RequestBody request: CreateTaxRateRequest,
    ): ResponseEntity<Any> {
        val taxRate = taxRateService.createTaxRate(request, orgId)
        return ResponseEntity.status(HttpStatus.CREATED).body(taxRate.toResponse())
    }

    @GetMapping("/rates")
    @PreAuthorize("hasAuthority('tax:read')")
    fun listTaxRates(
        @CurrentOrganizationId orgId: UUID,
        @RequestParam(required = false) active: Boolean?,
    ): ResponseEntity<Any> {
        val rates = taxRateService.listTaxRates(orgId, active ?: false)
        return ResponseEntity.ok(rates.map { it.toResponse() })
    }

    @GetMapping("/rates/{id}")
    @PreAuthorize("hasAuthority('tax:read')")
    fun getTaxRate(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> {
        val taxRate = taxRateService.getTaxRate(id, orgId)
        return ResponseEntity.ok(taxRate.toResponse())
    }

    @PutMapping("/rates/{id}")
    @PreAuthorize("hasAuthority('tax:create')")
    fun updateTaxRate(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: java.util.UUID,
        @Valid @RequestBody request: UpdateTaxRateRequest,
    ): ResponseEntity<Any> {
        val taxRate = taxRateService.updateTaxRate(id, request, orgId)
        return ResponseEntity.ok(taxRate.toResponse())
    }

    @DeleteMapping("/rates/{id}")
    @PreAuthorize("hasAuthority('tax:delete')")
    fun deleteTaxRate(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> {
        taxRateService.deleteTaxRate(id, orgId)
        return ResponseEntity.ok(mapOf("message" to "Tax rate deactivated"))
    }

    @PostMapping("/groups")
    @PreAuthorize("hasAuthority('tax:create')")
    fun createTaxGroup(
        @CurrentOrganizationId orgId: UUID,
        @Valid @RequestBody request: CreateTaxGroupRequest,
    ): ResponseEntity<Any> {
        val (taxGroup, rates) =
            taxGroupService.let {
                val group = it.createTaxGroup(request, orgId)
                it.getTaxGroupWithRates(group.id, orgId)
            }
        return ResponseEntity.status(HttpStatus.CREATED).body(taxGroup.toResponse(rates))
    }

    @GetMapping("/groups")
    @PreAuthorize("hasAuthority('tax:read')")
    fun listTaxGroups(
        @CurrentOrganizationId orgId: UUID,
        @RequestParam(required = false) active: Boolean?,
    ): ResponseEntity<Any> {
        val groups = taxGroupService.listTaxGroups(orgId, active ?: false)
        val allRateIds = groups.flatMap { it.taxRateIds }.distinct()
        val allRates =
            if (allRateIds.isNotEmpty()) {
                taxGroupService
                    .loadRatesByIds(allRateIds)
                    .filter { it.organizationId == orgId }
                    .associateBy { it.id }
            } else {
                emptyMap()
            }
        return ResponseEntity.ok(
            groups.map { group ->
                val rates = group.taxRateIds.mapNotNull { allRates[it] }
                group.toResponse(rates)
            },
        )
    }

    @GetMapping("/groups/{id}")
    @PreAuthorize("hasAuthority('tax:read')")
    fun getTaxGroup(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> {
        val (taxGroup, rates) = taxGroupService.getTaxGroupWithRates(id, orgId)
        return ResponseEntity.ok(taxGroup.toResponse(rates))
    }

    @PutMapping("/groups/{id}")
    @PreAuthorize("hasAuthority('tax:create')")
    fun updateTaxGroup(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: java.util.UUID,
        @Valid @RequestBody request: UpdateTaxGroupRequest,
    ): ResponseEntity<Any> {
        val updated = taxGroupService.updateTaxGroup(id, request, orgId)
        val (_, rates) = taxGroupService.getTaxGroupWithRates(updated.id, orgId)
        return ResponseEntity.ok(updated.toResponse(rates))
    }

    @DeleteMapping("/groups/{id}")
    @PreAuthorize("hasAuthority('tax:delete')")
    fun deleteTaxGroup(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> {
        taxGroupService.deleteTaxGroup(id, orgId)
        return ResponseEntity.ok(mapOf("message" to "Tax group deactivated"))
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('tax:read')")
    fun getTaxSummary(
        @CurrentOrganizationId orgId: UUID,
        @RequestParam startDate: java.time.LocalDate,
        @RequestParam endDate: java.time.LocalDate,
    ): ResponseEntity<Any> {
        val summary = taxGroupService.getTaxSummary(orgId, startDate, endDate)
        return ResponseEntity.ok(summary)
    }

    private fun TaxRate.toResponse() =
        TaxRateResponse(
            id = id,
            name = name,
            code = code,
            percentage = percentage,
            authority = authority,
            organizationId = organizationId,
            isActive = isActive,
            createdAt = createdAt?.toString(),
            updatedAt = updatedAt?.toString(),
        )

    private fun TaxGroup.toResponse(rates: List<TaxRate>) =
        TaxGroupResponse(
            id = id,
            name = name,
            code = code,
            taxRates = rates.map { it.toResponse() },
            combinedRate = combinedRate,
            organizationId = organizationId,
            isActive = isActive,
            createdAt = createdAt?.toString(),
            updatedAt = updatedAt?.toString(),
        )
}
