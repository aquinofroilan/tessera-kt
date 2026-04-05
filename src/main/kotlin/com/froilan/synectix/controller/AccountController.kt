package com.froilan.synectix.controller

import com.froilan.synectix.annotation.LogLevel
import com.froilan.synectix.annotation.Loggable
import com.froilan.synectix.dto.AccountResponse
import com.froilan.synectix.dto.CreateAccountRequest
import com.froilan.synectix.dto.UpdateAccountRequest
import com.froilan.synectix.model.Account
import com.froilan.synectix.model.AccountType
import com.froilan.synectix.model.User
import com.froilan.synectix.security.SessionContext
import com.froilan.synectix.service.AccountService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
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
@RequestMapping("/finance/accounts")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class AccountController(
    private val accountService: AccountService,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('account:create')")
    fun createAccount(
        @Valid @RequestBody request: CreateAccountRequest,
    ): ResponseEntity<Any> {
        val (_, sessionContext) = extractUserAndContext() ?: return unauthorized()

        return try {
            val account = accountService.createAccount(request, sessionContext.organizationId)
            ResponseEntity.status(HttpStatus.CREATED).body(account.toResponse())
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "Failed to create account")))
        }
    }

    @GetMapping
    @PreAuthorize("hasAuthority('account:read')")
    fun listAccounts(
        @RequestParam(required = false) type: String?,
        @RequestParam(required = false) parentId: String?,
    ): ResponseEntity<Any> {
        val (_, sessionContext) = extractUserAndContext() ?: return unauthorized()

        val accountType =
            if (type != null) {
                try {
                    AccountType.valueOf(type.uppercase())
                } catch (e: IllegalArgumentException) {
                    return ResponseEntity.badRequest().body(
                        mapOf("error" to "Invalid account type '$type'"),
                    )
                }
            } else {
                null
            }

        val accounts = accountService.listAccounts(sessionContext.organizationId, accountType, parentId)
        return ResponseEntity.ok(accounts.map { it.toResponse() })
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('account:read')")
    fun getAccount(
        @PathVariable id: String,
    ): ResponseEntity<Any> {
        val (_, sessionContext) = extractUserAndContext() ?: return unauthorized()

        return try {
            val account = accountService.getAccount(id, sessionContext.organizationId)
            ResponseEntity.ok(account.toResponse())
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("error" to (e.message ?: "Account not found")))
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('account:update')")
    fun updateAccount(
        @PathVariable id: String,
        @Valid @RequestBody request: UpdateAccountRequest,
    ): ResponseEntity<Any> {
        val (_, sessionContext) = extractUserAndContext() ?: return unauthorized()

        return try {
            val account = accountService.updateAccount(id, request, sessionContext.organizationId)
            ResponseEntity.ok(account.toResponse())
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "Failed to update account")))
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('account:delete')")
    fun deleteAccount(
        @PathVariable id: String,
    ): ResponseEntity<Any> {
        val (_, sessionContext) = extractUserAndContext() ?: return unauthorized()

        return try {
            accountService.deleteAccount(id, sessionContext.organizationId)
            ResponseEntity.ok(mapOf("message" to "Account deleted"))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "Failed to delete account")))
        }
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

    private fun extractUserAndContext(): Pair<User, SessionContext>? {
        val authentication = SecurityContextHolder.getContext().authentication
        val user = authentication?.principal as? User ?: return null
        val sessionContext = authentication.details as? SessionContext ?: return null
        return user to sessionContext
    }

    private fun unauthorized(): ResponseEntity<Any> =
        ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(mapOf("error" to "Authentication required"))
}
