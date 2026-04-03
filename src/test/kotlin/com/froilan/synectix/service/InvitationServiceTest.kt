package com.froilan.synectix.service

import com.froilan.synectix.dto.AcceptInvitationRequest
import com.froilan.synectix.dto.CreateInvitationRequest
import com.froilan.synectix.model.Invitation
import com.froilan.synectix.model.InvitationStatus
import com.froilan.synectix.model.Role
import com.froilan.synectix.model.RoleAssignment
import com.froilan.synectix.model.RoleLevel
import com.froilan.synectix.model.User
import com.froilan.synectix.repository.InvitationRepository
import com.froilan.synectix.repository.RoleRepository
import com.froilan.synectix.repository.UserRepository
import com.froilan.synectix.util.TokenHasher
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.springframework.data.mongodb.core.FindAndModifyOptions
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.LocalDateTime
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class InvitationServiceTest {
    private lateinit var invitationService: InvitationService
    private lateinit var invitationRepository: InvitationRepository
    private lateinit var userRepository: UserRepository
    private lateinit var roleRepository: RoleRepository
    private lateinit var mongoTemplate: MongoTemplate
    private lateinit var tokenHasher: TokenHasher
    private lateinit var passwordEncoder: PasswordEncoder

    @BeforeEach
    fun setup() {
        invitationRepository = mock(InvitationRepository::class.java)
        userRepository = mock(UserRepository::class.java)
        roleRepository = mock(RoleRepository::class.java)
        mongoTemplate = mock(MongoTemplate::class.java)
        tokenHasher = mock(TokenHasher::class.java)
        passwordEncoder = mock(PasswordEncoder::class.java)

        `when`(tokenHasher.hash(any())).thenAnswer { "hashed-${it.arguments[0]}" }

        invitationService =
            InvitationService(
                invitationRepository = invitationRepository,
                userRepository = userRepository,
                roleRepository = roleRepository,
                mongoTemplate = mongoTemplate,
                tokenHasher = tokenHasher,
                passwordEncoder = passwordEncoder,
                invitationExpiryHours = 72L,
            )
    }

    @Test
    fun `invite should create invitation and return raw token`() {
        val request = CreateInvitationRequest(email = "newuser@example.com", role = "MEMBER")
        val inviter = createMockUser()
        val memberRole = Role(name = "MEMBER", description = "Member", level = RoleLevel.ORGANIZATION)

        `when`(roleRepository.findByName("MEMBER")).thenReturn(Optional.of(memberRole))
        `when`(invitationRepository.findByEmailAndOrganizationIdAndStatusAndExpiryAtAfter(any(), any(), any(), any()))
            .thenReturn(Optional.empty())
        `when`(invitationRepository.save(any<Invitation>())).thenAnswer { it.arguments[0] }

        val token = invitationService.invite(request, inviter)

        assertNotNull(token)
        assertTrue(token.isNotEmpty())

        val captor = argumentCaptor<Invitation>()
        verify(invitationRepository).save(captor.capture())
        assertEquals("newuser@example.com", captor.firstValue.email)
        assertEquals("MEMBER", captor.firstValue.role)
        assertEquals(inviter.organizationId, captor.firstValue.organizationId)
        assertEquals(inviter.uuid, captor.firstValue.invitedBy)
        assertEquals(InvitationStatus.PENDING, captor.firstValue.status)
    }

    @Test
    fun `invite should throw when role does not exist`() {
        val request = CreateInvitationRequest(email = "newuser@example.com", role = "NONEXISTENT")
        val inviter = createMockUser()

        `when`(roleRepository.findByName("NONEXISTENT")).thenReturn(Optional.empty())

        val exception = assertThrows<IllegalArgumentException> { invitationService.invite(request, inviter) }
        assertEquals("Role 'NONEXISTENT' does not exist", exception.message)
    }

    @Test
    fun `invite should throw when role is system level`() {
        val request = CreateInvitationRequest(email = "newuser@example.com", role = "SUPER_ADMIN")
        val inviter = createMockUser()
        val systemRole = Role(name = "SUPER_ADMIN", description = "System admin", level = RoleLevel.SYSTEM)

        `when`(roleRepository.findByName("SUPER_ADMIN")).thenReturn(Optional.of(systemRole))

        val exception = assertThrows<IllegalArgumentException> { invitationService.invite(request, inviter) }
        assertEquals("Cannot invite with system-level role", exception.message)
    }

    @Test
    fun `invite should throw when pending invitation already exists`() {
        val request = CreateInvitationRequest(email = "existing@example.com", role = "MEMBER")
        val inviter = createMockUser()
        val memberRole = Role(name = "MEMBER", description = "Member", level = RoleLevel.ORGANIZATION)
        val existingInvitation = createMockInvitation()

        `when`(roleRepository.findByName("MEMBER")).thenReturn(Optional.of(memberRole))
        `when`(
            invitationRepository.findByEmailAndOrganizationIdAndStatusAndExpiryAtAfter(
                eq("existing@example.com"),
                eq(inviter.organizationId),
                eq(InvitationStatus.PENDING),
                any(),
            ),
        ).thenReturn(Optional.of(existingInvitation))

        val exception = assertThrows<IllegalArgumentException> { invitationService.invite(request, inviter) }
        assertEquals("An invitation has already been sent to this email", exception.message)
    }

    @Test
    fun `acceptInvitation should create new user when email not registered`() {
        val request =
            AcceptInvitationRequest(
                token = "raw-token",
                username = "newuser",
                password = "SecurePass123!",
                firstName = "New",
                lastName = "User",
            )
        val invitation = createMockInvitation()

        `when`(invitationRepository.findByTokenHash("hashed-raw-token")).thenReturn(Optional.of(invitation))
        `when`(mongoTemplate.findAndModify(any(), any(), any<FindAndModifyOptions>(), eq(Invitation::class.java)))
            .thenReturn(invitation.copy(status = InvitationStatus.ACCEPTED))
        `when`(userRepository.findByEmail(invitation.email)).thenReturn(Optional.empty())
        `when`(passwordEncoder.encode("SecurePass123!")).thenReturn("encodedPassword")
        `when`(userRepository.save(any<User>())).thenAnswer { it.arguments[0] }

        val result = invitationService.acceptInvitation(request)

        assertNotNull(result)
        assertEquals("newuser", result.username)
        assertEquals(invitation.email, result.email)
        assertEquals(invitation.organizationId, result.organizationId)

        val userCaptor = argumentCaptor<User>()
        verify(userRepository).save(userCaptor.capture())
        assertEquals("MEMBER", userCaptor.firstValue.roleAssignments[0].role)
        assertEquals(invitation.organizationId, userCaptor.firstValue.roleAssignments[0].organizationId)
    }

    @Test
    fun `acceptInvitation should add role to existing user`() {
        val request =
            AcceptInvitationRequest(
                token = "raw-token",
                username = "ignored",
                password = "ignored",
                firstName = "ignored",
                lastName = "ignored",
            )
        val invitation = createMockInvitation(role = "ADMIN")
        val existingUser =
            User(
                uuid = "existing-user",
                username = "existinguser",
                email = invitation.email,
                firstName = "Existing",
                lastName = "User",
                passwordHash = "existingHash",
                organizationId = "other-org",
                roleAssignments = listOf(RoleAssignment("MEMBER", "other-org")),
            )

        `when`(invitationRepository.findByTokenHash("hashed-raw-token")).thenReturn(Optional.of(invitation))
        `when`(mongoTemplate.findAndModify(any(), any(), any<FindAndModifyOptions>(), eq(Invitation::class.java)))
            .thenReturn(invitation.copy(status = InvitationStatus.ACCEPTED))
        `when`(userRepository.findByEmail(invitation.email)).thenReturn(Optional.of(existingUser))
        `when`(userRepository.save(any<User>())).thenAnswer { it.arguments[0] }

        val result = invitationService.acceptInvitation(request)

        assertEquals(2, result.roleAssignments.size)
        assertTrue(result.roleAssignments.any { it.role == "ADMIN" && it.organizationId == invitation.organizationId })

        verify(passwordEncoder, never()).encode(any())
    }

    @Test
    fun `acceptInvitation should throw for invalid token`() {
        val request =
            AcceptInvitationRequest(
                token = "invalid",
                username = "user",
                password = "password123",
                firstName = "F",
                lastName = "L",
            )

        `when`(invitationRepository.findByTokenHash("hashed-invalid")).thenReturn(Optional.empty())

        val exception = assertThrows<IllegalArgumentException> { invitationService.acceptInvitation(request) }
        assertEquals("Invalid or expired invitation token", exception.message)
    }

    @Test
    fun `acceptInvitation should throw for expired invitation`() {
        val request =
            AcceptInvitationRequest(
                token = "expired-token",
                username = "user",
                password = "password123",
                firstName = "F",
                lastName = "L",
            )
        val expiredInvitation = createMockInvitation(expiryAt = LocalDateTime.now().minusHours(1))

        `when`(invitationRepository.findByTokenHash("hashed-expired-token")).thenReturn(Optional.of(expiredInvitation))

        val exception = assertThrows<IllegalArgumentException> { invitationService.acceptInvitation(request) }
        assertEquals("Invalid or expired invitation token", exception.message)
    }

    @Test
    fun `acceptInvitation should throw when already accepted (race condition)`() {
        val request =
            AcceptInvitationRequest(
                token = "used-token",
                username = "user",
                password = "password123",
                firstName = "F",
                lastName = "L",
            )
        val invitation = createMockInvitation()

        `when`(invitationRepository.findByTokenHash("hashed-used-token")).thenReturn(Optional.of(invitation))
        `when`(mongoTemplate.findAndModify(any(), any(), any<FindAndModifyOptions>(), eq(Invitation::class.java)))
            .thenReturn(null)

        val exception = assertThrows<IllegalArgumentException> { invitationService.acceptInvitation(request) }
        assertEquals("Invalid or expired invitation token", exception.message)
    }

    @Test
    fun `revokeInvitation should update status to REVOKED`() {
        val user = createMockUser()
        val invitation = createMockInvitation()

        `when`(invitationRepository.findById(invitation.id)).thenReturn(Optional.of(invitation))
        `when`(invitationRepository.save(any<Invitation>())).thenAnswer { it.arguments[0] }

        invitationService.revokeInvitation(invitation.id, user)

        val captor = argumentCaptor<Invitation>()
        verify(invitationRepository).save(captor.capture())
        assertEquals(InvitationStatus.REVOKED, captor.firstValue.status)
    }

    @Test
    fun `revokeInvitation should throw when invitation not in same org`() {
        val user = createMockUser()
        val invitation = createMockInvitation(organizationId = "other-org")

        `when`(invitationRepository.findById(invitation.id)).thenReturn(Optional.of(invitation))

        val exception =
            assertThrows<IllegalArgumentException> {
                invitationService.revokeInvitation(invitation.id, user)
            }
        assertEquals("Invitation not found", exception.message)
    }

    @Test
    fun `revokeInvitation should throw when invitation is not pending`() {
        val user = createMockUser()
        val invitation = createMockInvitation(status = InvitationStatus.ACCEPTED)

        `when`(invitationRepository.findById(invitation.id)).thenReturn(Optional.of(invitation))

        val exception =
            assertThrows<IllegalArgumentException> {
                invitationService.revokeInvitation(invitation.id, user)
            }
        assertEquals("Invitation is not pending", exception.message)
    }

    @Test
    fun `listInvitations should return pending invitations for org`() {
        val invitations = listOf(createMockInvitation(), createMockInvitation(email = "other@example.com"))

        `when`(invitationRepository.findByOrganizationIdAndStatus("org-123", InvitationStatus.PENDING))
            .thenReturn(invitations)

        val result = invitationService.listInvitations("org-123")

        assertEquals(2, result.size)
    }

    private fun createMockUser() =
        User(
            uuid = "user-123",
            username = "testuser",
            email = "test@example.com",
            firstName = "Test",
            lastName = "User",
            passwordHash = "encodedPassword",
            organizationId = "org-123",
            roleAssignments = listOf(RoleAssignment("OWNER", "org-123")),
        )

    private fun createMockInvitation(
        email: String = "invited@example.com",
        role: String = "MEMBER",
        organizationId: String = "org-123",
        status: InvitationStatus = InvitationStatus.PENDING,
        expiryAt: LocalDateTime = LocalDateTime.now().plusHours(72),
    ) = Invitation(
        id = "inv-123",
        email = email,
        organizationId = organizationId,
        role = role,
        tokenHash = "hashed-token",
        invitedBy = "user-123",
        status = status,
        expiryAt = expiryAt,
    )
}
