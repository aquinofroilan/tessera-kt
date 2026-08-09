package com.aquinofroilan.tessera.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.dto.BankAccountResponse
import com.aquinofroilan.tessera.dto.CreateBankAccountRequest
import com.aquinofroilan.tessera.dto.UpdateBankAccountRequest
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.service.BankAccountService
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
@RequestMapping("/finance/bank-accounts")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class BankAccountController(
    private val bankAccountService: BankAccountService,
    private val authContext: AuthenticationContext,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('bank:write')")
    fun create(
        @Valid @RequestBody request: CreateBankAccountRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val userId = authContext.userId() ?: return authContext.unauthorized()
        val b = bankAccountService.createBankAccount(request, orgId, userId)
        return ResponseEntity.status(HttpStatus.CREATED).body(BankAccountResponse.from(b))
    }

    @GetMapping
    @PreAuthorize("hasAuthority('bank:read')")
    fun list(
        @RequestParam(required = false, defaultValue = "true") activeOnly: Boolean,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(bankAccountService.listBankAccounts(orgId, activeOnly).map { BankAccountResponse.from(it) })
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('bank:read')")
    fun get(
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(BankAccountResponse.from(bankAccountService.getBankAccount(id, orgId)))
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('bank:write')")
    fun update(
        @PathVariable id: java.util.UUID,
        @Valid @RequestBody request: UpdateBankAccountRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(BankAccountResponse.from(bankAccountService.updateBankAccount(id, request, orgId)))
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('bank:write')")
    fun deactivate(
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(BankAccountResponse.from(bankAccountService.deactivateBankAccount(id, orgId)))
    }
}
