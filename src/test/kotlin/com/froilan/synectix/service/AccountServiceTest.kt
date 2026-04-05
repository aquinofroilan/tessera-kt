package com.froilan.synectix.service

import com.froilan.synectix.dto.CreateAccountRequest
import com.froilan.synectix.dto.UpdateAccountRequest
import com.froilan.synectix.model.Account
import com.froilan.synectix.model.AccountType
import com.froilan.synectix.repository.AccountRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.springframework.dao.DuplicateKeyException
import java.util.Optional

class AccountServiceTest {
    private lateinit var accountService: AccountService
    private lateinit var accountRepository: AccountRepository

    @BeforeEach
    fun setup() {
        accountRepository = mock(AccountRepository::class.java)
        accountService = AccountService(accountRepository = accountRepository)
    }

    @Test
    fun `createAccount should create and return account`() {
        `when`(accountRepository.save(any<Account>())).thenAnswer { it.arguments[0] }

        val request =
            CreateAccountRequest(
                code = "1000",
                name = "Cash",
                type = "ASSET",
                parentId = null,
                description = "Cash account",
            )

        val result = accountService.createAccount(request, "org-123")

        assertThat(result.code).isEqualTo("1000")
        assertThat(result.name).isEqualTo("Cash")
        assertThat(result.type).isEqualTo(AccountType.ASSET)
        assertThat(result.description).isEqualTo("Cash account")
        assertThat(result.organizationId).isEqualTo("org-123")
        assertThat(result.isActive).isTrue()
        assertThat(result.isSystemAccount).isFalse()

        val captor = argumentCaptor<Account>()
        verify(accountRepository).save(captor.capture())
        assertThat(captor.firstValue.code).isEqualTo("1000")
        assertThat(captor.firstValue.organizationId).isEqualTo("org-123")
    }

    @Test
    fun `createAccount should throw when code already exists`() {
        `when`(accountRepository.save(any<Account>())).thenThrow(DuplicateKeyException("duplicate"))

        val request =
            CreateAccountRequest(
                code = "1000",
                name = "Cash",
                type = "ASSET",
                parentId = null,
                description = null,
            )

        val exception =
            assertThrows<IllegalArgumentException> {
                accountService.createAccount(request, "org-123")
            }
        assertThat(exception.message).contains("Account code '1000' already exists")
    }

    @Test
    fun `createAccount should throw when type is invalid`() {
        val request =
            CreateAccountRequest(
                code = "1000",
                name = "Bad Type",
                type = "INVALID_TYPE",
                parentId = null,
                description = null,
            )

        val exception =
            assertThrows<IllegalArgumentException> {
                accountService.createAccount(request, "org-123")
            }
        assertThat(exception.message).contains("Invalid account type")
        assertThat(exception.message).contains("INVALID_TYPE")
    }

    @Test
    fun `createAccount should throw when parent not found`() {
        `when`(accountRepository.findById("parent-999")).thenReturn(Optional.empty())

        val request =
            CreateAccountRequest(
                code = "1001",
                name = "Sub Cash",
                type = "ASSET",
                parentId = "parent-999",
                description = null,
            )

        val exception =
            assertThrows<IllegalArgumentException> {
                accountService.createAccount(request, "org-123")
            }
        assertThat(exception.message).isEqualTo("Parent account not found")
    }

    @Test
    fun `createAccount should throw when parent is different type`() {
        val parent =
            createMockAccount(
                id = "parent-1",
                type = AccountType.ASSET,
                organizationId = "org-123",
            )
        `when`(accountRepository.findById("parent-1")).thenReturn(Optional.of(parent))

        val request =
            CreateAccountRequest(
                code = "5001",
                name = "Expense Sub",
                type = "EXPENSE",
                parentId = "parent-1",
                description = null,
            )

        val exception =
            assertThrows<IllegalArgumentException> {
                accountService.createAccount(request, "org-123")
            }
        assertThat(exception.message).isEqualTo("Parent account must be the same type")
    }

