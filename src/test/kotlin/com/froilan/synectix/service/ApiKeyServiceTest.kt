package com.froilan.synectix.service

import com.froilan.synectix.model.ApiKey
import com.froilan.synectix.repository.ApiKeyRepository
import com.froilan.synectix.util.TokenHasher
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.springframework.data.mongodb.core.MongoTemplate
import java.time.LocalDateTime
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ApiKeyServiceTest {
    private lateinit var apiKeyService: ApiKeyService
    private lateinit var apiKeyRepository: ApiKeyRepository
    private lateinit var mongoTemplate: MongoTemplate
    private lateinit var tokenHasher: TokenHasher

    @BeforeEach
    fun setup() {
        apiKeyRepository = mock(ApiKeyRepository::class.java)
        mongoTemplate = mock(MongoTemplate::class.java)
        tokenHasher = mock(TokenHasher::class.java)

        `when`(tokenHasher.hash(any())).thenAnswer { "hashed-${it.arguments[0]}" }
        `when`(tokenHasher.generate(any())).thenReturn("generated-api-key-token")

        apiKeyService =
            ApiKeyService(
                apiKeyRepository = apiKeyRepository,
                mongoTemplate = mongoTemplate,
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
                organizationId = "org-123",
                createdBy = "user-123",
                creatorPermissions = setOf("session:read", "session:delete", "organization:read"),
            )

        assertEquals("Test Key", apiKey.name)
        assertEquals("org-123", apiKey.organizationId)
        assertEquals("user-123", apiKey.createdBy)
        assertEquals(listOf("session:read", "organization:read"), apiKey.permissions)
        assertEquals("generate", apiKey.keyPrefix.take(8))
        assertNotNull(rawKey)

        val captor = argumentCaptor<ApiKey>()
        verify(apiKeyRepository).save(captor.capture())
        assertTrue(captor.firstValue.isActive)
    }

    @Test
    fun `createApiKey should throw when permissions are invalid`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                apiKeyService.createApiKey(
                    name = "Bad Key",
                    permissions = listOf("session:read", "nonexistent:permission"),
                    organizationId = "org-123",
                    createdBy = "user-123",
                    creatorPermissions = setOf("session:read"),
                )
            }
        assertTrue(exception.message!!.contains("nonexistent:permission"))
    }

    @Test
    fun `createApiKey should throw when permissions are empty`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                apiKeyService.createApiKey(
                    name = "Empty Key",
                    permissions = emptyList(),
                    organizationId = "org-123",
                    createdBy = "user-123",
                    creatorPermissions = setOf("session:read"),
                )
            }
        assertEquals("At least one permission is required", exception.message)
    }

    @Test
    fun `createApiKey should throw when requesting permissions creator does not have`() {
        val exception =
            assertThrows<IllegalArgumentException> {
                apiKeyService.createApiKey(
                    name = "Escalated Key",
                    permissions = listOf("session:read", "user:delete"),
                    organizationId = "org-123",
                    createdBy = "user-123",
                    creatorPermissions = setOf("session:read", "organization:read"),
                )
            }
        assertTrue(exception.message!!.contains("user:delete"))
    }

    @Test
    fun `authenticateByApiKey should return api key for valid key`() {
        val apiKey = createMockApiKey()
        `when`(apiKeyRepository.findByKeyHash("hashed-valid-key")).thenReturn(Optional.of(apiKey))

        val result = apiKeyService.authenticateByApiKey("valid-key")

        assertNotNull(result)
        assertEquals(apiKey.id, result.id)
    }

    @Test
    fun `authenticateByApiKey should return null for inactive key`() {
        val apiKey = createMockApiKey(isActive = false)
        `when`(apiKeyRepository.findByKeyHash("hashed-inactive-key")).thenReturn(Optional.of(apiKey))

        val result = apiKeyService.authenticateByApiKey("inactive-key")

        assertNull(result)
    }

    @Test
    fun `authenticateByApiKey should return null for expired key`() {
        val apiKey = createMockApiKey(expiresAt = LocalDateTime.now().minusHours(1))
        `when`(apiKeyRepository.findByKeyHash("hashed-expired-key")).thenReturn(Optional.of(apiKey))

        val result = apiKeyService.authenticateByApiKey("expired-key")

        assertNull(result)
    }

    @Test
    fun `authenticateByApiKey should return null for unknown key`() {
        `when`(apiKeyRepository.findByKeyHash("hashed-unknown")).thenReturn(Optional.empty())

        val result = apiKeyService.authenticateByApiKey("unknown")

        assertNull(result)
    }

    @Test
    fun `listApiKeys should return active keys for org`() {
        val keys = listOf(createMockApiKey(), createMockApiKey(name = "Key 2"))
        `when`(apiKeyRepository.findByOrganizationIdAndIsActive("org-123", true)).thenReturn(keys)

        val result = apiKeyService.listApiKeys("org-123")

        assertEquals(2, result.size)
    }

    @Test
    fun `revokeApiKey should set isActive to false`() {
        val apiKey = createMockApiKey()
        `when`(apiKeyRepository.findById(apiKey.id)).thenReturn(Optional.of(apiKey))
        `when`(apiKeyRepository.save(any<ApiKey>())).thenAnswer { it.arguments[0] }

        apiKeyService.revokeApiKey(apiKey.id, "org-123")

        val captor = argumentCaptor<ApiKey>()
        verify(apiKeyRepository).save(captor.capture())
        assertEquals(false, captor.firstValue.isActive)
    }

    @Test
    fun `revokeApiKey should throw when key not in same org`() {
        val apiKey = createMockApiKey(organizationId = "other-org")
        `when`(apiKeyRepository.findById(apiKey.id)).thenReturn(Optional.of(apiKey))

        val exception =
            assertThrows<IllegalArgumentException> {
                apiKeyService.revokeApiKey(apiKey.id, "org-123")
            }
        assertEquals("API key not found", exception.message)
    }

    @Test
    fun `revokeApiKey should throw when key already revoked`() {
        val apiKey = createMockApiKey(isActive = false)
        `when`(apiKeyRepository.findById(apiKey.id)).thenReturn(Optional.of(apiKey))

        val exception =
            assertThrows<IllegalArgumentException> {
                apiKeyService.revokeApiKey(apiKey.id, "org-123")
            }
        assertEquals("API key is already revoked", exception.message)
    }

    private fun createMockApiKey(
        name: String = "Test Key",
        organizationId: String = "org-123",
        isActive: Boolean = true,
        expiresAt: LocalDateTime? = null,
    ) = ApiKey(
        id = "key-123",
        name = name,
        keyHash = "hashed-key",
        keyPrefix = "generate",
        organizationId = organizationId,
        permissions = listOf("session:read", "organization:read"),
        createdBy = "user-123",
        isActive = isActive,
        expiresAt = expiresAt,
    )
}
