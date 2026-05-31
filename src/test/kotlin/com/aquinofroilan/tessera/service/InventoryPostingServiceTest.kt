package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.model.Account
import com.aquinofroilan.tessera.model.AccountType
import com.aquinofroilan.tessera.model.Currency
import com.aquinofroilan.tessera.model.JournalEntry
import com.aquinofroilan.tessera.model.JournalEntryLine
import com.aquinofroilan.tessera.model.Organizations
import com.aquinofroilan.tessera.model.StockMovement
import com.aquinofroilan.tessera.model.StockMovementType
import com.aquinofroilan.tessera.repository.AccountRepository
import com.aquinofroilan.tessera.repository.JournalEntryRepository
import com.aquinofroilan.tessera.repository.OrganizationRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.Optional

class InventoryPostingServiceTest {
    private lateinit var organizationRepository: OrganizationRepository
    private lateinit var accountRepository: AccountRepository
    private lateinit var currencyService: CurrencyService
    private lateinit var journalEntryService: JournalEntryService
    private lateinit var journalEntryRepository: JournalEntryRepository
    private lateinit var service: InventoryPostingService

    private val orgId = "org-1"

    @BeforeEach
    fun setup() {
        organizationRepository = mock(OrganizationRepository::class.java)
        accountRepository = mock(AccountRepository::class.java)
        currencyService = mock(CurrencyService::class.java)
        journalEntryService = mock(JournalEntryService::class.java)
        journalEntryRepository = mock(JournalEntryRepository::class.java)
        whenever(currencyService.getCurrency(any())).thenReturn(Currency("USD", "US Dollar", "$", 2))
        whenever(journalEntryService.createSystemEntry(any(), any(), any(), any(), any(), any()))
            .thenReturn(mock(JournalEntry::class.java))
        service =
            InventoryPostingService(
                organizationRepository,
                accountRepository,
                currencyService,
                journalEntryService,
                journalEntryRepository,
            )
    }

    @Test
    fun `does not post when GL posting is disabled`() {
        stubOrg(enabled = false)

        service.postMovement(movement(StockMovementType.RECEIPT, BigDecimal("10")), BigDecimal("50.00"))

        verify(journalEntryService, never()).createSystemEntry(any(), any(), any(), any(), any(), any())
    }

    @Test
    fun `does not post transfers`() {
        stubOrg(enabled = true)

        service.postMovement(
            movement(StockMovementType.TRANSFER, BigDecimal("3"), transferTo = "wh-2"),
            BigDecimal("30.00"),
        )

        verify(journalEntryService, never()).createSystemEntry(any(), any(), any(), any(), any(), any())
    }

    @Test
    fun `does not post zero-cost movements`() {
        stubOrg(enabled = true)

        service.postMovement(movement(StockMovementType.ISSUE, BigDecimal("5")), BigDecimal.ZERO)

        verify(journalEntryService, never()).createSystemEntry(any(), any(), any(), any(), any(), any())
    }

    @Test
    fun `receipt debits inventory and credits inventory clearing`() {
        stubOrg(enabled = true)
        stubAccount("1200", "Inventory", AccountType.ASSET)
        stubAccount("2150", "Inventory Clearing", AccountType.LIABILITY)

        service.postMovement(movement(StockMovementType.RECEIPT, BigDecimal("10")), BigDecimal("50.00"))

        val lines = capturedLines()
        assertThat(debitOf(lines, "1200")).isEqualByComparingTo("50.00")
        assertThat(creditOf(lines, "2150")).isEqualByComparingTo("50.00")
    }

    @Test
    fun `issue debits COGS and credits inventory`() {
        stubOrg(enabled = true)
        stubAccount("5000", "COGS", AccountType.EXPENSE)
        stubAccount("1200", "Inventory", AccountType.ASSET)

        service.postMovement(movement(StockMovementType.ISSUE, BigDecimal("5")), BigDecimal("25.00"))

        val lines = capturedLines()
        assertThat(debitOf(lines, "5000")).isEqualByComparingTo("25.00")
        assertThat(creditOf(lines, "1200")).isEqualByComparingTo("25.00")
    }

    @Test
    fun `negative adjustment debits adjustment and credits inventory`() {
        stubOrg(enabled = true)
        stubAccount("5050", "Inventory Adjustment", AccountType.EXPENSE)
        stubAccount("1200", "Inventory", AccountType.ASSET)

        service.postMovement(movement(StockMovementType.ADJUSTMENT, BigDecimal("-4")), BigDecimal("12.00"))

        val lines = capturedLines()
        assertThat(debitOf(lines, "5050")).isEqualByComparingTo("12.00")
        assertThat(creditOf(lines, "1200")).isEqualByComparingTo("12.00")
    }

    @Test
    fun `does not post when an entry already exists for the movement`() {
        stubOrg(enabled = true)
        whenever(journalEntryRepository.existsByOrganizationIdAndSourceReference(eq(orgId), any()))
            .thenReturn(true)

        service.postMovement(movement(StockMovementType.RECEIPT, BigDecimal("10")), BigDecimal("50.00"))

        verify(journalEntryService, never()).createSystemEntry(any(), any(), any(), any(), any(), any())
    }

    @Test
    fun `throws when a posting account is missing`() {
        stubOrg(enabled = true)
        whenever(accountRepository.findByOrganizationIdAndCode(eq(orgId), any())).thenReturn(Optional.empty())

        assertThatThrownBy {
            service.postMovement(movement(StockMovementType.RECEIPT, BigDecimal("10")), BigDecimal("50.00"))
        }.isInstanceOf(IllegalStateException::class.java)
    }

    private fun stubOrg(enabled: Boolean) {
        whenever(organizationRepository.findById(orgId)).thenReturn(
            Optional.of(
                Organizations(
                    uuid = orgId,
                    orgSlug = "slug",
                    name = "Test",
                    legalName = "Test",
                    tradeName = "Test",
                    baseCurrency = "USD",
                    fiscalYearStart = LocalDateTime.of(2026, 1, 1, 0, 0),
                    timezone = "UTC",
                    inventoryGlPostingEnabled = enabled,
                ),
            ),
        )
    }

    private fun stubAccount(
        code: String,
        name: String,
        type: AccountType,
    ) {
        whenever(accountRepository.findByOrganizationIdAndCode(orgId, code)).thenReturn(
            Optional.of(Account(code = code, name = name, type = type, organizationId = orgId)),
        )
    }

    private fun capturedLines(): List<JournalEntryLine> {
        val captor = argumentCaptor<List<JournalEntryLine>>()
        verify(journalEntryService).createSystemEntry(any(), any(), eq(orgId), captor.capture(), any(), any())
        return captor.firstValue
    }

    private fun debitOf(
        lines: List<JournalEntryLine>,
        code: String,
    ): BigDecimal = lines.first { it.accountCode == code }.debit

    private fun creditOf(
        lines: List<JournalEntryLine>,
        code: String,
    ): BigDecimal = lines.first { it.accountCode == code }.credit

    private fun movement(
        type: StockMovementType,
        quantity: BigDecimal,
        transferTo: String? = null,
    ) = StockMovement(
        organizationId = orgId,
        type = type,
        productId = "prod-1",
        warehouseId = "wh-1",
        transferToWarehouseId = transferTo,
        quantity = quantity,
        occurredAt = LocalDateTime.of(2026, 5, 1, 0, 0),
        createdBy = "user-1",
    )
}
