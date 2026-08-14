package com.aquinofroilan.tessera.domain.finance.service

import com.aquinofroilan.tessera.domain.finance.dto.AutoMatchResponse
import com.aquinofroilan.tessera.domain.finance.dto.MatchedLineResponse
import com.aquinofroilan.tessera.domain.finance.dto.ReconciliationSummaryResponse
import com.aquinofroilan.tessera.domain.finance.model.BankStatement
import com.aquinofroilan.tessera.domain.finance.model.JournalEntry
import com.aquinofroilan.tessera.domain.finance.model.JournalEntryStatus
import com.aquinofroilan.tessera.domain.finance.repository.BankStatementRepository
import com.aquinofroilan.tessera.domain.finance.repository.JournalEntryRepository
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.abs

/**
 * Matches bank statement lines (what the bank says) against journal-entry
 * lines posting to the bank's GL account (what our books say). Auto-match
 * is conservative: it links a statement line to a single journal entry only
 * when there's exactly one POSTED journal entry within the date drift window
 * carrying the same signed amount against this bank account's GL. Anything
 * else surfaces as ambiguous or unmatched for a human to resolve via
 * [manualMatch] / [unmatch].
 */
@Service
class BankReconciliationService(
    private val statementRepository: BankStatementRepository,
    private val bankAccountService: BankAccountService,
    private val journalEntryRepository: JournalEntryRepository,
) {
    @Transactional
    fun autoMatch(
        statementId: java.util.UUID,
        organizationId: java.util.UUID,
        userId: java.util.UUID,
        maxDateDriftDays: Int,
    ): AutoMatchResponse {
        if (maxDateDriftDays < 0) throw BusinessRuleException("maxDateDriftDays must be >= 0")
        val statement = getStatement(statementId, organizationId)
        val bank = bankAccountService.getBankAccount(statement.bankAccountId, organizationId)

        val unreconciled = statement.lines.filter { !it.reconciled }
        if (unreconciled.isEmpty()) {
            return AutoMatchResponse(
                statementId = statement.id,
                matched = emptyList(),
                unmatchedLineIds = emptyList(),
                ambiguousLineIds = emptyList(),
            )
        }

        val earliest = unreconciled.minOf { it.postedDate }.minusDays(maxDateDriftDays.toLong())
        val latest = unreconciled.maxOf { it.postedDate }.plusDays(maxDateDriftDays.toLong())
        val candidates =
            journalEntryRepository
                .findByOrganizationIdAndStatusAndDateBetween(organizationId, JournalEntryStatus.POSTED, earliest, latest)
                .filter { je ->
                    je.lines.any { it.accountId == bank.glAccountId }
                }

        val alreadyMatched = statement.lines.mapNotNull { it.reconciledJournalEntryId }.toMutableSet()
        val matched = mutableListOf<MatchedLineResponse>()
        val unmatched = mutableListOf<java.util.UUID>()
        val ambiguous = mutableListOf<java.util.UUID>()

        val now = LocalDateTime.now()
        val updatedLines =
            statement.lines.toMutableList().also { mut ->
                unreconciled.forEach { line ->
                    val winners =
                        candidates.filter { je ->
                            je.id !in alreadyMatched &&
                                signedAmountFor(je, bank.glAccountId).compareTo(line.amount) == 0 &&
                                abs(ChronoUnit.DAYS.between(je.date, line.postedDate).toInt()) <= maxDateDriftDays
                        }
                    when (winners.size) {
                        0 -> unmatched.add(line.id)
                        1 -> {
                            val winner = winners.first()
                            alreadyMatched.add(winner.id)
                            val idx = mut.indexOfFirst { it.id == line.id }
                            mut[idx] =
                                line.copy(
                                    reconciled = true,
                                    reconciledJournalEntryId = winner.id,
                                    reconciledAt = now,
                                    reconciledBy = userId,
                                )
                            matched.add(
                                MatchedLineResponse(
                                    statementLineId = line.id,
                                    journalEntryId = winner.id,
                                    amount = line.amount,
                                    driftDays = abs(ChronoUnit.DAYS.between(winner.date, line.postedDate).toInt()),
                                ),
                            )
                        }
                        else -> ambiguous.add(line.id)
                    }
                }
            }
        statementRepository.save(statement.copy(lines = updatedLines))

        return AutoMatchResponse(
            statementId = statement.id,
            matched = matched,
            unmatchedLineIds = unmatched,
            ambiguousLineIds = ambiguous,
        )
    }

    @Transactional
    fun manualMatch(
        statementId: java.util.UUID,
        lineId: java.util.UUID,
        journalEntryId: java.util.UUID,
        organizationId: java.util.UUID,
        userId: java.util.UUID,
    ): BankStatement {
        val statement = getStatement(statementId, organizationId)
        val bank = bankAccountService.getBankAccount(statement.bankAccountId, organizationId)
        val line =
            statement.lines.firstOrNull { it.id == lineId }
                ?: throw ResourceNotFoundException("Statement line not found: $lineId")
        if (line.reconciled) throw BusinessRuleException("Statement line is already reconciled")

        val je =
            journalEntryRepository.findById(journalEntryId).orElseThrow {
                ResourceNotFoundException("Journal entry not found: $journalEntryId")
            }
        if (je.organizationId != organizationId) {
            throw ResourceNotFoundException("Journal entry not found: $journalEntryId")
        }
        if (je.status != JournalEntryStatus.POSTED) {
            throw BusinessRuleException("Cannot match against a ${je.status} journal entry")
        }
        val jeSignedAmount = signedAmountFor(je, bank.glAccountId)
        if (jeSignedAmount.signum() == 0) {
            throw BusinessRuleException("Journal entry does not post to this bank account's GL account")
        }
        if (jeSignedAmount.compareTo(line.amount) != 0) {
            throw BusinessRuleException(
                "Amount mismatch: statement line $${line.amount} vs journal entry $$jeSignedAmount",
            )
        }
        if (statement.lines.any { it.reconciledJournalEntryId == je.id }) {
            throw BusinessRuleException("Journal entry is already matched to another statement line")
        }

        val updated =
            statement.lines.map {
                if (it.id == lineId) {
                    it.copy(
                        reconciled = true,
                        reconciledJournalEntryId = je.id,
                        reconciledAt = LocalDateTime.now(),
                        reconciledBy = userId,
                    )
                } else {
                    it
                }
            }
        return statementRepository.save(statement.copy(lines = updated))
    }

    @Transactional
    fun unmatch(
        statementId: java.util.UUID,
        lineId: java.util.UUID,
        organizationId: java.util.UUID,
    ): BankStatement {
        val statement = getStatement(statementId, organizationId)
        val line =
            statement.lines.firstOrNull { it.id == lineId }
                ?: throw ResourceNotFoundException("Statement line not found: $lineId")
        if (!line.reconciled) throw BusinessRuleException("Statement line is not reconciled")
        val updated =
            statement.lines.map {
                if (it.id == lineId) {
                    it.copy(
                        reconciled = false,
                        reconciledJournalEntryId = null,
                        reconciledAt = null,
                        reconciledBy = null,
                    )
                } else {
                    it
                }
            }
        return statementRepository.save(statement.copy(lines = updated))
    }

    fun summary(
        bankAccountId: java.util.UUID,
        organizationId: java.util.UUID,
        asOfDate: LocalDate?,
    ): ReconciliationSummaryResponse {
        val bank = bankAccountService.getBankAccount(bankAccountId, organizationId)
        val asOf = asOfDate ?: LocalDate.now()
        val allStatements =
            statementRepository.findByOrganizationIdAndBankAccountIdOrderByStatementDateDesc(organizationId, bankAccountId)
        val latestStatement = allStatements.firstOrNull { !it.statementDate.isAfter(asOf) }
        val bankSide = latestStatement?.closingBalance ?: bank.openingBalance

        val postedEntries =
            journalEntryRepository
                .findByOrganizationIdAndStatusAndDateLessThanEqual(organizationId, JournalEntryStatus.POSTED, asOf)
        val glBalance =
            bank.openingBalance.add(
                postedEntries.fold(BigDecimal.ZERO) { acc, je ->
                    acc.add(signedAmountFor(je, bank.glAccountId))
                },
            )

        val unreconciledCount =
            allStatements
                .flatMap { it.lines }
                .count { !it.reconciled }
                .toLong()

        return ReconciliationSummaryResponse(
            bankAccountId = bank.id,
            glAccountId = bank.glAccountId,
            bankSideBalance = bankSide,
            glSideBalance = glBalance,
            variance = bankSide.subtract(glBalance),
            unreconciledLineCount = unreconciledCount,
            asOfDate = asOf,
        )
    }

    private fun signedAmountFor(
        je: JournalEntry,
        glAccountId: java.util.UUID,
    ): BigDecimal {
        // A debit to the bank GL increases cash (positive); a credit decreases (negative).
        var net = BigDecimal.ZERO
        je.lines.filter { it.accountId == glAccountId }.forEach { l ->
            net = net.add(l.debit).subtract(l.credit)
        }
        return net
    }

    private fun getStatement(
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
}
