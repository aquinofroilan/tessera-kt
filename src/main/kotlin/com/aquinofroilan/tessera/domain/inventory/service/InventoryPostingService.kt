package com.aquinofroilan.tessera.domain.inventory.service

import com.aquinofroilan.tessera.domain.finance.model.Account
import com.aquinofroilan.tessera.domain.finance.model.JournalEntryLine
import com.aquinofroilan.tessera.domain.finance.repository.AccountRepository
import com.aquinofroilan.tessera.domain.finance.repository.JournalEntryRepository
import com.aquinofroilan.tessera.domain.finance.service.CurrencyService
import com.aquinofroilan.tessera.domain.finance.service.JournalEntryService
import com.aquinofroilan.tessera.domain.inventory.model.StockMovement
import com.aquinofroilan.tessera.domain.inventory.model.StockMovementType
import com.aquinofroilan.tessera.domain.organization.repository.OrganizationRepository
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Posts inventory movements to the General Ledger so inventory value and COGS
 * reconcile to the balance sheet / P&L. Opt-in per organization via
 * [com.aquinofroilan.tessera.model.Organizations.inventoryGlPostingEnabled].
 *
 * Account conventions (must exist + be active when posting is enabled):
 * - 1200 Inventory (asset)
 * - 2150 Inventory Received Not Invoiced (liability, receipt offset)
 * - 3200 Opening Balance Equity (opening-balance offset)
 * - 5000 Cost of Goods Sold
 * - 5050 Inventory Adjustment
 *
 * Posting is idempotent per movement: each entry carries a deterministic
 * `INVENTORY-<type>-<movementId>` source reference and is skipped if one
 * already exists.
 */
@Service
class InventoryPostingService(
    private val organizationRepository: OrganizationRepository,
    private val accountRepository: AccountRepository,
    private val currencyService: CurrencyService,
    private val journalEntryService: JournalEntryService,
    private val journalEntryRepository: JournalEntryRepository,
) {
    @Transactional
    fun postMovement(
        movement: StockMovement,
        cost: BigDecimal,
    ) {
        val organization =
            organizationRepository.findById(movement.organizationId).orElseThrow {
                ResourceNotFoundException("Organization not found")
            }
        if (!organization.inventoryGlPostingEnabled) {
            return
        }
        if (movement.type == StockMovementType.TRANSFER) {
            return
        }

        val decimals = currencyService.getCurrency(organization.baseCurrency).decimalPlaces
        val amount = cost.abs().setScale(decimals, RoundingMode.HALF_UP)
        if (amount.signum() == 0) {
            return
        }

        val sourceReference = "INVENTORY-${movement.type}-${movement.id}"
        if (journalEntryRepository.existsByOrganizationIdAndSourceReference(movement.organizationId, sourceReference)) {
            return
        }

        val (debitCode, creditCode) = postingAccounts(movement)
        val debit = account(movement.organizationId, debitCode)
        val credit = account(movement.organizationId, creditCode)
        val label = "${movement.type} ${movement.productId} - ${movement.reference ?: movement.id}"

        journalEntryService.createSystemEntry(
            date = movement.occurredAt.toLocalDate(),
            description = "Inventory $label",
            organizationId = movement.organizationId,
            lines =
                listOf(
                    JournalEntryLine(
                        accountId = debit.id,
                        accountCode = debit.code,
                        accountName = debit.name,
                        debit = amount,
                        credit = BigDecimal.ZERO,
                        description = label,
                    ),
                    JournalEntryLine(
                        accountId = credit.id,
                        accountCode = credit.code,
                        accountName = credit.name,
                        debit = BigDecimal.ZERO,
                        credit = amount,
                        description = label,
                    ),
                ),
            sourceReference = sourceReference,
            createdBy = movement.createdBy,
        )
    }

    private fun postingAccounts(movement: StockMovement): Pair<String, String> =
        when (movement.type) {
            StockMovementType.RECEIPT -> INVENTORY_ASSET to INVENTORY_CLEARING
            StockMovementType.OPENING_BALANCE -> INVENTORY_ASSET to OPENING_BALANCE_EQUITY
            StockMovementType.ISSUE -> COGS to INVENTORY_ASSET
            StockMovementType.WIP_ISSUE -> WIP_ASSET to INVENTORY_ASSET
            StockMovementType.WIP_RECEIPT -> INVENTORY_ASSET to WIP_ASSET
            StockMovementType.ADJUSTMENT ->
                if (movement.quantity.signum() > 0) {
                    INVENTORY_ASSET to INVENTORY_ADJUSTMENT
                } else {
                    INVENTORY_ADJUSTMENT to INVENTORY_ASSET
                }
            StockMovementType.TRANSFER -> throw IllegalStateException("Transfers are not posted to the GL")
        }

    private fun account(
        organizationId: java.util.UUID,
        code: String,
    ): Account {
        val account =
            accountRepository.findByOrganizationIdAndCode(organizationId, code).orElseThrow {
                IllegalStateException("Inventory posting account ($code) not found; configure it or disable inventory GL posting")
            }
        if (!account.isActive) {
            throw BusinessRuleException("Inventory posting account ($code) is inactive")
        }
        return account
    }

    private companion object {
        const val INVENTORY_ASSET = "1200"
        const val WIP_ASSET = "1250"
        const val INVENTORY_CLEARING = "2150"
        const val OPENING_BALANCE_EQUITY = "3200"
        const val COGS = "5000"
        const val INVENTORY_ADJUSTMENT = "5050"
    }
}
