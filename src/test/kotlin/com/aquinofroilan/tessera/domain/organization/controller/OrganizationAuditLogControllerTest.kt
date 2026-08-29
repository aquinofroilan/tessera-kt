package com.aquinofroilan.tessera.domain.organization.controller

import com.aquinofroilan.tessera.aspect.LoggingAspect
import com.aquinofroilan.tessera.config.TestSecurityConfig
import com.aquinofroilan.tessera.config.WebMvcConfig
import com.aquinofroilan.tessera.domain.auth.model.RoleAssignment
import com.aquinofroilan.tessera.domain.auth.model.User
import com.aquinofroilan.tessera.domain.auth.repository.InvitationRepository
import com.aquinofroilan.tessera.domain.auth.repository.PasswordResetTokenRepository
import com.aquinofroilan.tessera.domain.auth.repository.RefreshTokenRepository
import com.aquinofroilan.tessera.domain.auth.repository.SessionTokenRepository
import com.aquinofroilan.tessera.domain.auth.repository.UserRepository
import com.aquinofroilan.tessera.domain.auth.service.ApiKeyService
import com.aquinofroilan.tessera.domain.auth.service.AuthService
import com.aquinofroilan.tessera.domain.finance.service.AccountService
import com.aquinofroilan.tessera.domain.finance.service.JournalEntryService
import com.aquinofroilan.tessera.domain.organization.dto.AuditLogResponse
import com.aquinofroilan.tessera.domain.organization.model.AuditAction
import com.aquinofroilan.tessera.domain.organization.model.AuditCategory
import com.aquinofroilan.tessera.domain.organization.repository.OrganizationRepository
import com.aquinofroilan.tessera.domain.organization.service.OrganizationAuditService
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.security.RolePermissionCache
import com.aquinofroilan.tessera.security.SessionContext
import com.aquinofroilan.tessera.security.TesseraPermissionEvaluator
import com.aquinofroilan.tessera.util.TokenHasher
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageImpl
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime
import java.util.UUID

@WebMvcTest(controllers = [OrganizationAuditLogController::class])
@Import(WebMvcConfig::class, LoggingAspect::class, TestSecurityConfig::class, TesseraPermissionEvaluator::class)
@ActiveProfiles("test")
class OrganizationAuditLogControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var auditService: OrganizationAuditService

    @MockitoBean
    private lateinit var authService: AuthService

    @MockitoBean
    private lateinit var sessionTokenRepository: SessionTokenRepository

    @MockitoBean
    private lateinit var userRepository: UserRepository

    @MockitoBean
    private lateinit var organizationRepository: OrganizationRepository

    @MockitoBean
    private lateinit var refreshTokenRepository: RefreshTokenRepository

    @MockitoBean
    private lateinit var passwordResetTokenRepository: PasswordResetTokenRepository

    @MockitoBean
    private lateinit var invitationRepository: InvitationRepository

    @MockitoBean
    private lateinit var tokenHasher: TokenHasher

    @MockitoBean
    private lateinit var rolePermissionCache: RolePermissionCache

    @MockitoBean
    private lateinit var apiKeyService: ApiKeyService

    @MockitoBean
    private lateinit var accountService: AccountService

    @MockitoBean
    private lateinit var journalEntryService: JournalEntryService

    @MockitoBean
    private lateinit var authenticationContext: AuthenticationContext

    private val testOrgId = UUID.fromString("4abe9f6d-6df3-6e5c-953e-3695db9a5216")
    private val testUserId = UUID.fromString("bc17c97c-3d89-7d43-b7e0-7ca0266eafa8")
    private val testLogId = UUID.fromString("11111111-2222-3333-4444-555555555555")

    private val testUser =
        User(
            uuid = testUserId,
            username = "testowner",
            email = "owner@example.com",
            firstName = "Test",
            lastName = "Owner",
            passwordHash = "encoded",
            organizationId = testOrgId,
            roleAssignments = listOf(RoleAssignment("OWNER", testOrgId)),
        )

    @BeforeEach
    fun setup() {
        setupAuthWithPermissions("organization:read", "organization:write")
        `when`(authenticationContext.organizationId()).thenReturn(testOrgId)
        `when`(authenticationContext.userId()).thenReturn(testUserId)
    }

    private fun setupAuthWithPermissions(vararg permissions: String) {
        val roleAuthorities = testUser.roleAssignments.map { SimpleGrantedAuthority("ROLE_${it.role}") }
        val permissionAuthorities = permissions.map { SimpleGrantedAuthority(it) }
        val authentication = UsernamePasswordAuthenticationToken(testUser, null, roleAuthorities + permissionAuthorities)
        authentication.details =
            SessionContext(
                sessionId = UUID.fromString("79c5ca4c-8e48-a8f8-6ffc-5b3271a250aa"),
                organizationId = testOrgId,
            )
        SecurityContextHolder.getContext().authentication = authentication
    }

    private fun createAuditLogResponse() =
        AuditLogResponse(
            id = testLogId,
            organizationId = testOrgId,
            actorId = testUserId,
            actorName = "testowner",
            action = AuditAction.ORG_SETTINGS_UPDATED.name,
            category = AuditCategory.SETTINGS,
            entityType = "ORGANIZATION",
            entityId = testOrgId.toString(),
            oldValue = "{\"name\":\"Old Corp\"}",
            newValue = "{\"name\":\"New Corp\"}",
            ipAddress = "127.0.0.1",
            userAgent = "Mozilla/5.0",
            createdAt = LocalDateTime.of(2026, 1, 1, 12, 0),
        )

    @Test
    fun `GET audit logs should return 200 with paginated audit logs`() {
        val page = PageImpl(listOf(createAuditLogResponse()))
        `when`(auditService.getAuditLogs(eq(testOrgId), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), any()))
            .thenReturn(page)

        mockMvc
            .perform(get("/api/v1/organization/audit-logs"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content[0].id").value(testLogId.toString()))
            .andExpect(jsonPath("$.content[0].action").value("ORG_SETTINGS_UPDATED"))
            .andExpect(jsonPath("$.content[0].category").value("SETTINGS"))
            .andExpect(jsonPath("$.content[0].actorName").value("testowner"))
    }

    @Test
    fun `GET audit log by id should return 200 with audit details`() {
        `when`(auditService.getAuditLogById(testOrgId, testLogId)).thenReturn(createAuditLogResponse())

        mockMvc
            .perform(get("/api/v1/organization/audit-logs/$testLogId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(testLogId.toString()))
            .andExpect(jsonPath("$.action").value("ORG_SETTINGS_UPDATED"))
    }

    @Test
    fun `GET audit logs should return 403 when missing organization read`() {
        setupAuthWithPermissions("inventory:read")

        mockMvc
            .perform(get("/api/v1/organization/audit-logs"))
            .andExpect(status().isForbidden)
    }
}
