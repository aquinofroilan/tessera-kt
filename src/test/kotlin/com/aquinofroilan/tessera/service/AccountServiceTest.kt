package com.aquinofroilan.tessera.service

import java.util.UUID

import com.aquinofroilan.tessera.dto.CreateAccountRequest
import com.aquinofroilan.tessera.dto.UpdateAccountRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import com.aquinofroilan.tessera.model.Account
import com.aquinofroilan.tessera.model.AccountType
import com.aquinofroilan.tessera.repository.AccountRepository
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

        val result = accountService.createAccount(request, java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d"))

        assertThat(result.code).isEqualTo("1000")
        assertThat(result.name).isEqualTo("Cash")
        assertThat(result.type).isEqualTo(AccountType.ASSET)
        assertThat(result.description).isEqualTo("Cash account")
        assertThat(result.organizationId).isEqualTo(java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d"))
        assertThat(result.isActive).isTrue()
        assertThat(result.isSystemAccount).isFalse()

        val captor = argumentCaptor<Account>()
        verify(accountRepository).save(captor.capture())
        assertThat(captor.firstValue.code).isEqualTo("1000")
        assertThat(captor.firstValue.organizationId).isEqualTo(java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d"))
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
            assertThrows<BusinessRuleException> {
                accountService.createAccount(request, java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d"))
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
            assertThrows<BusinessRuleException> {
                accountService.createAccount(request, java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d"))
            }
        assertThat(exception.message).contains("Invalid account type")
        assertThat(exception.message).contains("INVALID_TYPE")
    }

    @Test
    fun `createAccount should throw when parent not found`() {
        `when`(accountRepository.findById(java.util.UUID.fromString("df772144-4a16-3fd3-9f6a-15885c5c7ab0"))).thenReturn(Optional.empty())

        val request =
            CreateAccountRequest(
                code = "1001",
                name = "Sub Cash",
                type = "ASSET",
                parentId = java.util.UUID.fromString("df772144-4a16-3fd3-9f6a-15885c5c7ab0"),
                description = null,
            )

        val exception =
            assertThrows<BusinessRuleException> {
                accountService.createAccount(request, java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d"))
            }
        assertThat(exception.message).isEqualTo("Parent account not found")
    }

    @Test
    fun `createAccount should throw when parent is different type`() {
        val parent =
            createMockAccount(id = java.util.UUID.fromString("a287d498-1fce-3c08-926f-097971e137a0"),
                type = AccountType.ASSET,
                organizationId = java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d"),
            )
        `when`(accountRepository.findById(java.util.UUID.fromString("a287d498-1fce-3c08-926f-097971e137a0"))).thenReturn(Optional.of(parent))

        val request =
            CreateAccountRequest(
                code = "5001",
                name = "Expense Sub",
                type = "EXPENSE",
                parentId = java.util.UUID.fromString("a287d498-1fce-3c08-926f-097971e137a0"),
                description = null,
            )

        val exception =
            assertThrows<BusinessRuleException> {
                accountService.createAccount(request, java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d"))
            }
        assertThat(exception.message).isEqualTo("Parent account must be the same type")
    }

    @Test
    fun `createAccount should throw when parent is different org`() {
        val parent =
            createMockAccount(id = java.util.UUID.fromString("a287d498-1fce-3c08-926f-097971e137a0"),
                type = AccountType.ASSET,
                organizationId = java.util.UUID.fromString("fbede99a-0bef-3bf9-ba0b-8d28f050479d"),
            )
        `when`(accountRepository.findById(java.util.UUID.fromString("a287d498-1fce-3c08-926f-097971e137a0"))).thenReturn(Optional.of(parent))

        val request =
            CreateAccountRequest(
                code = "1001",
                name = "Sub Cash",
                type = "ASSET",
                parentId = java.util.UUID.fromString("a287d498-1fce-3c08-926f-097971e137a0"),
                description = null,
            )

        val exception =
            assertThrows<BusinessRuleException> {
                accountService.createAccount(request, java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d"))
            }
        assertThat(exception.message).isEqualTo("Parent account not found")
    }

    @Test
    fun `updateAccount should update name and description`() {
        val existing = createMockAccount(id = java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a"), code = "1000", name = "Cash", type = AccountType.ASSET)
        `when`(accountRepository.findById(java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a"))).thenReturn(Optional.of(existing))
        `when`(accountRepository.save(any<Account>())).thenAnswer { it.arguments[0] }

        val request = UpdateAccountRequest(name = "Updated Cash", description = "Updated desc")

        val result = accountService.updateAccount(java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a"), request, java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d"))

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
        `when`(accountRepository.findById(java.util.UUID.fromString("3b29ac85-9e7e-3010-b2f4-c7ded43370d9"))).thenReturn(Optional.empty())

        val request = UpdateAccountRequest(name = "New Name")

        val exception =
            assertThrows<ResourceNotFoundException> {
                accountService.updateAccount(java.util.UUID.fromString("3b29ac85-9e7e-3010-b2f4-c7ded43370d9"), request, java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d"))
            }
        assertThat(exception.message).isEqualTo("Account not found")
    }

    @Test
    fun `updateAccount should throw when wrong org`() {
        val existing = createMockAccount(id = java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a"), organizationId = java.util.UUID.fromString("fbede99a-0bef-3bf9-ba0b-8d28f050479d"))
        `when`(accountRepository.findById(java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a"))).thenReturn(Optional.of(existing))

        val request = UpdateAccountRequest(name = "New Name")

        val exception =
            assertThrows<ResourceNotFoundException> {
                accountService.updateAccount(java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a"), request, java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d"))
            }
        assertThat(exception.message).isEqualTo("Account not found")
    }

    @Test
    fun `getAccount should return account`() {
        val account = createMockAccount(id = java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a"))
        `when`(accountRepository.findById(java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a"))).thenReturn(Optional.of(account))

        val result = accountService.getAccount(java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a"), java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d"))

        assertThat(result.id).isEqualTo(java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a"))
        assertThat(result.organizationId).isEqualTo(java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d"))
    }

    @Test
    fun `getAccount should throw when wrong org`() {
        val account = createMockAccount(id = java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a"), organizationId = java.util.UUID.fromString("fbede99a-0bef-3bf9-ba0b-8d28f050479d"))
        `when`(accountRepository.findById(java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a"))).thenReturn(Optional.of(account))

        val exception =
            assertThrows<ResourceNotFoundException> {
                accountService.getAccount(java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a"), java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d"))
            }
        assertThat(exception.message).isEqualTo("Account not found")
    }

    @Test
    fun `listAccounts should return active accounts`() {
        val accounts =
            listOf(
                createMockAccount(id = java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a")),
                createMockAccount(id = java.util.UUID.fromString("19770c7a-8b4f-3b6e-adb1-3631faff91ec"), name = "Receivables"),
            )
        `when`(accountRepository.findByOrganizationIdAndIsActive(java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d"), true)).thenReturn(accounts)

        val result = accountService.listAccounts(java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d"), type = null, parentId = null)

        assertThat(result).hasSize(2)
        verify(accountRepository).findByOrganizationIdAndIsActive(java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d"), true)
    }

    @Test
    fun `listAccounts should filter by type`() {
        val accounts = listOf(createMockAccount(id = java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a"), type = AccountType.EXPENSE))
        `when`(
            accountRepository.findByOrganizationIdAndTypeAndIsActive(java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d"), AccountType.EXPENSE, true),
        ).thenReturn(accounts)

        val result = accountService.listAccounts(java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d"), type = AccountType.EXPENSE, parentId = null)

        assertThat(result).hasSize(1)
        assertThat(result[0].type).isEqualTo(AccountType.EXPENSE)
        verify(accountRepository).findByOrganizationIdAndTypeAndIsActive(java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d"), AccountType.EXPENSE, true)
    }

    @Test
    fun `listAccounts should filter by parentId`() {
        val accounts = listOf(createMockAccount(id = java.util.UUID.fromString("19770c7a-8b4f-3b6e-adb1-3631faff91ec"), parentId = java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a")))
        `when`(
            accountRepository.findByOrganizationIdAndParentIdAndIsActive(java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d"), java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a"), true),
        ).thenReturn(accounts)

        val result = accountService.listAccounts(java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d"), type = null, parentId = java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a"))

        assertThat(result).hasSize(1)
        assertThat(result[0].parentId).isEqualTo(java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a"))
        verify(accountRepository).findByOrganizationIdAndParentIdAndIsActive(java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d"), java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a"), true)
    }

    @Test
    fun `listAccounts should filter by type and parentId combined`() {
        val accounts = listOf(createMockAccount(id = java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a")))
        `when`(
            accountRepository.findByOrganizationIdAndTypeAndParentIdAndIsActive(java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d"), AccountType.ASSET, java.util.UUID.fromString("a287d498-1fce-3c08-926f-097971e137a0"), true),
        ).thenReturn(accounts)

        val result = accountService.listAccounts(java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d"), type = AccountType.ASSET, parentId = java.util.UUID.fromString("a287d498-1fce-3c08-926f-097971e137a0"))

        assertThat(result).hasSize(1)
        verify(accountRepository).findByOrganizationIdAndTypeAndParentIdAndIsActive(java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d"), AccountType.ASSET, java.util.UUID.fromString("a287d498-1fce-3c08-926f-097971e137a0"), true)
    }

    @Test
    fun `deleteAccount should soft delete account`() {
        val account = createMockAccount(id = java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a"), isSystemAccount = false)
        `when`(accountRepository.findById(java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a"))).thenReturn(Optional.of(account))
        `when`(accountRepository.existsByOrganizationIdAndParentIdAndIsActive(java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d"), java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a"), true)).thenReturn(false)
        `when`(accountRepository.save(any<Account>())).thenAnswer { it.arguments[0] }

        accountService.deleteAccount(java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a"), java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d"))

        val captor = argumentCaptor<Account>()
        verify(accountRepository).save(captor.capture())
        assertThat(captor.firstValue.isActive).isFalse()
        assertThat(captor.firstValue.id).isEqualTo(java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a"))
    }

    @Test
    fun `deleteAccount should throw when system account`() {
        val account = createMockAccount(id = java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a"), isSystemAccount = true)
        `when`(accountRepository.findById(java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a"))).thenReturn(Optional.of(account))

        val exception =
            assertThrows<BusinessRuleException> {
                accountService.deleteAccount(java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a"), java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d"))
            }
        assertThat(exception.message).isEqualTo("System accounts cannot be deleted")
    }

    @Test
    fun `deleteAccount should throw when has children`() {
        val account = createMockAccount(id = java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a"), isSystemAccount = false)
        `when`(accountRepository.findById(java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a"))).thenReturn(Optional.of(account))
        `when`(accountRepository.existsByOrganizationIdAndParentIdAndIsActive(java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d"), java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a"), true)).thenReturn(true)

        val exception =
            assertThrows<BusinessRuleException> {
                accountService.deleteAccount(java.util.UUID.fromString("6c9034de-2612-334f-98d8-57e3ec94932a"), java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d"))
            }
        assertThat(exception.message).isEqualTo("Cannot delete account with child accounts")
    }

    @Test
    fun `seedDefaultAccounts should create 22 default accounts`() {
        `when`(accountRepository.saveAll(any<List<Account>>())).thenAnswer { it.arguments[0] }

        accountService.seedDefaultAccounts(java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d"))

        @Suppress("UNCHECKED_CAST")
        val captor = argumentCaptor<List<Account>>()
        verify(accountRepository).saveAll(captor.capture())

        val savedAccounts = captor.firstValue
        assertThat(savedAccounts).hasSize(22)
        assertThat(savedAccounts).allMatch { it.isSystemAccount }
        assertThat(savedAccounts).allMatch { it.organizationId == java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d") }
        assertThat(savedAccounts).allMatch { it.isActive }
    }

    private fun createMockAccount(
        id: UUID = java.util.UUID.randomUUID(),
        code: String = "1000",
        name: String = "Cash",
        type: AccountType = AccountType.ASSET,
        parentId: UUID? = null,
        organizationId: UUID = java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d"),
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
