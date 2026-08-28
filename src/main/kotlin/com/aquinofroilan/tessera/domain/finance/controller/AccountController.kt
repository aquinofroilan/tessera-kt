package com.aquinofroilan.tessera.domain.finance.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.domain.finance.dto.AccountResponse
import com.aquinofroilan.tessera.domain.finance.dto.CreateAccountRequest
import com.aquinofroilan.tessera.domain.finance.dto.UpdateAccountRequest
import com.aquinofroilan.tessera.domain.finance.model.Account
import com.aquinofroilan.tessera.domain.finance.model.AccountType
import com.aquinofroilan.tessera.domain.finance.service.AccountService
import com.aquinofroilan.tessera.domain.finance.service.JournalEntryService
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
import java.time.LocalDate
import java.util.Locale
import java.util.UUID

@RestController
@RequestMapping("/api/v1/finance/accounts")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class AccountController(
    private val accountService: AccountService,
    private val journalEntryService: JournalEntryService,
    private val authContext: AuthenticationContext,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('account:create')")
    fun createAccount(
        @CurrentOrganizationId orgId: UUID,
        @Valid @RequestBody request: CreateAccountRequest,
    ): ResponseEntity<Any> {
        val account = accountService.createAccount(request, orgId)
        return ResponseEntity.status(HttpStatus.CREATED).body(account.toResponse())
    }

    @GetMapping
    @PreAuthorize("hasAuthority('account:read')")
    fun listAccounts(
        @CurrentOrganizationId orgId: UUID,
        @RequestParam(required = false) type: String?,
        @RequestParam(required = false) parentId: java.util.UUID?,
    ): ResponseEntity<Any> {
        val accountType =
            if (type != null) {
                try {
                    AccountType.valueOf(type.uppercase(Locale.ROOT))
                } catch (e: IllegalArgumentException) {
                    return ResponseEntity.badRequest().body(
                        mapOf("error" to "Invalid account type '$type'"),
                    )
                }
            } else {
                null
            }

        val accounts = accountService.listAccounts(orgId, accountType, parentId)
        return ResponseEntity.ok(accounts.map { it.toResponse() })
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('account:read')")
    fun getAccount(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> {
        val account = accountService.getAccount(id, orgId)
        return ResponseEntity.ok(account.toResponse())
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('account:update')")
    fun updateAccount(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: java.util.UUID,
        @Valid @RequestBody request: UpdateAccountRequest,
    ): ResponseEntity<Any> {
        val account = accountService.updateAccount(id, request, orgId)
        return ResponseEntity.ok(account.toResponse())
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('account:delete')")
    fun deleteAccount(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> {
        accountService.deleteAccount(id, orgId)
        return ResponseEntity.ok(mapOf("message" to "Account deactivated"))
    }

    @GetMapping("/{id}/balance")
    @PreAuthorize("hasAuthority('account:read')")
    fun getAccountBalance(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: java.util.UUID,
        @RequestParam(required = false) asOfDate: LocalDate?,
    ): ResponseEntity<Any> {
        val balance = journalEntryService.getAccountBalance(id, orgId, asOfDate)
        return ResponseEntity.ok(balance)
    }

    private fun Account.toResponse() =
        AccountResponse(
            id = id,
            code = code,
            name = name,
            description = description,
            type = type.name,
            parentId = parentId,
            organizationId = organizationId,
            isActive = isActive,
            isSystemAccount = isSystemAccount,
            createdAt = createdAt?.toString(),
            updatedAt = updatedAt?.toString(),
        )
}
