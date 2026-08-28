package com.aquinofroilan.tessera.domain.finance.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.domain.finance.dto.AutoMatchRequest
import com.aquinofroilan.tessera.domain.finance.dto.BankStatementResponse
import com.aquinofroilan.tessera.domain.finance.dto.ManualMatchRequest
import com.aquinofroilan.tessera.domain.finance.service.BankReconciliationService
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.security.CurrentOrganizationId
import com.aquinofroilan.tessera.security.CurrentUserId
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/api/v1/finance/reconciliation")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class BankReconciliationController(
    private val reconciliationService: BankReconciliationService,
    private val authContext: AuthenticationContext,
) {
    @PostMapping("/statements/{statementId}/auto-match")
    @PreAuthorize("hasAuthority('bank:approve')")
    fun autoMatch(
        @CurrentUserId userId: UUID,
        @CurrentOrganizationId orgId: UUID,
        @PathVariable statementId: java.util.UUID,
        @Valid @RequestBody(required = false) request: AutoMatchRequest?,
    ): ResponseEntity<Any> {
        val drift = request?.maxDateDriftDays ?: 5
        return ResponseEntity.ok(reconciliationService.autoMatch(statementId, orgId, userId, drift))
    }

    @PostMapping("/statements/{statementId}/lines/{lineId}/match")
    @PreAuthorize("hasAuthority('bank:approve')")
    fun manualMatch(
        @CurrentUserId userId: UUID,
        @CurrentOrganizationId orgId: UUID,
        @PathVariable statementId: java.util.UUID,
        @PathVariable lineId: java.util.UUID,
        @Valid @RequestBody request: ManualMatchRequest,
    ): ResponseEntity<Any> {
        val updated = reconciliationService.manualMatch(statementId, lineId, request.journalEntryId, orgId, userId)
        return ResponseEntity.ok(BankStatementResponse.from(updated))
    }

    @PostMapping("/statements/{statementId}/lines/{lineId}/unmatch")
    @PreAuthorize("hasAuthority('bank:approve')")
    fun unmatch(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable statementId: java.util.UUID,
        @PathVariable lineId: java.util.UUID,
    ): ResponseEntity<Any> {
        val updated = reconciliationService.unmatch(statementId, lineId, orgId)
        return ResponseEntity.ok(BankStatementResponse.from(updated))
    }

    @GetMapping("/bank-accounts/{bankAccountId}/summary")
    @PreAuthorize("hasAuthority('bank:read')")
    fun summary(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable bankAccountId: java.util.UUID,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) asOf: LocalDate?,
    ): ResponseEntity<Any> = ResponseEntity.ok(reconciliationService.summary(bankAccountId, orgId, asOf))
}
