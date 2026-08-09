package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.ImportStatementRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import com.aquinofroilan.tessera.model.BankStatement
import com.aquinofroilan.tessera.model.BankStatementLine
import com.aquinofroilan.tessera.repository.BankStatementRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
class BankStatementService(
    private val statementRepository: BankStatementRepository,
    private val bankAccountService: BankAccountService,
) {
    @Transactional
    fun importStatement(
        request: ImportStatementRequest,
        organizationId: java.util.UUID,
        userId: java.util.UUID,
    ): BankStatement {
        val statementDate = request.statementDate ?: throw BusinessRuleException("statementDate is required")
        val opening = request.openingBalance ?: throw BusinessRuleException("openingBalance is required")
        val closing = request.closingBalance ?: throw BusinessRuleException("closingBalance is required")
        if (request.lines.isEmpty()) throw BusinessRuleException("Statement must have at least one line")

        val bankAccount = bankAccountService.getBankAccount(request.bankAccountId, organizationId)
        if (!bankAccount.isActive) {
            throw BusinessRuleException("Bank account '${bankAccount.code}' is inactive")
        }

        val lines =
            request.lines.mapIndexed { index, lineReq ->
                val posted = lineReq.postedDate ?: throw BusinessRuleException("Line ${index + 1}: postedDate is required")
                val amount = lineReq.amount ?: throw BusinessRuleException("Line ${index + 1}: amount is required")
                if (amount.signum() == 0) {
                    throw BusinessRuleException("Line ${index + 1}: amount cannot be zero")
                }
                BankStatementLine(
                    lineNumber = index + 1,
                    postedDate = posted,
                    description = lineReq.description.trim(),
                    reference = lineReq.reference?.trim(),
                    amount = amount,
                )
            }

        val sumOfLines = lines.fold(BigDecimal.ZERO) { acc, l -> acc.add(l.amount) }
        val expectedClosing = opening.add(sumOfLines)
        if (expectedClosing.compareTo(closing) != 0) {
            throw BusinessRuleException(
                "Statement does not balance: opening $opening + sum(lines) $sumOfLines = $expectedClosing, declared closing = $closing",
            )
        }

        val source = (request.source ?: "CSV").uppercase()
        if (source !in setOf("CSV", "OFX", "MANUAL")) {
            throw BusinessRuleException("Unsupported source '$source'")
        }

        val saved =
            statementRepository.save(
                BankStatement(
                    organizationId = organizationId,
                    bankAccountId = bankAccount.id,
                    statementDate = statementDate,
                    openingBalance = opening,
                    closingBalance = closing,
                    currency = bankAccount.currency,
                    source = source,
                    uploadedBy = userId,
                    notes = request.notes,
                    lines = lines,
                ),
            )

        // Update cached current_balance on the bank account by the net of this statement.
        // Reconciliation will re-derive balances against the GL; this is a fast approximation
        // so /finance/bank-accounts list reflects the latest known position.
        bankAccountService.applyBalanceDelta(bankAccount.id, organizationId, sumOfLines)
        return saved
    }

    fun getStatement(
        id: java.util.UUID,
        organizationId: java.util.UUID,
    ): BankStatement {
        val s =
            statementRepository.findById(id).orElseThrow {
                ResourceNotFoundException("Statement not found: $id")
            }
        if (s.organizationId != organizationId) {
            throw ResourceNotFoundException("Statement not found: $id")
        }
        return s
    }

    fun listStatements(
        organizationId: java.util.UUID,
        bankAccountId: java.util.UUID?,
    ): List<BankStatement> =
        if (bankAccountId != null) {
            statementRepository.findByOrganizationIdAndBankAccountIdOrderByStatementDateDesc(organizationId, bankAccountId)
        } else {
            statementRepository.findByOrganizationId(organizationId)
        }
}
