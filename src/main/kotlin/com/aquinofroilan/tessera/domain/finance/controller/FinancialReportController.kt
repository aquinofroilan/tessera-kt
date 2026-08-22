package com.aquinofroilan.tessera.domain.finance.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.domain.finance.service.FinancialReportService
import com.aquinofroilan.tessera.security.AuthenticationContext
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/finance/reports")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class FinancialReportController(
    private val financialReportService: FinancialReportService,
    private val authContext: AuthenticationContext,
) {
    @GetMapping("/trial-balance")
    @PreAuthorize("hasAuthority('journal:read')")
    fun getTrialBalance(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) asOfDate: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) compareAsOfDate: LocalDate?,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val report = financialReportService.getComparativeTrialBalance(orgId, asOfDate, compareAsOfDate)
        return ResponseEntity.ok(report)
    }

    @GetMapping("/income-statement")
    @PreAuthorize("hasAuthority('journal:read')")
    fun getIncomeStatement(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) compareStartDate: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) compareEndDate: LocalDate?,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val report =
            financialReportService.getIncomeStatement(
                organizationId = orgId,
                startDate = startDate,
                endDate = endDate,
                compareStartDate = compareStartDate,
                compareEndDate = compareEndDate,
            )
        return ResponseEntity.ok(report)
    }

    @GetMapping("/balance-sheet")
    @PreAuthorize("hasAuthority('journal:read')")
    fun getBalanceSheet(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) asOfDate: LocalDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) compareAsOfDate: LocalDate?,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val report =
            financialReportService.getBalanceSheet(
                organizationId = orgId,
                asOfDate = asOfDate,
                compareAsOfDate = compareAsOfDate,
            )
        return ResponseEntity.ok(report)
    }
}
