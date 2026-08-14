package com.aquinofroilan.tessera.domain.auth.service

import com.aquinofroilan.tessera.domain.auth.model.LoginLinkToken
import com.aquinofroilan.tessera.domain.auth.model.RefreshToken
import com.aquinofroilan.tessera.domain.auth.model.SessionToken
import com.aquinofroilan.tessera.domain.auth.model.User
import com.aquinofroilan.tessera.domain.auth.repository.LoginLinkTokenRepository
import com.aquinofroilan.tessera.domain.auth.repository.RefreshTokenRepository
import com.aquinofroilan.tessera.domain.auth.repository.SessionTokenRepository
import com.aquinofroilan.tessera.domain.auth.repository.UserRepository
import com.aquinofroilan.tessera.exception.AuthenticationException
import com.aquinofroilan.tessera.util.TokenHasher
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Optional

class LoginLinkServiceTest {
    private lateinit var userRepository: UserRepository
    private lateinit var loginLinkTokenRepository: LoginLinkTokenRepository
    private lateinit var sessionTokenRepository: SessionTokenRepository
    private lateinit var refreshTokenRepository: RefreshTokenRepository
    private lateinit var tokenHasher: TokenHasher
    private lateinit var service: LoginLinkService

    private val email = "ada@example.com"
    private val orgId = java.util.UUID.fromString("e5628ca4-87a8-3e6f-8ae2-20213cc7ef92")
    private val userId = java.util.UUID.fromString("1fd9446c-9f04-31e6-941e-53b391d01cab")

    @BeforeEach
    fun setup() {
        userRepository = mock(UserRepository::class.java)
        loginLinkTokenRepository = mock(LoginLinkTokenRepository::class.java)
        sessionTokenRepository = mock(SessionTokenRepository::class.java)
        refreshTokenRepository = mock(RefreshTokenRepository::class.java)
        tokenHasher = TokenHasher("SHA-256").apply { validate() }
        whenever(loginLinkTokenRepository.save(any<LoginLinkToken>())).thenAnswer { it.arguments[0] }
        whenever(sessionTokenRepository.save(any<SessionToken>())).thenAnswer { it.arguments[0] }
        whenever(refreshTokenRepository.save(any<RefreshToken>())).thenAnswer { it.arguments[0] }
        service =
            LoginLinkService(
                userRepository,
                loginLinkTokenRepository,
                sessionTokenRepository,
                refreshTokenRepository,
                tokenHasher,
                linkExpiryMinutes = 15,
                tokenValidityMs = 60_000,
                refreshTokenValidityMs = 600_000,
            )
    }

    @Test
    fun `request returns null for unknown email (uniform response)`() {
        whenever(userRepository.findByEmail(email)).thenReturn(Optional.empty())
        assertThat(service.request(email)).isNull()
    }

    @Test
    fun `request returns null for inactive user`() {
        whenever(userRepository.findByEmail(email)).thenReturn(Optional.of(user(isActive = false)))
        assertThat(service.request(email)).isNull()
    }

    @Test
    fun `request issues a raw token and wipes outstanding ones`() {
        whenever(userRepository.findByEmail(email)).thenReturn(Optional.of(user()))
        val rawToken = service.request(email)
        assertThat(rawToken).isNotBlank
        org.mockito.kotlin
            .verify(loginLinkTokenRepository)
            .deleteByUserId(userId)
    }

    @Test
    fun `consume rejects unknown token`() {
        whenever(loginLinkTokenRepository.findByTokenHash(any())).thenReturn(Optional.empty())
        assertThatThrownBy { service.consume("garbage") }
            .isInstanceOf(AuthenticationException::class.java)
    }

    @Test
    fun `consume rejects expired token and cleans it up`() {
        val token = tokenHasher.generate(32)
        val hash = tokenHasher.hash(token)
        val expired =
            LoginLinkToken(
                tokenHash = hash,
                userId = userId,
                expiryAt = LocalDateTime.now(ZoneOffset.UTC).minusMinutes(1),
            )
        whenever(loginLinkTokenRepository.findByTokenHash(eq(hash))).thenReturn(Optional.of(expired))
        assertThatThrownBy { service.consume(token) }
            .isInstanceOf(AuthenticationException::class.java)
    }

    @Test
    fun `consume rejects already-consumed token`() {
        val token = tokenHasher.generate(32)
        val hash = tokenHasher.hash(token)
        val consumed =
            LoginLinkToken(
                tokenHash = hash,
                userId = userId,
                expiryAt = LocalDateTime.now(ZoneOffset.UTC).plusMinutes(5),
                consumedAt = LocalDateTime.now(ZoneOffset.UTC),
            )
        whenever(loginLinkTokenRepository.findByTokenHash(eq(hash))).thenReturn(Optional.of(consumed))
        assertThatThrownBy { service.consume(token) }
            .isInstanceOf(AuthenticationException::class.java)
    }

    @Test
    fun `consume on valid token mints session + refresh tokens`() {
        val token = tokenHasher.generate(32)
        val hash = tokenHasher.hash(token)
        whenever(loginLinkTokenRepository.findByTokenHash(eq(hash))).thenReturn(
            Optional.of(
                LoginLinkToken(
                    tokenHash = hash,
                    userId = userId,
                    expiryAt = LocalDateTime.now(ZoneOffset.UTC).plusMinutes(5),
                ),
            ),
        )
        whenever(userRepository.findById(userId)).thenReturn(Optional.of(user()))

        val response = service.consume(token, ipAddress = "1.2.3.4", userAgent = "ua")

        assertThat(response.accessToken).isNotBlank
        assertThat(response.refreshToken).isNotBlank
        assertThat(response.username).isEqualTo("ada")
        assertThat(response.organizationId).isEqualTo(orgId)
    }

    @Test
    fun `consume rejects when user has become inactive after request`() {
        val token = tokenHasher.generate(32)
        val hash = tokenHasher.hash(token)
        whenever(loginLinkTokenRepository.findByTokenHash(eq(hash))).thenReturn(
            Optional.of(
                LoginLinkToken(
                    tokenHash = hash,
                    userId = userId,
                    expiryAt = LocalDateTime.now(ZoneOffset.UTC).plusMinutes(5),
                ),
            ),
        )
        whenever(userRepository.findById(userId)).thenReturn(Optional.of(user(isActive = false)))
        assertThatThrownBy { service.consume(token) }
            .isInstanceOf(AuthenticationException::class.java)
    }

    private fun user(isActive: Boolean = true) =
        User(
            uuid = userId,
            username = "ada",
            email = email,
            firstName = "Ada",
            lastName = "Lovelace",
            passwordHash = "ignored",
            organizationId = orgId,
            isActive = isActive,
        )
}
