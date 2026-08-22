package com.aquinofroilan.tessera.domain.finance.service

import com.aquinofroilan.tessera.domain.finance.dto.ImportStatementLineRequest
import com.aquinofroilan.tessera.domain.finance.dto.ImportStatementRequest
import com.aquinofroilan.tessera.domain.finance.model.BankAccount
import com.aquinofroilan.tessera.domain.finance.model.BankStatement
import com.aquinofroilan.tessera.domain.finance.repository.BankStatementRepository
import com.aquinofroilan.tessera.exception.BusinessRuleException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.LocalDate

class BankStatementServiceTest {
    private lateinit var repository: BankStatementRepository
    private lateinit var bankAccountService: BankAccountService
    private lateinit var service: BankStatementService

    private val orgId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000100")
    private val userId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000101")
    private val bankId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000102")

    @BeforeEach
    fun setup() {
        repository = mock(BankStatementRepository::class.java)
        bankAccountService = mock(BankAccountService::class.java)
        whenever(repository.save(any<BankStatement>())).thenAnswer { it.arguments[0] }
        whenever(bankAccountService.getBankAccount(bankId, orgId)).thenReturn(activeAccount())
        service = BankStatementService(repository, bankAccountService)
    }

    @Test
    fun `import accepts balanced statement, stamps source, applies balance delta`() {
        val req =
            request(
                opening = BigDecimal("1000"),
                closing = BigDecimal("950"),
                lines = listOf(line(BigDecimal("100")), line(BigDecimal("-150"))),
            )

        val s = service.importStatement(req, orgId, userId)

        assertThat(s.openingBalance).isEqualByComparingTo(BigDecimal("1000"))
        assertThat(s.closingBalance).isEqualByComparingTo(BigDecimal("950"))
        assertThat(s.source).isEqualTo("CSV")
        assertThat(s.lines).hasSize(2)
        verify(bankAccountService).applyBalanceDelta(bankId, orgId, BigDecimal("-50"))
    }

    @Test
    fun `import rejects unbalanced statement`() {
        val req =
            request(
                opening = BigDecimal("1000"),
                closing = BigDecimal("999"),
                lines = listOf(line(BigDecimal("100"))),
            )
        assertThatThrownBy { service.importStatement(req, orgId, userId) }
            .isInstanceOf(BusinessRuleException::class.java)
            .hasMessageContaining("does not balance")
    }

    @Test
    fun `import rejects zero-amount line`() {
        val req =
            request(
                opening = BigDecimal("1000"),
                closing = BigDecimal("1000"),
                lines = listOf(line(BigDecimal.ZERO)),
            )
        assertThatThrownBy { service.importStatement(req, orgId, userId) }
            .isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `import rejects inactive bank account`() {
        whenever(bankAccountService.getBankAccount(bankId, orgId)).thenReturn(activeAccount().copy(isActive = false))
        val req =
            request(
                opening = BigDecimal("0"),
                closing = BigDecimal("100"),
                lines = listOf(line(BigDecimal("100"))),
            )
        assertThatThrownBy { service.importStatement(req, orgId, userId) }
            .isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `import rejects unsupported source`() {
        val req =
            request(
                opening = BigDecimal("0"),
                closing = BigDecimal("100"),
                source = "XML",
                lines = listOf(line(BigDecimal("100"))),
            )
        assertThatThrownBy { service.importStatement(req, orgId, userId) }
            .isInstanceOf(BusinessRuleException::class.java)
            .hasMessageContaining("Unsupported source")
    }

    private fun line(amount: BigDecimal) =
        ImportStatementLineRequest(
            postedDate = LocalDate.of(2026, 6, 1),
            description = "Transaction",
            reference = null,
            amount = amount,
        )

    private fun request(
        opening: BigDecimal,
        closing: BigDecimal,
        lines: List<ImportStatementLineRequest>,
        source: String? = null,
    ) = ImportStatementRequest(
        bankAccountId = bankId,
        statementDate = LocalDate.of(2026, 6, 1),
        openingBalance = opening,
        closingBalance = closing,
        source = source,
        lines = lines,
    )

    private fun activeAccount() =
        BankAccount(
            id = bankId,
            organizationId = orgId,
            code = "MAIN",
            name = "Main",
            currency = "USD",
            glAccountId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000103"),
            isActive = true,
            createdBy = userId,
        )
}
