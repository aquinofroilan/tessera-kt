package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.AcceptInvitationRequest
import com.aquinofroilan.tessera.dto.CreateInvitationRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import com.aquinofroilan.tessera.model.Invitation
import com.aquinofroilan.tessera.model.InvitationStatus
import com.aquinofroilan.tessera.model.Role
import com.aquinofroilan.tessera.model.RoleAssignment
import com.aquinofroilan.tessera.model.RoleLevel
import com.aquinofroilan.tessera.model.User
import com.aquinofroilan.tessera.repository.InvitationRepository
import com.aquinofroilan.tessera.repository.RoleRepository
import com.aquinofroilan.tessera.repository.UserRepository
import com.aquinofroilan.tessera.util.TokenHasher
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.anyVararg
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.springframework.dao.DuplicateKeyException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Optional
import java.util.UUID

class InvitationServiceTest {
    private lateinit var invitationService: InvitationService
    private lateinit var invitationRepository: InvitationRepository
    private lateinit var userRepository: UserRepository
    private lateinit var roleRepository: RoleRepository
    private lateinit var jdbcTemplate: JdbcTemplate
    private lateinit var tokenHasher: TokenHasher
    private lateinit var passwordEncoder: PasswordEncoder

    @BeforeEach
    fun setup() {
        invitationRepository = mock(InvitationRepository::class.java)
        userRepository = mock(UserRepository::class.java)
        roleRepository = mock(RoleRepository::class.java)
        jdbcTemplate = mock(JdbcTemplate::class.java)
        tokenHasher = mock(TokenHasher::class.java)
        passwordEncoder = mock(PasswordEncoder::class.java)

        `when`(tokenHasher.hash(any())).thenAnswer { "hashed-${it.arguments[0]}" }
        `when`(tokenHasher.generate(any())).thenReturn("generated-test-token")

        invitationService =
            InvitationService(
                invitationRepository = invitationRepository,
                userRepository = userRepository,
                roleRepository = roleRepository,
                jdbcTemplate = jdbcTemplate,
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
        `when`(invitationRepository.findByEmailAndOrganizationIdAndStatus(any(), any(), any()))
            .thenReturn(Optional.empty())
        `when`(invitationRepository.save(any<Invitation>())).thenAnswer { it.arguments[0] }

        val token = invitationService.invite(request, inviter, inviter.organizationId)

        assertThat(token).isNotNull().isNotEmpty()

        val captor = argumentCaptor<Invitation>()
        verify(invitationRepository).save(captor.capture())
        assertThat(captor.firstValue.email).isEqualTo("newuser@example.com")
        assertThat(captor.firstValue.role).isEqualTo("MEMBER")
        assertThat(captor.firstValue.organizationId).isEqualTo(inviter.organizationId)
        assertThat(captor.firstValue.invitedBy).isEqualTo(inviter.uuid)
        assertThat(captor.firstValue.status).isEqualTo(InvitationStatus.PENDING)
    }

    @Test
    fun `invite should throw when role does not exist`() {
        val request = CreateInvitationRequest(email = "newuser@example.com", role = "NONEXISTENT")
        val inviter = createMockUser()

        `when`(roleRepository.findByName("NONEXISTENT")).thenReturn(Optional.empty())

        val exception = assertThrows<BusinessRuleException> { invitationService.invite(request, inviter, inviter.organizationId) }
        assertThat(exception.message).isEqualTo("Role 'NONEXISTENT' does not exist")
    }

    @Test
    fun `invite should throw when role is system level`() {
        val request = CreateInvitationRequest(email = "newuser@example.com", role = "SUPER_ADMIN")
        val inviter = createMockUser()
        val systemRole = Role(name = "SUPER_ADMIN", description = "System admin", level = RoleLevel.SYSTEM)

        `when`(roleRepository.findByName("SUPER_ADMIN")).thenReturn(Optional.of(systemRole))

        val exception = assertThrows<BusinessRuleException> { invitationService.invite(request, inviter, inviter.organizationId) }
        assertThat(exception.message).isEqualTo("Cannot invite with system-level role")
    }

    @Test
    fun `invite should throw when active pending invitation already exists`() {
        val request = CreateInvitationRequest(email = "existing@example.com", role = "MEMBER")
        val inviter = createMockUser()
        val memberRole = Role(name = "MEMBER", description = "Member", level = RoleLevel.ORGANIZATION)
        val existingInvitation = createMockInvitation(email = "existing@example.com")

        `when`(roleRepository.findByName("MEMBER")).thenReturn(Optional.of(memberRole))
        `when`(
            invitationRepository.findByEmailAndOrganizationIdAndStatus(
                eq("existing@example.com"),
                eq(inviter.organizationId),
                eq(InvitationStatus.PENDING),
            ),
        ).thenReturn(Optional.of(existingInvitation))

        val exception = assertThrows<BusinessRuleException> { invitationService.invite(request, inviter, inviter.organizationId) }
        assertThat(exception.message).isEqualTo("An invitation has already been sent to this email")
    }

    @Test
    fun `invite should expire old invitation and create new one when previous expired`() {
        val request = CreateInvitationRequest(email = "reinvite@example.com", role = "MEMBER")
        val inviter = createMockUser()
        val memberRole = Role(name = "MEMBER", description = "Member", level = RoleLevel.ORGANIZATION)
        val expiredInvitation =
            createMockInvitation(
                email = "reinvite@example.com",
                expiryAt = LocalDateTime.now(ZoneOffset.UTC).minusHours(1),
            )

        `when`(roleRepository.findByName("MEMBER")).thenReturn(Optional.of(memberRole))
        `when`(
            invitationRepository.findByEmailAndOrganizationIdAndStatus(
                eq("reinvite@example.com"),
                eq(inviter.organizationId),
                eq(InvitationStatus.PENDING),
            ),
        ).thenReturn(Optional.of(expiredInvitation))
        `when`(invitationRepository.save(any<Invitation>())).thenAnswer { it.arguments[0] }

        val token = invitationService.invite(request, inviter, inviter.organizationId)

        assertThat(token).isNotNull()
        val captor = argumentCaptor<Invitation>()
        verify(invitationRepository, times(2)).save(captor.capture())

        val expired = captor.allValues.first { it.status == InvitationStatus.EXPIRED }
        assertThat(expired.id).isEqualTo(expiredInvitation.id)

        val newInvite = captor.allValues.first { it.status == InvitationStatus.PENDING }
        assertThat(newInvite.email).isEqualTo("reinvite@example.com")
    }

    @Test
    fun `validateInvitation should return invitation details with existingUser false`() {
        val invitation = createMockInvitation()

        `when`(invitationRepository.findByTokenHash("hashed-valid-token")).thenReturn(Optional.of(invitation))
        `when`(userRepository.findByEmail(invitation.email)).thenReturn(Optional.empty())

        val result = invitationService.validateInvitation("valid-token")

        assertThat(result.email).isEqualTo(invitation.email)
        assertThat(result.role).isEqualTo(invitation.role)
        assertThat(result.organizationId).isEqualTo(invitation.organizationId)
        assertThat(result.existingUser).isEqualTo(false)
    }

    @Test
    fun `validateInvitation should return existingUser true when email registered`() {
        val invitation = createMockInvitation()
        val existingUser = createMockUser().apply { email = invitation.email }

        `when`(invitationRepository.findByTokenHash("hashed-valid-token")).thenReturn(Optional.of(invitation))
        `when`(userRepository.findByEmail(invitation.email)).thenReturn(Optional.of(existingUser))

        val result = invitationService.validateInvitation("valid-token")

        assertThat(result.existingUser).isEqualTo(true)
    }

    @Test
    fun `validateInvitation should throw for invalid token`() {
        `when`(invitationRepository.findByTokenHash("hashed-bad-token")).thenReturn(Optional.empty())

        val exception = assertThrows<BusinessRuleException> { invitationService.validateInvitation("bad-token") }
        assertThat(exception.message).isEqualTo("Invalid or expired invitation token")
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
        val accepted = invitation.apply { status = InvitationStatus.ACCEPTED }

        `when`(jdbcTemplate.update(any<String>(), anyVararg<Any>())).thenReturn(1)
        `when`(invitationRepository.findByTokenHash(any())).thenReturn(Optional.of(accepted))
        `when`(userRepository.findByEmail(invitation.email)).thenReturn(Optional.empty())
        `when`(passwordEncoder.encode("SecurePass123!")).thenReturn("encodedPassword")
        `when`(userRepository.save(any<User>())).thenAnswer { it.arguments[0] }

        val result = invitationService.acceptInvitation(request)

        assertThat(result).isNotNull()
        assertThat(result.username).isEqualTo("newuser")
        assertThat(result.email).isEqualTo(invitation.email)
        assertThat(result.organizationId).isEqualTo(invitation.organizationId)

        val userCaptor = argumentCaptor<User>()
        verify(userRepository).save(userCaptor.capture())
        assertThat(userCaptor.firstValue.roleAssignments[0].role).isEqualTo("MEMBER")
        assertThat(userCaptor.firstValue.roleAssignments[0].organizationId).isEqualTo(invitation.organizationId)
    }

    @Test
    fun `acceptInvitation should add role to existing user with only token`() {
        val request = AcceptInvitationRequest(token = "raw-token")
        val invitation = createMockInvitation(role = "ADMIN")
        val accepted = invitation.apply { status = InvitationStatus.ACCEPTED }
        val existingUser =
            User(
                uuid = java.util.UUID.fromString("a5e1c214-a202-3e30-807d-048adc806677"),
                username = "existinguser",
                email = invitation.email,
                firstName = "Existing",
                lastName = "User",
                passwordHash = "hash",
                organizationId = java.util.UUID.fromString("fbede99a-0bef-3bf9-ba0b-8d28f050479d"),
                roleAssignments =
                    listOf(
                        RoleAssignment(
                            "fd77e00b-9d47-330b-92f4-a64820025f7d",
                            java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d"),
                        ),
                    ),
            )

        `when`(jdbcTemplate.update(any<String>(), anyVararg<Any>())).thenReturn(1)
        `when`(invitationRepository.findByTokenHash(any())).thenReturn(Optional.of(accepted))
        `when`(userRepository.findByEmail(invitation.email)).thenReturn(Optional.of(existingUser))
        `when`(userRepository.save(any<User>())).thenAnswer { it.arguments[0] }

        val result = invitationService.acceptInvitation(request)

        assertThat(result.roleAssignments.size).isEqualTo(2)
        assertThat(result.roleAssignments.any { it.role == "ADMIN" && it.organizationId == invitation.organizationId }).isTrue()

        verify(passwordEncoder, never()).encode(any())
    }

    @Test
    fun `acceptInvitation should throw for invalid or expired token`() {
        val request =
            AcceptInvitationRequest(
                token = "invalid",
                username = "user",
                password = "password123",
                firstName = "F",
                lastName = "L",
            )

        `when`(jdbcTemplate.update(any<String>(), anyVararg<Any>())).thenReturn(0)

        val exception = assertThrows<BusinessRuleException> { invitationService.acceptInvitation(request) }
        assertThat(exception.message).isEqualTo("Invalid or expired invitation token")
    }

    @Test
    fun `acceptInvitation should throw when new user missing required fields`() {
        val request = AcceptInvitationRequest(token = "raw-token")
        val invitation = createMockInvitation()

        `when`(jdbcTemplate.update(any<String>(), anyVararg<Any>())).thenReturn(1)
        `when`(invitationRepository.findByTokenHash(any())).thenReturn(Optional.of(invitation.apply { status = InvitationStatus.ACCEPTED }))
        `when`(userRepository.findByEmail(invitation.email)).thenReturn(Optional.empty())

        val exception = assertThrows<BusinessRuleException> { invitationService.acceptInvitation(request) }
        assertThat(exception.message).isEqualTo("Username, password, first name, and last name are required for new users")
    }

    @Test
    fun `acceptInvitation should throw when username already exists`() {
        val request =
            AcceptInvitationRequest(
                token = "raw-token",
                username = "taken",
                password = "SecurePass123!",
                firstName = "New",
                lastName = "User",
            )
        val invitation = createMockInvitation()

        `when`(jdbcTemplate.update(any<String>(), anyVararg<Any>())).thenReturn(1)
        `when`(invitationRepository.findByTokenHash(any())).thenReturn(Optional.of(invitation.apply { status = InvitationStatus.ACCEPTED }))
        `when`(userRepository.findByEmail(invitation.email)).thenReturn(Optional.empty())
        `when`(passwordEncoder.encode("SecurePass123!")).thenReturn("encodedPassword")
        `when`(userRepository.save(any<User>()))
            .thenThrow(DuplicateKeyException("E11000 duplicate key error collection: tessera.users index: username"))

        val exception = assertThrows<BusinessRuleException> { invitationService.acceptInvitation(request) }
        assertThat(exception.message).isEqualTo("Username already exists")
    }

    @Test
    fun `revokeInvitation should update status to REVOKED`() {
        val invitation = createMockInvitation()

        `when`(invitationRepository.findById(invitation.id)).thenReturn(Optional.of(invitation))
        `when`(invitationRepository.save(any<Invitation>())).thenAnswer { it.arguments[0] }

        invitationService.revokeInvitation(invitation.id, java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d"))

        val captor = argumentCaptor<Invitation>()
        verify(invitationRepository).save(captor.capture())
        assertThat(captor.firstValue.status).isEqualTo(InvitationStatus.REVOKED)
    }

    @Test
    fun `revokeInvitation should throw when invitation not in same org`() {
        val invitation = createMockInvitation(organizationId = java.util.UUID.fromString("fbede99a-0bef-3bf9-ba0b-8d28f050479d"))

        `when`(invitationRepository.findById(invitation.id)).thenReturn(Optional.of(invitation))

        val exception =
            assertThrows<ResourceNotFoundException> {
                invitationService.revokeInvitation(invitation.id, java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d"))
            }
        assertThat(exception.message).isEqualTo("Invitation not found")
    }

    @Test
    fun `revokeInvitation should throw when invitation is not pending`() {
        val invitation = createMockInvitation(status = InvitationStatus.ACCEPTED)

        `when`(invitationRepository.findById(invitation.id)).thenReturn(Optional.of(invitation))

        val exception =
            assertThrows<BusinessRuleException> {
                invitationService.revokeInvitation(invitation.id, java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d"))
            }
        assertThat(exception.message).isEqualTo("Invitation is not pending")
    }

    @Test
    fun `listInvitations should return pending invitations for org`() {
        val invitations = listOf(createMockInvitation(), createMockInvitation(email = "other@example.com"))

        `when`(
            invitationRepository.findByOrganizationIdAndStatusAndExpiryAtAfter(
                eq(java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d")),
                eq(InvitationStatus.PENDING),
                any(),
            ),
        ).thenReturn(invitations)

        val result = invitationService.listInvitations(java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d"))

        assertThat(result.size).isEqualTo(2)
    }

    private fun createMockUser() =
        User(
            uuid = java.util.UUID.fromString("3a01035d-c5db-3981-bf73-f18b3a0c1df9"),
            username = "testuser",
            email = "test@example.com",
            firstName = "Test",
            lastName = "User",
            passwordHash = "hash",
            organizationId = java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d"),
            roleAssignments =
                listOf(
                    RoleAssignment(
                        "fd77e00b-9d47-330b-92f4-a64820025f7d",
                        java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d"),
                    ),
                ),
        )

    private fun createMockInvitation(
        email: String = "invited@example.com",
        role: String = "MEMBER",
        organizationId: UUID = java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d"),
        status: InvitationStatus = InvitationStatus.PENDING,
        expiryAt: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC).plusHours(72),
    ) = Invitation(
        id = java.util.UUID.fromString("c065d6b7-6bc0-3834-937c-c5f22333dee6"),
        email = email,
        organizationId = organizationId,
        role = role,
        tokenHash = "hashed-token",
        invitedBy = java.util.UUID.fromString("3a01035d-c5db-3981-bf73-f18b3a0c1df9"),
        status = status,
        expiryAt = expiryAt,
    )
}
