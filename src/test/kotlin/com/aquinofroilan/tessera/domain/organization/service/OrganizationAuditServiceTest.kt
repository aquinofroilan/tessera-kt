package com.aquinofroilan.tessera.domain.organization.service

import com.aquinofroilan.tessera.domain.auth.model.RoleAssignment
import com.aquinofroilan.tessera.domain.auth.model.User
import com.aquinofroilan.tessera.domain.organization.model.AuditAction
import com.aquinofroilan.tessera.domain.organization.model.AuditCategory
import com.aquinofroilan.tessera.domain.organization.model.OrganizationAuditLog
import com.aquinofroilan.tessera.domain.organization.repository.OrganizationAuditLogRepository
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import com.aquinofroilan.tessera.security.AuthenticationContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.domain.Specification
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import tools.jackson.databind.ObjectMapper
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID

class OrganizationAuditServiceTest {
    private lateinit var auditLogRepository: OrganizationAuditLogRepository
    private lateinit var objectMapper: ObjectMapper
    private lateinit var authContext: AuthenticationContext
    private lateinit var service: OrganizationAuditService

    private val orgId = UUID.fromString("4abe9f6d-6df3-6e5c-953e-3695db9a5216")
    private val userId = UUID.fromString("bc17c97c-3d89-7d43-b7e0-7ca0266eafa8")

    @BeforeEach
    fun setUp() {
        auditLogRepository = mock(OrganizationAuditLogRepository::class.java)
        objectMapper = ObjectMapper()
        authContext = mock(AuthenticationContext::class.java)
        service = OrganizationAuditService(auditLogRepository, objectMapper, authContext)

        val testUser =
            User(
                uuid = userId,
                username = "admin_user",
                email = "admin@example.com",
                firstName = "Admin",
                lastName = "User",
                passwordHash = "encoded",
                organizationId = orgId,
                roleAssignments = listOf(RoleAssignment("OWNER", orgId)),
            )
        val authentication =
            UsernamePasswordAuthenticationToken(
                testUser,
                null,
                listOf(SimpleGrantedAuthority("ROLE_OWNER")),
            )
        SecurityContextHolder.getContext().authentication = authentication

        `when`(authContext.userId()).thenReturn(userId)
        `when`(authContext.organizationId()).thenReturn(orgId)
    }

    @Test
    fun `logEvent persists audit log entry with actor and serialized values`() {
        `when`(auditLogRepository.save(any<OrganizationAuditLog>())).thenAnswer { it.arguments[0] }

        val log =
            service.logEvent(
                organizationId = orgId,
                action = AuditAction.ORG_SETTINGS_UPDATED.name,
                category = AuditCategory.SETTINGS,
                entityType = "ORGANIZATION",
                entityId = orgId.toString(),
                oldValue = mapOf("name" to "Old Name"),
                newValue = mapOf("name" to "New Name"),
            )

        assertNotNull(log.id)
        assertEquals(orgId, log.organizationId)
        assertEquals(userId, log.actorId)
        assertEquals("admin_user", log.actorName)
        assertEquals(AuditAction.ORG_SETTINGS_UPDATED.name, log.action)
        assertEquals(AuditCategory.SETTINGS, log.category)
        assertTrue(log.oldValue!!.contains("Old Name"))
        assertTrue(log.newValue!!.contains("New Name"))
    }

    @Test
    fun `getAuditLogs returns paginated list of audit responses`() {
        val auditLog =
            OrganizationAuditLog(
                id = UUID.randomUUID(),
                organizationId = orgId,
                actorId = userId,
                actorName = "admin_user",
                action = AuditAction.ORG_STATUS_CHANGED.name,
                category = AuditCategory.LIFECYCLE,
                entityType = "ORGANIZATION",
                entityId = orgId.toString(),
                oldValue = "{\"status\":\"ACTIVE\"}",
                newValue = "{\"status\":\"SUSPENDED\"}",
                createdAt = LocalDateTime.now(),
            )

        val page = PageImpl(listOf(auditLog))
        `when`(auditLogRepository.findAll(any<Specification<OrganizationAuditLog>>(), any<PageRequest>()))
            .thenReturn(page)

        val result = service.getAuditLogs(orgId, AuditCategory.LIFECYCLE, null, null, null, null, PageRequest.of(0, 10))

        assertEquals(1, result.totalElements)
        val first = result.content[0]
        assertEquals(AuditAction.ORG_STATUS_CHANGED.name, first.action)
        assertEquals(AuditCategory.LIFECYCLE, first.category)
        assertEquals("admin_user", first.actorName)
    }

    @Test
    fun `getAuditLogById returns audit log when found`() {
        val logId = UUID.randomUUID()
        val auditLog =
            OrganizationAuditLog(
                id = logId,
                organizationId = orgId,
                actorId = userId,
                actorName = "admin_user",
                action = AuditAction.BILLING_PLAN_UPDATED.name,
                category = AuditCategory.BILLING,
                entityType = "ORGANIZATION",
                entityId = orgId.toString(),
                oldValue = "{\"plan\":\"FREE\"}",
                newValue = "{\"plan\":\"ENTERPRISE\"}",
            )

        `when`(auditLogRepository.findByOrganizationIdAndId(orgId, logId)).thenReturn(Optional.of(auditLog))

        val response = service.getAuditLogById(orgId, logId)

        assertEquals(logId, response.id)
        assertEquals(AuditAction.BILLING_PLAN_UPDATED.name, response.action)
        assertEquals(AuditCategory.BILLING, response.category)
    }

    @Test
    fun `getAuditLogById throws ResourceNotFoundException when not found`() {
        val logId = UUID.randomUUID()
        `when`(auditLogRepository.findByOrganizationIdAndId(orgId, logId)).thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> {
            service.getAuditLogById(orgId, logId)
        }
    }
}
