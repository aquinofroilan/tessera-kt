package com.froilan.synectix.service

import com.froilan.synectix.dto.CreateAccountRequest
import com.froilan.synectix.dto.UpdateAccountRequest
import com.froilan.synectix.model.Account
import com.froilan.synectix.model.AccountType
import com.froilan.synectix.repository.AccountRepository
import org.slf4j.LoggerFactory
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Locale

@Service
class AccountService(
    private val accountRepository: AccountRepository,
) {
    private val log = LoggerFactory.getLogger(AccountService::class.java)

    @Transactional
    fun createAccount(
        request: CreateAccountRequest,
        organizationId: String,
    ): Account {
        val type =
            try {
                AccountType.valueOf(request.type.uppercase(Locale.ROOT))
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException(
                    "Invalid account type '${request.type}'. Must be one of: ${AccountType.entries.joinToString()}",
                )
            }

        if (request.parentId != null) {
            val parent =
                accountRepository.findById(request.parentId).orElseThrow {
                    IllegalArgumentException("Parent account not found")
                }
            if (parent.organizationId != organizationId) {
                throw IllegalArgumentException("Parent account not found")
            }
            if (parent.type != type) {
                throw IllegalArgumentException("Parent account must be the same type")
            }
        }

        val account =
            Account(
                code = request.code,
                name = request.name,
                description = request.description,
                type = type,
                parentId = request.parentId,
                organizationId = organizationId,
            )
        return try {
            accountRepository.save(account)
        } catch (e: DuplicateKeyException) {
            throw IllegalArgumentException("Account code '${request.code}' already exists in this organization")
        }
    }

    @Transactional
    fun updateAccount(
        accountId: String,
        request: UpdateAccountRequest,
        organizationId: String,
    ): Account {
        val account =
            accountRepository.findById(accountId).orElseThrow {
                IllegalArgumentException("Account not found")
            }
        if (account.organizationId != organizationId) {
            throw IllegalArgumentException("Account not found")
        }
        if (request.name != null && request.name.isBlank()) {
            throw IllegalArgumentException("Account name must not be blank")
        }

        val updated =
            account.copy(
                name = request.name ?: account.name,
                description = request.description ?: account.description,
            )
        return accountRepository.save(updated)
    }

    fun getAccount(
        accountId: String,
        organizationId: String,
    ): Account {
        val account =
            accountRepository.findById(accountId).orElseThrow {
                IllegalArgumentException("Account not found")
            }
        if (account.organizationId != organizationId) {
            throw IllegalArgumentException("Account not found")
        }
        return account
    }

    fun listAccounts(
        organizationId: String,
        type: AccountType? = null,
        parentId: String? = null,
    ): List<Account> =
        when {
            type != null && parentId != null ->
                accountRepository.findByOrganizationIdAndTypeAndParentIdAndIsActive(organizationId, type, parentId, true)
            type != null -> accountRepository.findByOrganizationIdAndTypeAndIsActive(organizationId, type, true)
            parentId != null -> accountRepository.findByOrganizationIdAndParentIdAndIsActive(organizationId, parentId, true)
            else -> accountRepository.findByOrganizationIdAndIsActive(organizationId, true)
        }

    @Transactional
    fun deleteAccount(
        accountId: String,
        organizationId: String,
    ) {
        val account =
            accountRepository.findById(accountId).orElseThrow {
                IllegalArgumentException("Account not found")
            }
        if (account.organizationId != organizationId) {
            throw IllegalArgumentException("Account not found")
        }
        if (account.isSystemAccount) {
            throw IllegalArgumentException("System accounts cannot be deleted")
        }
        if (accountRepository.existsByOrganizationIdAndParentId(organizationId, accountId)) {
            throw IllegalArgumentException("Cannot delete account with child accounts")
        }
        accountRepository.save(account.copy(isActive = false))
    }

    @Transactional
    fun seedDefaultAccounts(organizationId: String) {
        val defaults =
            listOf(
                Account(code = "1000", name = "Cash", type = AccountType.ASSET, organizationId = organizationId, isSystemAccount = true),
                Account(
                    code = "1100",
                    name = "Accounts Receivable",
                    type = AccountType.ASSET,
                    organizationId = organizationId,
                    isSystemAccount = true,
                ),
                Account(
                    code = "1200",
                    name = "Inventory",
                    type = AccountType.ASSET,
                    organizationId = organizationId,
                    isSystemAccount = true,
                ),
                Account(
                    code = "1300",
                    name = "Prepaid Expenses",
                    type = AccountType.ASSET,
                    organizationId = organizationId,
                    isSystemAccount = true,
                ),
                Account(
                    code = "1500",
                    name = "Fixed Assets",
                    type = AccountType.ASSET,
                    organizationId = organizationId,
                    isSystemAccount = true,
                ),
                Account(
                    code = "2000",
                    name = "Accounts Payable",
                    type = AccountType.LIABILITY,
                    organizationId = organizationId,
                    isSystemAccount = true,
                ),
                Account(
                    code = "2100",
                    name = "Accrued Expenses",
                    type = AccountType.LIABILITY,
                    organizationId = organizationId,
                    isSystemAccount = true,
                ),
                Account(
                    code = "2200",
                    name = "Short-term Loans",
                    type = AccountType.LIABILITY,
                    organizationId = organizationId,
                    isSystemAccount = true,
                ),
                Account(
                    code = "2500",
                    name = "Long-term Debt",
                    type = AccountType.LIABILITY,
                    organizationId = organizationId,
                    isSystemAccount = true,
                ),
                Account(
                    code = "3000",
                    name = "Owner's Equity",
                    type = AccountType.EQUITY,
                    organizationId = organizationId,
                    isSystemAccount = true,
                ),
                Account(
                    code = "3100",
                    name = "Retained Earnings",
                    type = AccountType.EQUITY,
                    organizationId = organizationId,
                    isSystemAccount = true,
                ),
                Account(
                    code = "4000",
                    name = "Sales Revenue",
                    type = AccountType.REVENUE,
                    organizationId = organizationId,
                    isSystemAccount = true,
                ),
                Account(
                    code = "4100",
                    name = "Service Revenue",
                    type = AccountType.REVENUE,
                    organizationId = organizationId,
                    isSystemAccount = true,
                ),
                Account(
                    code = "4200",
                    name = "Other Income",
                    type = AccountType.REVENUE,
                    organizationId = organizationId,
                    isSystemAccount = true,
                ),
                Account(
                    code = "5000",
                    name = "Cost of Goods Sold",
                    type = AccountType.EXPENSE,
                    organizationId = organizationId,
                    isSystemAccount = true,
                ),
                Account(
                    code = "5100",
                    name = "Salaries & Wages",
                    type = AccountType.EXPENSE,
                    organizationId = organizationId,
                    isSystemAccount = true,
                ),
                Account(
                    code = "5200",
                    name = "Rent Expense",
                    type = AccountType.EXPENSE,
                    organizationId = organizationId,
                    isSystemAccount = true,
                ),
                Account(
                    code = "5300",
                    name = "Utilities",
                    type = AccountType.EXPENSE,
                    organizationId = organizationId,
                    isSystemAccount = true,
                ),
                Account(
                    code = "5400",
                    name = "Office Supplies",
                    type = AccountType.EXPENSE,
                    organizationId = organizationId,
                    isSystemAccount = true,
                ),
                Account(
                    code = "5500",
                    name = "Depreciation",
                    type = AccountType.EXPENSE,
                    organizationId = organizationId,
                    isSystemAccount = true,
                ),
            )
        try {
            accountRepository.saveAll(defaults)
            log.info("Seeded {} default accounts for org: {}", defaults.size, organizationId)
        } catch (e: DuplicateKeyException) {
            log.info("Default accounts already exist for org: {}. Skipping.", organizationId)
        }
    }
}
