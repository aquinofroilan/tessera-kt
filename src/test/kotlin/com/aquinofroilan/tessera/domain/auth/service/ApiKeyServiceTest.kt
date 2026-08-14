package com.aquinofroilan.tessera.domain.auth.service

import com.aquinofroilan.tessera.domain.auth.model.ApiKey
import com.aquinofroilan.tessera.domain.auth.repository.ApiKeyRepository
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import com.aquinofroilan.tessera.util.TokenHasher
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Optional
import java.util.UUID

class ApiKeyServiceTest {
    private lateinit var apiKeyService: ApiKeyService
    private lateinit var apiKeyRepository: ApiKeyRepository
    private lateinit var tokenHasher: TokenHasher

    @BeforeEach
    fun setup() {
        apiKeyRepository = mock(ApiKeyRepository::class.java)
        tokenHasher = mock(TokenHasher::class.java)

        `when`(tokenHasher.hash(any())).thenAnswer { "hashed-${it.arguments[0]}" }
        `when`(tokenHasher.generate(any())).thenReturn("generated-api-key-token")

        apiKeyService =
            ApiKeyService(
                apiKeyRepository = apiKeyRepository,
                tokenHasher = tokenHasher,
            )
    }

    @Test
    fun `createApiKey should create key and return raw key`() {
        `when`(apiKeyRepository.save(any<ApiKey>())).thenAnswer { it.arguments[0] }

        val (apiKey, rawKey) =
            apiKeyService.createApiKey(
                name = "Test Key",
                permissions = listOf("session:read", "organization:read"),
                organizationId = java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d"),
                createdBy = java.util.UUID.fromString("3a01035d-c5db-3981-bf73-f18b3a0c1df9"),
                creatorPermissions = setOf("session:read", "session:delete", "organization:read"),
            )

        assertThat(apiKey.name).isEqualTo("Test Key")
        assertThat(apiKey.organizationId).isEqualTo(java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d"))
        assertThat(apiKey.createdBy).isEqualTo(java.util.UUID.fromString("3a01035d-c5db-3981-bf73-f18b3a0c1df9"))
        assertThat(apiKey.permissions).isEqualTo(listOf("session:read", "organization:read"))
        assertThat(apiKey.keyPrefix.take(8)).isEqualTo("generate")
        assertThat(rawKey).isNotNull()

        val captor = argumentCaptor<ApiKey>()
        verify(apiKeyRepository).save(captor.capture())
        assertThat(captor.firstValue.isActive).isTrue()
    }

    @Test
    fun `createApiKey should throw when permissions are invalid`() {
        val exception =
            assertThrows<BusinessRuleException> {
                apiKeyService.createApiKey(
                    name = "Bad Key",
                    permissions = listOf("session:read", "nonexistent:permission"),
                    organizationId = java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d"),
                    createdBy = java.util.UUID.fromString("3a01035d-c5db-3981-bf73-f18b3a0c1df9"),
                    creatorPermissions = setOf("session:read"),
                )
            }
        assertThat(exception.message).contains("nonexistent:permission")
    }

    @Test
    fun `createApiKey should throw when permissions are empty`() {
        val exception =
            assertThrows<BusinessRuleException> {
                apiKeyService.createApiKey(
                    name = "Empty Key",
                    permissions = emptyList(),
                    organizationId = java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d"),
                    createdBy = java.util.UUID.fromString("3a01035d-c5db-3981-bf73-f18b3a0c1df9"),
                    creatorPermissions = setOf("session:read"),
                )
            }
        assertThat(exception.message).isEqualTo("At least one permission is required")
    }

    @Test
    fun `createApiKey should throw when requesting permissions creator does not have`() {
        val exception =
            assertThrows<BusinessRuleException> {
                apiKeyService.createApiKey(
                    name = "Escalated Key",
                    permissions = listOf("session:read", "user:delete"),
                    organizationId = java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d"),
                    createdBy = java.util.UUID.fromString("3a01035d-c5db-3981-bf73-f18b3a0c1df9"),
                    creatorPermissions = setOf("session:read", "organization:read"),
                )
            }
        assertThat(exception.message).contains("user:delete")
    }

    @Test
    fun `authenticateByApiKey should return api key for valid key`() {
        val apiKey = createMockApiKey()
        `when`(apiKeyRepository.findByKeyHash("hashed-valid-key")).thenReturn(Optional.of(apiKey))

        val result = apiKeyService.authenticateByApiKey("valid-key")

        assertThat(result).isNotNull()
        assertThat(result?.id).isEqualTo(apiKey.id)
    }

    @Test
    fun `authenticateByApiKey should return null for inactive key`() {
        val apiKey = createMockApiKey(isActive = false)
        `when`(apiKeyRepository.findByKeyHash("hashed-inactive-key")).thenReturn(Optional.of(apiKey))

        val result = apiKeyService.authenticateByApiKey("inactive-key")

        assertThat(result).isNull()
    }

    @Test
    fun `authenticateByApiKey should return null for expired key`() {
        val apiKey = createMockApiKey(expiresAt = LocalDateTime.now(ZoneOffset.UTC).minusHours(1))
        `when`(apiKeyRepository.findByKeyHash("hashed-expired-key")).thenReturn(Optional.of(apiKey))

        val result = apiKeyService.authenticateByApiKey("expired-key")

        assertThat(result).isNull()
    }

    @Test
    fun `authenticateByApiKey should return null for unknown key`() {
        `when`(apiKeyRepository.findByKeyHash("hashed-unknown")).thenReturn(Optional.empty())

        val result = apiKeyService.authenticateByApiKey("unknown")

        assertThat(result).isNull()
    }

    @Test
    fun `listApiKeys should return active keys for org`() {
        val keys = listOf(createMockApiKey(), createMockApiKey(name = "Key 2"))
        `when`(
            apiKeyRepository.findByOrganizationIdAndIsActive(java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d"), true),
        ).thenReturn(keys)

        val result = apiKeyService.listApiKeys(java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d"))

        assertThat(result.size).isEqualTo(2)
    }

    @Test
    fun `revokeApiKey should set isActive to false`() {
        val apiKey = createMockApiKey()
        `when`(apiKeyRepository.findById(apiKey.id)).thenReturn(Optional.of(apiKey))
        `when`(apiKeyRepository.save(any<ApiKey>())).thenAnswer { it.arguments[0] }

        apiKeyService.revokeApiKey(apiKey.id, java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d"))

        val captor = argumentCaptor<ApiKey>()
        verify(apiKeyRepository).save(captor.capture())
        assertThat(captor.firstValue.isActive).isEqualTo(false)
    }

    @Test
    fun `revokeApiKey should throw when key not in same org`() {
        val apiKey = createMockApiKey(organizationId = java.util.UUID.fromString("fbede99a-0bef-3bf9-ba0b-8d28f050479d"))
        `when`(apiKeyRepository.findById(apiKey.id)).thenReturn(Optional.of(apiKey))

        val exception =
            assertThrows<ResourceNotFoundException> {
                apiKeyService.revokeApiKey(apiKey.id, java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d"))
            }
        assertThat(exception.message).isEqualTo("API key not found")
    }

    @Test
    fun `revokeApiKey should throw when key already revoked`() {
        val apiKey = createMockApiKey(isActive = false)
        `when`(apiKeyRepository.findById(apiKey.id)).thenReturn(Optional.of(apiKey))

        val exception =
            assertThrows<BusinessRuleException> {
                apiKeyService.revokeApiKey(apiKey.id, java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d"))
            }
        assertThat(exception.message).isEqualTo("API key is already revoked")
    }

    private fun createMockApiKey(
        name: String = "Test Key",
        organizationId: UUID = java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d"),
        isActive: Boolean = true,
        expiresAt: LocalDateTime? = null,
    ) = ApiKey(
        id = java.util.UUID.fromString("6b9a90d2-9f77-3ac1-89b4-41f2c27bb55a"),
        name = name,
        keyHash = "07f345f8-a2e7-3f88-86bf-8ebd86d33f23",
        keyPrefix = "generate",
        organizationId = organizationId,
        permissions = listOf("session:read", "organization:read"),
        createdBy = java.util.UUID.fromString("3a01035d-c5db-3981-bf73-f18b3a0c1df9"),
        isActive = isActive,
        expiresAt = expiresAt,
    )
}
