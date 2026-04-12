package com.froilan.synectix.controller

import com.froilan.synectix.annotation.LogLevel
import com.froilan.synectix.annotation.Loggable
import com.froilan.synectix.dto.CreateTaxGroupRequest
import com.froilan.synectix.dto.CreateTaxRateRequest
import com.froilan.synectix.dto.TaxGroupResponse
import com.froilan.synectix.dto.TaxRateResponse
import com.froilan.synectix.dto.UpdateTaxGroupRequest
import com.froilan.synectix.dto.UpdateTaxRateRequest
import com.froilan.synectix.model.TaxGroup
import com.froilan.synectix.model.TaxRate
import com.froilan.synectix.security.AuthenticationContext
import com.froilan.synectix.service.TaxGroupService
import com.froilan.synectix.service.TaxRateService
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

@RestController
@RequestMapping("/finance/tax")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class TaxController(
    private val taxRateService: TaxRateService,
    private val taxGroupService: TaxGroupService,
    private val authContext: AuthenticationContext,
) {
    @PostMapping("/rates")
    @PreAuthorize("hasAuthority('tax:create')")
    fun createTaxRate(
        @Valid @RequestBody request: CreateTaxRateRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val taxRate = taxRateService.createTaxRate(request, orgId)
        return ResponseEntity.status(HttpStatus.CREATED).body(taxRate.toResponse())
    }

    @GetMapping("/rates")
    @PreAuthorize("hasAuthority('tax:read')")
    fun listTaxRates(
        @RequestParam(required = false) active: Boolean?,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val rates = taxRateService.listTaxRates(orgId, active ?: false)
        return ResponseEntity.ok(rates.map { it.toResponse() })
    }

    @GetMapping("/rates/{id}")
    @PreAuthorize("hasAuthority('tax:read')")
    fun getTaxRate(
        @PathVariable id: String,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val taxRate = taxRateService.getTaxRate(id, orgId)
        return ResponseEntity.ok(taxRate.toResponse())
    }

    @PutMapping("/rates/{id}")
    @PreAuthorize("hasAuthority('tax:create')")
    fun updateTaxRate(
        @PathVariable id: String,
        @Valid @RequestBody request: UpdateTaxRateRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val taxRate = taxRateService.updateTaxRate(id, request, orgId)
        return ResponseEntity.ok(taxRate.toResponse())
    }

    @DeleteMapping("/rates/{id}")
    @PreAuthorize("hasAuthority('tax:delete')")
    fun deleteTaxRate(
        @PathVariable id: String,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        taxRateService.deleteTaxRate(id, orgId)
        return ResponseEntity.ok(mapOf("message" to "Tax rate deactivated"))
    }

    @PostMapping("/groups")
    @PreAuthorize("hasAuthority('tax:create')")
    fun createTaxGroup(
        @Valid @RequestBody request: CreateTaxGroupRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
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
        @RequestParam(required = false) active: Boolean?,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val groups = taxGroupService.listTaxGroups(orgId, active ?: false)
        val allRateIds = groups.flatMap { it.taxRateIds }.distinct()
        val allRates = taxRateService.listTaxRates(orgId).associateBy { it.id }
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
        @PathVariable id: String,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val (taxGroup, rates) = taxGroupService.getTaxGroupWithRates(id, orgId)
        return ResponseEntity.ok(taxGroup.toResponse(rates))
    }

    @PutMapping("/groups/{id}")
    @PreAuthorize("hasAuthority('tax:create')")
    fun updateTaxGroup(
        @PathVariable id: String,
        @Valid @RequestBody request: UpdateTaxGroupRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val updated = taxGroupService.updateTaxGroup(id, request, orgId)
        val (_, rates) = taxGroupService.getTaxGroupWithRates(updated.id, orgId)
        return ResponseEntity.ok(updated.toResponse(rates))
    }

    @DeleteMapping("/groups/{id}")
    @PreAuthorize("hasAuthority('tax:delete')")
    fun deleteTaxGroup(
        @PathVariable id: String,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        taxGroupService.deleteTaxGroup(id, orgId)
        return ResponseEntity.ok(mapOf("message" to "Tax group deactivated"))
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority('tax:read')")
    fun getTaxSummary(
        @RequestParam startDate: java.time.LocalDate,
        @RequestParam endDate: java.time.LocalDate,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
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
