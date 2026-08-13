package com.aquinofroilan.tessera.service.asset

import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import com.aquinofroilan.tessera.model.AssetDisposal
import com.aquinofroilan.tessera.model.AssetStatus
import com.aquinofroilan.tessera.model.DisposalStatus
import com.aquinofroilan.tessera.model.DisposalType
import com.aquinofroilan.tessera.model.JournalEntryLine
import com.aquinofroilan.tessera.repository.AccountRepository
import com.aquinofroilan.tessera.repository.AssetDisposalRepository
import com.aquinofroilan.tessera.repository.FixedAssetRepository
import com.aquinofroilan.tessera.service.FixedAssetService
import com.aquinofroilan.tessera.service.JournalEntryService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

/**
 * Asset disposal lifecycle. Two states: DRAFT (configured but not yet
 * reflected in the books) and POSTED (JE generated, asset retired).
 *
 * Posting an asset disposal:
 * 1. Calculates the net book value: NBV = cost - accumulated_depreciation.
 * 2. Computes gain or loss against proceeds: gain = proceeds - NBV.
 * 3. Generates a system [com.aquinofroilan.tessera.model.JournalEntry]
 *    that removes the asset and accumulated depreciation balances,
 *    records the proceeds (if any), and posts the gain/loss net.
 * 4. Flips the asset status to DISPOSED.
 *
 * Required GL accounts at post time:
 * - asset.assetAccountId
 * - asset.accumulatedDepreciationAccountId
 * - disposal.gainLossAccountId (always)
 * - disposal.cashAccountId (only when proceeds > 0)
 */
