package com.aquinofroilan.tessera.domain.finance.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.domain.finance.model.FiscalYear
import com.aquinofroilan.tessera.domain.finance.service.FiscalYearService
import com.aquinofroilan.tessera.domain.platform.dto.CreateFiscalYearRequest
import com.aquinofroilan.tessera.domain.platform.dto.FiscalPeriodResponse
import com.aquinofroilan.tessera.domain.platform.dto.FiscalYearResponse
import com.aquinofroilan.tessera.domain.platform.dto.FiscalYearSummaryResponse
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.security.CurrentOrganizationId
import com.aquinofroilan.tessera.security.CurrentUserId
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/finance/fiscal")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class FiscalYearController(
    private val fiscalYearService: FiscalYearService,
    private val authContext: AuthenticationContext,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('fiscal:create')")
    fun createFiscalYear(
        @CurrentOrganizationId orgId: UUID,
        @Valid @RequestBody request: CreateFiscalYearRequest,
    ): ResponseEntity<Any> {
        val fiscalYear = fiscalYearService.createFiscalYear(request, orgId)
        return ResponseEntity.status(HttpStatus.CREATED).body(fiscalYear.toResponse())
    }

    @GetMapping
    @PreAuthorize("hasAuthority('fiscal:read')")
    fun listFiscalYears(
        @CurrentOrganizationId orgId: UUID,
    ): ResponseEntity<Any> {
        val fiscalYears = fiscalYearService.listFiscalYears(orgId)
        return ResponseEntity.ok(fiscalYears.map { it.toSummaryResponse() })
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('fiscal:read')")
    fun getFiscalYear(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> {
        val fiscalYear = fiscalYearService.getFiscalYear(id, orgId)
        return ResponseEntity.ok(fiscalYear.toResponse())
    }

    @PostMapping("/{id}/periods/{periodId}/close")
    @PreAuthorize("hasAuthority('fiscal:close')")
    fun closePeriod(
        @CurrentUserId userId: UUID,
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: java.util.UUID,
        @PathVariable periodId: java.util.UUID,
    ): ResponseEntity<Any> {
        val fiscalYear = fiscalYearService.closePeriod(id, periodId, orgId, userId)
        return ResponseEntity.ok(fiscalYear.toResponse())
    }

    @PostMapping("/{id}/periods/{periodId}/reopen")
    @PreAuthorize("hasAuthority('fiscal:close')")
    fun reopenPeriod(
        @CurrentUserId userId: UUID,
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: java.util.UUID,
        @PathVariable periodId: java.util.UUID,
    ): ResponseEntity<Any> {
        val fiscalYear = fiscalYearService.reopenPeriod(id, periodId, orgId, userId)
        return ResponseEntity.ok(fiscalYear.toResponse())
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAuthority('fiscal:close')")
    fun closeYear(
        @CurrentUserId userId: UUID,
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> {
        val fiscalYear = fiscalYearService.closeYear(id, orgId, userId)
        return ResponseEntity.ok(fiscalYear.toResponse())
    }

    private fun FiscalYear.toResponse() =
        FiscalYearResponse(
            id = id,
            name = name,
            startDate = startDate.toString(),
            endDate = endDate.toString(),
            status = status.name,
            organizationId = organizationId,
            periods =
                periods.map { period ->
                    FiscalPeriodResponse(
                        id = period.id,
                        periodNumber = period.periodNumber,
                        name = period.name,
                        startDate = period.startDate.toString(),
                        endDate = period.endDate.toString(),
                        status = period.status.name,
                        closedAt = period.closedAt?.toString(),
                        closedBy = period.closedBy,
                        reopenedAt = period.reopenedAt?.toString(),
                        reopenedBy = period.reopenedBy,
                    )
                },
            closedAt = closedAt?.toString(),
            closedBy = closedBy,
            closingEntryId = closingEntryId,
            createdAt = createdAt?.toString(),
            updatedAt = updatedAt?.toString(),
        )

    private fun FiscalYear.toSummaryResponse() =
        FiscalYearSummaryResponse(
            id = id,
            name = name,
            startDate = startDate.toString(),
            endDate = endDate.toString(),
            status = status.name,
            organizationId = organizationId,
            createdAt = createdAt?.toString(),
            updatedAt = updatedAt?.toString(),
        )
}
