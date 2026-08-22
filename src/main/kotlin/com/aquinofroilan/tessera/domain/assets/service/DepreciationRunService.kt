package com.aquinofroilan.tessera.domain.assets.service

import com.aquinofroilan.tessera.domain.assets.model.AssetDepreciationRun
import com.aquinofroilan.tessera.domain.assets.model.AssetDepreciationRunLine
import com.aquinofroilan.tessera.domain.assets.model.AssetStatus
import com.aquinofroilan.tessera.domain.assets.model.DepreciationRunStatus
import com.aquinofroilan.tessera.domain.assets.repository.AssetDepreciationRunLineRepository
import com.aquinofroilan.tessera.domain.assets.repository.AssetDepreciationRunRepository
import com.aquinofroilan.tessera.domain.assets.repository.FixedAssetRepository
import com.aquinofroilan.tessera.domain.finance.model.JournalEntryLine
import com.aquinofroilan.tessera.domain.finance.repository.AccountRepository
import com.aquinofroilan.tessera.domain.finance.service.JournalEntryService
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneOffset
import java.util.UUID

/**
 * Monthly depreciation runs. The lifecycle is DRAFT → POSTED:
 *
 * - **createRun** is idempotent for a given (org, year, month): if a DRAFT
 *   already exists, its lines are recomputed from current asset state.
 *   POSTED runs for the same period are immovable — rejected.
 * - **postRun** creates a single system JournalEntry aggregating the
 *   period's lines by (debit, credit) account pair, updates each asset's
 *   accumulated_depreciation, and flips the asset to FULLY_DEPRECIATED
 *   once it hits its salvage floor.
 *
 * Lines whose asset is missing either GL account reference are still saved
 * to the run for visibility, but skipped in the JE — admins fix the asset
 * and re-create the draft.
 */