@Service
class AssetDisposalService(
    private val disposalRepository: AssetDisposalRepository,
    private val fixedAssetRepository: FixedAssetRepository,
    private val fixedAssetService: FixedAssetService,
    private val journalEntryService: JournalEntryService,
    private val accountRepository: AccountRepository,
) {
    @Transactional
    fun createDisposal(
        organizationId: UUID,
        assetId: UUID,
        disposalType: DisposalType,
        disposalDate: LocalDate,
        proceeds: BigDecimal,
        gainLossAccountId: UUID?,
        cashAccountId: UUID?,
        notes: String?,
    ): AssetDisposal {
        val asset = fixedAssetService.getAsset(assetId, organizationId)
        if (asset.status == AssetStatus.DISPOSED) {
            throw BusinessRuleException("Asset ${asset.assetNumber} is already disposed")
        }
        if (proceeds.signum() < 0) {
            throw BusinessRuleException("Proceeds cannot be negative")
        }
        if (disposalRepository.existsByAssetIdAndStatus(assetId, DisposalStatus.POSTED)) {
            throw BusinessRuleException("A posted disposal already exists for asset ${asset.assetNumber}")
        }
        return disposalRepository.save(
            AssetDisposal(
                organizationId = organizationId,
                assetId = assetId,
                disposalDate = disposalDate,
                disposalType = disposalType,
                proceeds = proceeds,
                gainLossAccountId = gainLossAccountId,
                cashAccountId = cashAccountId,
                notes = notes?.trim()?.takeIf { it.isNotEmpty() },
            ),
        )
    }

    fun listDisposals(organizationId: UUID): List<AssetDisposal> =
        disposalRepository.findByOrganizationIdOrderByDisposalDateDesc(organizationId)

    fun getDisposal(
        id: UUID,
        organizationId: UUID,
    ): AssetDisposal {
        val disposal =
            disposalRepository.findById(id).orElseThrow {
                ResourceNotFoundException("Disposal $id not found")
            }
        if (disposal.organizationId != organizationId) {
            throw ResourceNotFoundException("Disposal $id not found")
        }
        return disposal
    }

    @Transactional
    fun postDisposal(
        id: UUID,
        organizationId: UUID,
        postedBy: UUID,
    ): AssetDisposal {
        val disposal = getDisposal(id, organizationId)
        if (disposal.status != DisposalStatus.DRAFT) {
            throw BusinessRuleException("Only draft disposals can be posted")
        }
        val asset = fixedAssetService.getAsset(disposal.assetId, organizationId)

        val assetAccountId =
            asset.assetAccountId?.let { UUID.fromString(it) }
                ?: throw BusinessRuleException("Asset ${asset.assetNumber} has no asset GL account configured")
        val accumulatedAccountId =
            asset.accumulatedDepreciationAccountId?.let { UUID.fromString(it) }
                ?: throw BusinessRuleException("Asset ${asset.assetNumber} has no accumulated-depreciation GL account configured")
        val gainLossAccountId =
            disposal.gainLossAccountId
                ?: throw BusinessRuleException("Disposal requires a gain/loss GL account")
        val cashAccountId =
            if (disposal.proceeds.signum() > 0) {
                disposal.cashAccountId
                    ?: throw BusinessRuleException("Disposal with proceeds requires a cash GL account")
            } else {
                null
            }

        val accountIds =
            listOfNotNull(assetAccountId, accumulatedAccountId, gainLossAccountId, cashAccountId)
        val accountsById = accountRepository.findAllById(accountIds).associateBy { it.id }
        val asAccount = { aid: UUID ->
            accountsById[aid] ?: throw BusinessRuleException("Account $aid not found")
        }

        val nbv = asset.acquisitionCost.subtract(asset.accumulatedDepreciation)
        val gain = disposal.proceeds.subtract(nbv) // positive = gain, negative = loss

        val lines = mutableListOf<JournalEntryLine>()
        // Remove the gross asset cost (Cr) and accumulated depreciation (Dr).
        val accumulated = asAccount(accumulatedAccountId)
        val assetAcct = asAccount(assetAccountId)
        lines +=
            JournalEntryLine(
                accountId = accumulated.id,
                accountCode = accumulated.code,
                accountName = accumulated.name,
                debit = asset.accumulatedDepreciation,
                credit = BigDecimal.ZERO,
                description = "Remove accumulated depreciation on disposal",
            )
        lines +=
            JournalEntryLine(
                accountId = assetAcct.id,
                accountCode = assetAcct.code,
                accountName = assetAcct.name,
                debit = BigDecimal.ZERO,
                credit = asset.acquisitionCost,
                description = "Remove asset cost on disposal",
            )
        // Cash proceeds, if any.
        if (cashAccountId != null) {
            val cash = asAccount(cashAccountId)
            lines +=
                JournalEntryLine(
                    accountId = cash.id,
                    accountCode = cash.code,
                    accountName = cash.name,
                    debit = disposal.proceeds,
                    credit = BigDecimal.ZERO,
                    description = "Proceeds from disposal",
                )
        }
        // Net gain (Cr) or loss (Dr).
        if (gain.signum() != 0) {
            val gainLoss = asAccount(gainLossAccountId)
            val gainAbs = gain.abs()
            lines +=
                if (gain.signum() > 0) {
                    JournalEntryLine(
                        accountId = gainLoss.id,
                        accountCode = gainLoss.code,
                        accountName = gainLoss.name,
                        debit = BigDecimal.ZERO,
                        credit = gainAbs,
                        description = "Gain on disposal",
                    )
                } else {
                    JournalEntryLine(
                        accountId = gainLoss.id,
                        accountCode = gainLoss.code,
                        accountName = gainLoss.name,
                        debit = gainAbs,
                        credit = BigDecimal.ZERO,
                        description = "Loss on disposal",
                    )
                }
        }

        val entry =
            journalEntryService.createSystemEntry(
                date = disposal.disposalDate,
                description = "Disposal of asset ${asset.assetNumber}",
                organizationId = organizationId,
                lines = lines,
                sourceReference = "asset_disposal:${disposal.id}",
                createdBy = postedBy,
            )

        fixedAssetRepository.save(asset.copy(status = AssetStatus.DISPOSED))

        return disposalRepository.save(
            disposal.copy(
                status = DisposalStatus.POSTED,
                journalEntryId = entry.id,
                postedAt = LocalDateTime.now(ZoneOffset.UTC),
                postedBy = postedBy,
            ),
        )
    }
}
