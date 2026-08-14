package com.aquinofroilan.tessera.domain.finance.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.domain.finance.dto.BankStatementResponse
import com.aquinofroilan.tessera.domain.finance.dto.ImportStatementRequest
import com.aquinofroilan.tessera.domain.finance.service.BankStatementService
import com.aquinofroilan.tessera.security.AuthenticationContext
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/finance/bank-statements")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class BankStatementController(
    private val statementService: BankStatementService,
    private val authContext: AuthenticationContext,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('bank:write')")
    fun importStatement(
        @Valid @RequestBody request: ImportStatementRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val userId = authContext.userId() ?: return authContext.unauthorized()
        val s = statementService.importStatement(request, orgId, userId)
        return ResponseEntity.status(HttpStatus.CREATED).body(BankStatementResponse.from(s))
    }

    @GetMapping
    @PreAuthorize("hasAuthority('bank:read')")
    fun list(
        @RequestParam(required = false) bankAccountId: java.util.UUID?,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(statementService.listStatements(orgId, bankAccountId).map { BankStatementResponse.from(it) })
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('bank:read')")
    fun get(
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(BankStatementResponse.from(statementService.getStatement(id, orgId)))
    }
}
