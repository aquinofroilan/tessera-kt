package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.CreateBankAccountRequest
import com.aquinofroilan.tessera.dto.UpdateBankAccountRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import com.aquinofroilan.tessera.model.AccountType
import com.aquinofroilan.tessera.model.BankAccount
import com.aquinofroilan.tessera.repository.BankAccountRepository
import com.aquinofroilan.tessera.repository.OrganizationRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
class BankAccountService(
    private val bankAccountRepository: BankAccountRepository,
    private val accountService: AccountService,
    private val organizationRepository: OrganizationRepository,
) {
    @Transactional
    fun createBankAccount(
        request: CreateBankAccountRequest,
        organizationId: String,
        userId: String,
    ): BankAccount {
        val code = request.code.trim().uppercase()
        if (code.isBlank()) throw BusinessRuleException("Code cannot be blank")
        bankAccountRepository.findByOrganizationIdAndCode(organizationId, code).ifPresent {
            throw BusinessRuleException("Bank account with code '$code' already exists")
        }
        val glAccount = accountService.getAccount(request.glAccountId, organizationId)
        if (glAccount.type != AccountType.ASSET) {
            throw BusinessRuleException("Bank account must link to an ASSET GL account, got ${glAccount.type}")
        }
        if (!glAccount.isActive) {
            throw BusinessRuleException("GL account '${glAccount.code}' is inactive")
        }
        val currency = request.currency?.uppercase() ?: orgCurrency(organizationId)
        val opening = request.openingBalance ?: BigDecimal.ZERO
        val bank =
            BankAccount(
                organizationId = organizationId,
                code = code,
                name = request.name.trim(),
                bankName = request.bankName?.trim(),
                accountNumberLast4 = request.accountNumberLast4,
                currency = currency,
                glAccountId = glAccount.id,
                openingBalance = opening,
                currentBalance = opening,
                notes = request.notes,
                createdBy = userId,
            )
        return try {
            bankAccountRepository.save(bank)
        } catch (e: DataIntegrityViolationException) {
            throw BusinessRuleException("Bank account with code '$code' already exists")
        }
    }

    fun getBankAccount(
        id: String,
        organizationId: String,
    ): BankAccount {
        val b =
            bankAccountRepository.findById(id).orElseThrow {
                ResourceNotFoundException("Bank account not found: $id")
            }
        if (b.organizationId != organizationId) {
            throw ResourceNotFoundException("Bank account not found: $id")
        }
        return b
    }

    fun listBankAccounts(
        organizationId: String,
        activeOnly: Boolean,
    ): List<BankAccount> =
        if (activeOnly) {
            bankAccountRepository.findByOrganizationIdAndIsActive(organizationId, true)
        } else {
            bankAccountRepository.findByOrganizationId(organizationId)
        }

    @Transactional
    fun updateBankAccount(
        id: String,
        request: UpdateBankAccountRequest,
        organizationId: String,
    ): BankAccount {
        val b = getBankAccount(id, organizationId)
        return bankAccountRepository.save(
            b.copy(
                name = request.name?.trim() ?: b.name,
                bankName = request.bankName?.trim() ?: b.bankName,
                accountNumberLast4 = request.accountNumberLast4 ?: b.accountNumberLast4,
                isActive = request.isActive ?: b.isActive,
                notes = request.notes ?: b.notes,
            ),
        )
    }

    @Transactional
    fun deactivateBankAccount(
        id: String,
        organizationId: String,
    ): BankAccount {
        val b = getBankAccount(id, organizationId)
        if (!b.isActive) throw BusinessRuleException("Bank account '${b.code}' is already inactive")
        return bankAccountRepository.save(b.copy(isActive = false))
    }

    /** Applies a signed delta to the cached current_balance. Used by statement import / reconciliation. */
    @Transactional
    fun applyBalanceDelta(
        id: String,
        organizationId: String,
        delta: BigDecimal,
    ): BankAccount {
        val b = getBankAccount(id, organizationId)
        return bankAccountRepository.save(b.copy(currentBalance = b.currentBalance.add(delta)))
    }

    private fun orgCurrency(organizationId: String): String =
        organizationRepository.findById(organizationId).map { it.baseCurrency }.orElse("USD")
}