    @Test
    fun `createAccount should throw when parent is different org`() {
        val parent =
            createMockAccount(
                id = "parent-1",
                type = AccountType.ASSET,
                organizationId = "other-org",
            )
        `when`(accountRepository.findById("parent-1")).thenReturn(Optional.of(parent))

        val request =
            CreateAccountRequest(
                code = "1001",
                name = "Sub Cash",
                type = "ASSET",
                parentId = "parent-1",
                description = null,
            )

        val exception =
            assertThrows<IllegalArgumentException> {
                accountService.createAccount(request, "org-123")
            }
        assertThat(exception.message).isEqualTo("Parent account not found")
    }

    @Test
    fun `updateAccount should update name and description`() {
        val existing = createMockAccount(id = "acc-1", code = "1000", name = "Cash", type = AccountType.ASSET)
        `when`(accountRepository.findById("acc-1")).thenReturn(Optional.of(existing))
        `when`(accountRepository.save(any<Account>())).thenAnswer { it.arguments[0] }

        val request = UpdateAccountRequest(name = "Updated Cash", description = "Updated desc", isActive = null)

        val result = accountService.updateAccount("acc-1", request, "org-123")

        assertThat(result.name).isEqualTo("Updated Cash")
        assertThat(result.description).isEqualTo("Updated desc")
        assertThat(result.code).isEqualTo("1000")
        assertThat(result.type).isEqualTo(AccountType.ASSET)

        val captor = argumentCaptor<Account>()
        verify(accountRepository).save(captor.capture())
        assertThat(captor.firstValue.code).isEqualTo("1000")
        assertThat(captor.firstValue.type).isEqualTo(AccountType.ASSET)
    }

    @Test
    fun `updateAccount should throw when account not found`() {
        `when`(accountRepository.findById("acc-999")).thenReturn(Optional.empty())

        val request = UpdateAccountRequest(name = "New Name", description = null, isActive = null)

        val exception =
            assertThrows<IllegalArgumentException> {
                accountService.updateAccount("acc-999", request, "org-123")
            }
        assertThat(exception.message).isEqualTo("Account not found")
    }

    @Test
    fun `updateAccount should throw when wrong org`() {
        val existing = createMockAccount(id = "acc-1", organizationId = "other-org")
        `when`(accountRepository.findById("acc-1")).thenReturn(Optional.of(existing))

        val request = UpdateAccountRequest(name = "New Name", description = null, isActive = null)

        val exception =
            assertThrows<IllegalArgumentException> {
                accountService.updateAccount("acc-1", request, "org-123")
            }
        assertThat(exception.message).isEqualTo("Account not found")
    }

    @Test
    fun `getAccount should return account`() {
        val account = createMockAccount(id = "acc-1")
        `when`(accountRepository.findById("acc-1")).thenReturn(Optional.of(account))

        val result = accountService.getAccount("acc-1", "org-123")

        assertThat(result.id).isEqualTo("acc-1")
        assertThat(result.organizationId).isEqualTo("org-123")
    }

    @Test
    fun `getAccount should throw when wrong org`() {
        val account = createMockAccount(id = "acc-1", organizationId = "other-org")
        `when`(accountRepository.findById("acc-1")).thenReturn(Optional.of(account))

        val exception =
            assertThrows<IllegalArgumentException> {
                accountService.getAccount("acc-1", "org-123")
            }
        assertThat(exception.message).isEqualTo("Account not found")
    }

    @Test
    fun `listAccounts should return active accounts`() {
        val accounts =
            listOf(
                createMockAccount(id = "acc-1"),
                createMockAccount(id = "acc-2", name = "Receivables"),
            )
        `when`(accountRepository.findByOrganizationIdAndIsActive("org-123", true)).thenReturn(accounts)

        val result = accountService.listAccounts("org-123", type = null, parentId = null)

        assertThat(result).hasSize(2)
        verify(accountRepository).findByOrganizationIdAndIsActive("org-123", true)
    }

