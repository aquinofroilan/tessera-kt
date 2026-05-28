package com.loom.synectix.controller

import com.loom.synectix.annotation.LogLevel
import com.loom.synectix.annotation.Loggable
import com.loom.synectix.dto.CreateFiscalYearRequest
import com.loom.synectix.dto.FiscalPeriodResponse
import com.loom.synectix.dto.FiscalYearResponse
import com.loom.synectix.dto.FiscalYearSummaryResponse
import com.loom.synectix.model.FiscalYear
import com.loom.synectix.security.AuthenticationContext
import com.loom.synectix.service.FiscalYearService
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

@RestController
@RequestMapping("/finance/fiscal")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class FiscalYearController(
    private val fiscalYearService: FiscalYearService,
    private val authContext: AuthenticationContext,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('fiscal:create')")
    fun createFiscalYear(
        @Valid @RequestBody request: CreateFiscalYearRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()

        val fiscalYear = fiscalYearService.createFiscalYear(request, orgId)
        return ResponseEntity.status(HttpStatus.CREATED).body(fiscalYear.toResponse())
    }

    @GetMapping
    @PreAuthorize("hasAuthority('fiscal:read')")
    fun listFiscalYears(): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val fiscalYears = fiscalYearService.listFiscalYears(orgId)
        return ResponseEntity.ok(fiscalYears.map { it.toSummaryResponse() })
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('fiscal:read')")
    fun getFiscalYear(
        @PathVariable id: String,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()

        val fiscalYear = fiscalYearService.getFiscalYear(id, orgId)
        return ResponseEntity.ok(fiscalYear.toResponse())
    }

    @PostMapping("/{id}/periods/{periodId}/close")
    @PreAuthorize("hasAuthority('fiscal:close')")
    fun closePeriod(
        @PathVariable id: String,
        @PathVariable periodId: String,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val userId = authContext.userId() ?: "api-key"

        val fiscalYear = fiscalYearService.closePeriod(id, periodId, orgId, userId)
        return ResponseEntity.ok(fiscalYear.toResponse())
    }

    @PostMapping("/{id}/periods/{periodId}/reopen")
    @PreAuthorize("hasAuthority('fiscal:close')")
    fun reopenPeriod(
        @PathVariable id: String,
        @PathVariable periodId: String,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val userId = authContext.userId() ?: "api-key"

        val fiscalYear = fiscalYearService.reopenPeriod(id, periodId, orgId, userId)
        return ResponseEntity.ok(fiscalYear.toResponse())
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAuthority('fiscal:close')")
    fun closeYear(
        @PathVariable id: String,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val userId = authContext.userId() ?: "api-key"

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
