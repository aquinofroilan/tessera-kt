package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.CreateBankAccountRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.model.Account
import com.aquinofroilan.tessera.model.AccountType
import com.aquinofroilan.tessera.model.BankAccount
import com.aquinofroilan.tessera.model.Organizations
import com.aquinofroilan.tessera.repository.BankAccountRepository
import com.aquinofroilan.tessera.repository.OrganizationRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.Optional

class BankAccountServiceTest {
    private lateinit var repository: BankAccountRepository
    private lateinit var accountService: AccountService
    private lateinit var organizationRepository: OrganizationRepository
    private lateinit var service: BankAccountService

    private val orgId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val userId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000002")
    private val glAssetId = java.util.UUID.fromString("00000000-0000-0000-0000-000000001000")
    private val glLiabId = java.util.UUID.fromString("00000000-0000-0000-0000-000000002000")

    @BeforeEach
    fun setup() {
        repository = mock(BankAccountRepository::class.java)
        accountService = mock(AccountService::class.java)
        organizationRepository = mock(OrganizationRepository::class.java)
        whenever(repository.save(any<BankAccount>())).thenAnswer { it.arguments[0] }
        whenever(repository.findByOrganizationIdAndCode(any(), any())).thenReturn(Optional.empty())
        whenever(accountService.getAccount(glAssetId, orgId)).thenReturn(account(glAssetId, "1000", AccountType.ASSET, isActive = true))
        whenever(accountService.getAccount(glLiabId, orgId)).thenReturn(account(glLiabId, "2000", AccountType.LIABILITY, isActive = true))
        whenever(organizationRepository.findById(orgId)).thenReturn(
            Optional.of(
                Organizations(
                    uuid = orgId,
                    orgSlug = "org",
                    name = "Org",
                    legalName = "Org",
                    tradeName = "Org",
                    baseCurrency = "USD",
                    fiscalYearStart = LocalDateTime.now(),
                    timezone = "UTC",
                ),
            ),
        )
        service = BankAccountService(repository, accountService, organizationRepository)
    }

    @Test
    fun `create normalises code to upper, defaults currency, initialises balance`() {
        val b =
            service.createBankAccount(
                CreateBankAccountRequest(
                    code = " main-chk ",
                    name = "Main Checking",
                    glAccountId = glAssetId,
                    openingBalance = BigDecimal("1000"),
                ),
                orgId,
                userId,
            )
        assertThat(b.code).isEqualTo("MAIN-CHK")
        assertThat(b.currency).isEqualTo("USD")
        assertThat(b.openingBalance).isEqualByComparingTo(BigDecimal("1000"))
        assertThat(b.currentBalance).isEqualByComparingTo(BigDecimal("1000"))
    }

    @Test
    fun `create rejects non-ASSET GL account`() {
        assertThatThrownBy {
            service.createBankAccount(
                CreateBankAccountRequest(code = "X", name = "X", glAccountId = glLiabId),
                orgId,
                userId,
            )
        }.isInstanceOf(BusinessRuleException::class.java).hasMessageContaining("ASSET")
    }

    @Test
    fun `create rejects inactive GL account`() {
        whenever(accountService.getAccount(glAssetId, orgId)).thenReturn(
            account(glAssetId, "1000", AccountType.ASSET, isActive = false),
        )
        assertThatThrownBy {
            service.createBankAccount(
                CreateBankAccountRequest(code = "X", name = "X", glAccountId = glAssetId),
                orgId,
                userId,
            )
        }.isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `create rejects duplicate code`() {
        whenever(repository.findByOrganizationIdAndCode(orgId, "MAIN")).thenReturn(
            Optional.of(
                BankAccount(
                    organizationId = orgId,
                    code = "MAIN",
                    name = "x",
                    currency = "USD",
                    glAccountId = glAssetId,
                    createdBy = userId,
                ),
            ),
        )
        assertThatThrownBy {
            service.createBankAccount(
                CreateBankAccountRequest(code = "MAIN", name = "x", glAccountId = glAssetId),
                orgId,
                userId,
            )
        }.isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `applyBalanceDelta updates current balance`() {
        val b1Id = java.util.UUID.fromString("00000000-0000-0000-0000-000000000010")
        whenever(repository.findById(b1Id)).thenReturn(
            Optional.of(
                BankAccount(
                    id = b1Id,
                    organizationId = orgId,
                    code = "MAIN",
                    name = "x",
                    currency = "USD",
                    glAccountId = glAssetId,
                    currentBalance = BigDecimal("500"),
                    createdBy = userId,
                ),
            ),
        )
        val updated = service.applyBalanceDelta(b1Id, orgId, BigDecimal("-100"))
        assertThat(updated.currentBalance).isEqualByComparingTo(BigDecimal("400"))
    }

    @Test
    fun `deactivate rejects double-deactivation`() {
        val b1Id = java.util.UUID.fromString("00000000-0000-0000-0000-000000000010")
        whenever(repository.findById(b1Id)).thenReturn(
            Optional.of(
                BankAccount(
                    id = b1Id,
                    organizationId = orgId,
                    code = "MAIN",
                    name = "x",
                    currency = "USD",
                    glAccountId = glAssetId,
                    isActive = false,
                    createdBy = userId,
                ),
            ),
        )
        assertThatThrownBy { service.deactivateBankAccount(b1Id, orgId) }
            .isInstanceOf(BusinessRuleException::class.java)
    }

    private fun account(
        id: java.util.UUID,
        code: String,
        type: AccountType,
        isActive: Boolean,
    ) = Account(
        id = id,
        code = code,
        name = code,
        type = type,
        organizationId = orgId,
        isActive = isActive,
    )
}
