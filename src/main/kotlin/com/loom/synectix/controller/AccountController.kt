package com.loom.synectix.controller

import com.loom.synectix.annotation.LogLevel
import com.loom.synectix.annotation.Loggable
import com.loom.synectix.dto.AccountResponse
import com.loom.synectix.dto.CreateAccountRequest
import com.loom.synectix.dto.UpdateAccountRequest
import com.loom.synectix.model.Account
import com.loom.synectix.model.AccountType
import com.loom.synectix.security.AuthenticationContext
import com.loom.synectix.service.AccountService
import com.loom.synectix.service.JournalEntryService
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

@RestController
@RequestMapping("/finance/accounts")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class AccountController(
    private val accountService: AccountService,
    private val journalEntryService: JournalEntryService,
    private val authContext: AuthenticationContext,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('account:create')")
    fun createAccount(
        @Valid @RequestBody request: CreateAccountRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()

        val account = accountService.createAccount(request, orgId)
        return ResponseEntity.status(HttpStatus.CREATED).body(account.toResponse())
    }

    @GetMapping
    @PreAuthorize("hasAuthority('account:read')")
    fun listAccounts(
        @RequestParam(required = false) type: String?,
        @RequestParam(required = false) parentId: String?,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()

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
        @PathVariable id: String,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()

        val account = accountService.getAccount(id, orgId)
        return ResponseEntity.ok(account.toResponse())
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('account:update')")
    fun updateAccount(
        @PathVariable id: String,
        @Valid @RequestBody request: UpdateAccountRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()

        val account = accountService.updateAccount(id, request, orgId)
        return ResponseEntity.ok(account.toResponse())
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('account:delete')")
    fun deleteAccount(
        @PathVariable id: String,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()

        accountService.deleteAccount(id, orgId)
        return ResponseEntity.ok(mapOf("message" to "Account deactivated"))
    }

    @GetMapping("/{id}/balance")
    @PreAuthorize("hasAuthority('account:read')")
    fun getAccountBalance(
        @PathVariable id: String,
        @RequestParam(required = false) asOfDate: LocalDate?,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()

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