    @Test
    fun `listAccounts should filter by type`() {
        val accounts = listOf(createMockAccount(id = "acc-1", type = AccountType.EXPENSE))
        `when`(
            accountRepository.findByOrganizationIdAndTypeAndIsActive("org-123", AccountType.EXPENSE, true),
        ).thenReturn(accounts)

        val result = accountService.listAccounts("org-123", type = AccountType.EXPENSE, parentId = null)

        assertThat(result).hasSize(1)
        assertThat(result[0].type).isEqualTo(AccountType.EXPENSE)
        verify(accountRepository).findByOrganizationIdAndTypeAndIsActive("org-123", AccountType.EXPENSE, true)
    }

    @Test
    fun `listAccounts should filter by parentId`() {
        val accounts = listOf(createMockAccount(id = "acc-2", parentId = "acc-1"))
        `when`(
            accountRepository.findByOrganizationIdAndParentIdAndIsActive("org-123", "acc-1", true),
        ).thenReturn(accounts)

        val result = accountService.listAccounts("org-123", type = null, parentId = "acc-1")

        assertThat(result).hasSize(1)
        assertThat(result[0].parentId).isEqualTo("acc-1")
        verify(accountRepository).findByOrganizationIdAndParentIdAndIsActive("org-123", "acc-1", true)
    }

    @Test
    fun `deleteAccount should soft delete account`() {
        val account = createMockAccount(id = "acc-1", isSystemAccount = false)
        `when`(accountRepository.findById("acc-1")).thenReturn(Optional.of(account))
        `when`(accountRepository.existsByOrganizationIdAndParentId("org-123", "acc-1")).thenReturn(false)
        `when`(accountRepository.save(any<Account>())).thenAnswer { it.arguments[0] }

        accountService.deleteAccount("acc-1", "org-123")

        val captor = argumentCaptor<Account>()
        verify(accountRepository).save(captor.capture())
        assertThat(captor.firstValue.isActive).isFalse()
        assertThat(captor.firstValue.id).isEqualTo("acc-1")
    }

    @Test
    fun `deleteAccount should throw when system account`() {
        val account = createMockAccount(id = "acc-1", isSystemAccount = true)
        `when`(accountRepository.findById("acc-1")).thenReturn(Optional.of(account))

        val exception =
            assertThrows<IllegalArgumentException> {
                accountService.deleteAccount("acc-1", "org-123")
            }
        assertThat(exception.message).isEqualTo("System accounts cannot be deleted")
    }

    @Test
    fun `deleteAccount should throw when has children`() {
        val account = createMockAccount(id = "acc-1", isSystemAccount = false)
        `when`(accountRepository.findById("acc-1")).thenReturn(Optional.of(account))
        `when`(accountRepository.existsByOrganizationIdAndParentId("org-123", "acc-1")).thenReturn(true)

        val exception =
            assertThrows<IllegalArgumentException> {
                accountService.deleteAccount("acc-1", "org-123")
            }
        assertThat(exception.message).isEqualTo("Cannot delete account with child accounts")
    }

    @Test
    fun `seedDefaultAccounts should create 20 default accounts`() {
        `when`(accountRepository.saveAll(any<List<Account>>())).thenAnswer { it.arguments[0] }

        accountService.seedDefaultAccounts("org-123")

        @Suppress("UNCHECKED_CAST")
        val captor = argumentCaptor<List<Account>>()
        verify(accountRepository).saveAll(captor.capture())

        val savedAccounts = captor.firstValue
        assertThat(savedAccounts).hasSize(20)
        assertThat(savedAccounts).allMatch { it.isSystemAccount }
        assertThat(savedAccounts).allMatch { it.organizationId == "org-123" }
        assertThat(savedAccounts).allMatch { it.isActive }
    }

    private fun createMockAccount(
        id: String = "acc-1",
        code: String = "1000",
        name: String = "Cash",
        type: AccountType = AccountType.ASSET,
        parentId: String? = null,
        organizationId: String = "org-123",
        isActive: Boolean = true,
        isSystemAccount: Boolean = false,
    ) = Account(
        id = id,
        code = code,
        name = name,
        type = type,
        parentId = parentId,
        organizationId = organizationId,
        isActive = isActive,
        isSystemAccount = isSystemAccount,
    )
}