@Service
class DepreciationRunService(
    private val runRepository: AssetDepreciationRunRepository,
    private val lineRepository: AssetDepreciationRunLineRepository,
    private val fixedAssetRepository: FixedAssetRepository,
    private val calculator: DepreciationCalculator,
    private val journalEntryService: JournalEntryService,
    private val accountRepository: AccountRepository,
) {
    private val log = LoggerFactory.getLogger(DepreciationRunService::class.java)

    @Transactional
    fun createRun(
        organizationId: UUID,
        year: Int,
        month: Int,
    ): AssetDepreciationRun {
        if (month !in 1..12) throw BusinessRuleException("Month must be between 1 and 12")
        val period = YearMonth.of(year, month)

        val existing =
            runRepository
                .findByOrganizationIdAndPeriodYearAndPeriodMonth(organizationId, year, month)
                .orElse(null)

        if (existing != null && existing.status == DepreciationRunStatus.POSTED) {
            throw BusinessRuleException("Depreciation run for $year-$month is already posted")
        }

        val assets =
            fixedAssetRepository
                .findByOrganizationIdAndStatus(organizationId, AssetStatus.ACTIVE)

        val draftLines =
            assets
                .map { asset -> asset to calculator.monthlyDepreciation(asset, period) }
                .filter { (_, amount) -> amount.signum() > 0 }
                .map { (asset, amount) ->
                    AssetDepreciationRunLine(
                        runId = existing?.id ?: PLACEHOLDER_RUN_ID,
                        assetId = asset.id,
                        depreciationAmount = amount,
                        debitAccountId = asset.depreciationExpenseAccountId?.let { UUID.fromString(it) },
                        creditAccountId = asset.accumulatedDepreciationAccountId?.let { UUID.fromString(it) },
                    )
                }

        val total = draftLines.fold(BigDecimal.ZERO) { acc, line -> acc.add(line.depreciationAmount) }

        val run =
            if (existing != null) {
                lineRepository.deleteByRunId(existing.id)
                runRepository.save(existing.copy(totalDepreciation = total))
            } else {
                runRepository.save(
                    AssetDepreciationRun(
                        organizationId = organizationId,
                        periodYear = year,
                        periodMonth = month,
                        totalDepreciation = total,
                    ),
                )
            }

        if (draftLines.isNotEmpty()) {
            lineRepository.saveAll(draftLines.map { it.copy(runId = run.id) })
        }
        return run
    }

    fun listRuns(organizationId: UUID): List<AssetDepreciationRun> =
        runRepository.findByOrganizationIdOrderByPeriodYearDescPeriodMonthDesc(organizationId)

    fun getRun(
        id: UUID,
        organizationId: UUID,
    ): AssetDepreciationRun {
        val run =
            runRepository.findById(id).orElseThrow {
                ResourceNotFoundException("Depreciation run $id not found")
            }
        if (run.organizationId != organizationId) {
            throw ResourceNotFoundException("Depreciation run $id not found")
        }
        return run
    }

    fun listLines(runId: UUID): List<AssetDepreciationRunLine> = lineRepository.findByRunId(runId)

    @Transactional
    fun postRun(
        id: UUID,
        organizationId: UUID,
        postedBy: UUID,
    ): AssetDepreciationRun {
        val run = getRun(id, organizationId)
        if (run.status != DepreciationRunStatus.DRAFT) {
            throw BusinessRuleException("Only draft depreciation runs can be posted")
        }

        val lines = lineRepository.findByRunId(run.id)
        if (lines.isEmpty()) {
            throw BusinessRuleException("Depreciation run $id has no lines to post")
        }

        val postableLines =
            lines.filter { it.debitAccountId != null && it.creditAccountId != null }
        if (postableLines.size < lines.size) {
            log.warn(
                "Depreciation run {} has {} line(s) missing GL accounts — skipped from the journal entry",
                id,
                lines.size - postableLines.size,
            )
        }

        val journalEntryId =
            if (postableLines.isNotEmpty()) {
                val journalLines = buildJournalLines(postableLines)
                val periodDate = lastDayOf(run.periodYear, run.periodMonth)
                val entry =
                    journalEntryService.createSystemEntry(
                        date = periodDate,
                        description = "Depreciation for %04d-%02d".format(run.periodYear, run.periodMonth),
                        organizationId = organizationId,
                        lines = journalLines,
                        sourceReference = "depreciation_run:${run.id}",
                        createdBy = postedBy,
                    )
                entry.id
            } else {
                null
            }

        // Roll the postable amounts into each asset's accumulated balance and
        // flip the status flag when the salvage floor is hit.
        val assetsById = fixedAssetRepository.findAllById(postableLines.map { it.assetId }).associateBy { it.id }
        postableLines.forEach { line ->
            val asset = assetsById[line.assetId] ?: return@forEach
            val newAccumulated = asset.accumulatedDepreciation.add(line.depreciationAmount)
            val depreciableBase = asset.acquisitionCost.subtract(asset.salvageValue)
            val nextStatus =
                if (newAccumulated >= depreciableBase) AssetStatus.FULLY_DEPRECIATED else asset.status
            fixedAssetRepository.save(
                asset.copy(accumulatedDepreciation = newAccumulated, status = nextStatus),
            )
        }

        val postedTotal = postableLines.fold(BigDecimal.ZERO) { acc, l -> acc.add(l.depreciationAmount) }
        return runRepository.save(
            run.copy(
                status = DepreciationRunStatus.POSTED,
                journalEntryId = journalEntryId,
                postedAt = LocalDateTime.now(ZoneOffset.UTC),
                postedBy = postedBy,
                totalDepreciation = postedTotal,
            ),
        )
    }

    private fun buildJournalLines(lines: List<AssetDepreciationRunLine>): List<JournalEntryLine> {
        // Sum by (debit_account, credit_account) pair so the JE has just two
        // lines per account-pair instead of two per asset.
        val byPair =
            lines
                .groupBy { Pair(it.debitAccountId!!, it.creditAccountId!!) }
                .mapValues { (_, ls) -> ls.fold(BigDecimal.ZERO) { acc, l -> acc.add(l.depreciationAmount) } }

        val accountIds = byPair.keys.flatMap { listOf(it.first, it.second) }.distinct()
        val accountsById = accountRepository.findAllById(accountIds).associateBy { it.id }

        return byPair.flatMap { (pair, amount) ->
            val (debitId, creditId) = pair
            val debit = accountsById[debitId] ?: throw BusinessRuleException("Account $debitId not found")
            val credit = accountsById[creditId] ?: throw BusinessRuleException("Account $creditId not found")
            listOf(
                JournalEntryLine(
                    accountId = debit.id,
                    accountCode = debit.code,
                    accountName = debit.name,
                    debit = amount,
                    credit = BigDecimal.ZERO,
                    description = "Depreciation expense",
                ),
                JournalEntryLine(
                    accountId = credit.id,
                    accountCode = credit.code,
                    accountName = credit.name,
                    debit = BigDecimal.ZERO,
                    credit = amount,
                    description = "Accumulated depreciation",
                ),
            )
        }
    }

    private fun lastDayOf(
        year: Int,
        month: Int,
    ): LocalDate = YearMonth.of(year, month).atEndOfMonth()

    companion object {
        private val PLACEHOLDER_RUN_ID = UUID.fromString("00000000-0000-0000-0000-000000000000")
    }
}
