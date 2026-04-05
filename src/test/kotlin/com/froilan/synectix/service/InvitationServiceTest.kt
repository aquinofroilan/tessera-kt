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
import org.mockito.kotlin.times
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.mongodb.core.FindAndModifyOptions
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.LocalDateTime
import java.util.Optional
import org.assertj.core.api.Assertions.assertThat

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
        `when`(tokenHasher.generate(any())).thenReturn("generated-test-token")

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
        `when`(invitationRepository.findByEmailAndOrganizationIdAndStatus(any(), any(), any()))
            .thenReturn(Optional.empty())
        `when`(invitationRepository.save(any<Invitation>())).thenAnswer { it.arguments[0] }

        val token = invitationService.invite(request, inviter, inviter.organizationId)

        assertThat(token).isNotNull()
        assertThat(token.isNotEmpty()).isTrue()

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

        val exception = assertThrows<IllegalArgumentException> { invitationService.invite(request, inviter, inviter.organizationId) }
        assertThat(exception.message).isEqualTo("Role 'NONEXISTENT' does not exist")
    }

    @Test
    fun `invite should throw when role is system level`() {
        val request = CreateInvitationRequest(email = "newuser@example.com", role = "SUPER_ADMIN")
        val inviter = createMockUser()
        val systemRole = Role(name = "SUPER_ADMIN", description = "System admin", level = RoleLevel.SYSTEM)

        `when`(roleRepository.findByName("SUPER_ADMIN")).thenReturn(Optional.of(systemRole))

        val exception = assertThrows<IllegalArgumentException> { invitationService.invite(request, inviter, inviter.organizationId) }
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

        val exception = assertThrows<IllegalArgumentException> { invitationService.invite(request, inviter, inviter.organizationId) }
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
                expiryAt = LocalDateTime.now().minusHours(1),
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
        val existingUser = createMockUser().copy(email = invitation.email)

        `when`(invitationRepository.findByTokenHash("hashed-valid-token")).thenReturn(Optional.of(invitation))
        `when`(userRepository.findByEmail(invitation.email)).thenReturn(Optional.of(existingUser))

        val result = invitationService.validateInvitation("valid-token")

        assertThat(result.existingUser).isEqualTo(true)
    }

    @Test
    fun `validateInvitation should throw for invalid token`() {
        `when`(invitationRepository.findByTokenHash("hashed-bad-token")).thenReturn(Optional.empty())

        val exception = assertThrows<IllegalArgumentException> { invitationService.validateInvitation("bad-token") }
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
        val accepted = invitation.copy(status = InvitationStatus.ACCEPTED)

        `when`(mongoTemplate.findAndModify(any(), any(), any<FindAndModifyOptions>(), eq(Invitation::class.java)))
            .thenReturn(accepted)
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
        val accepted = invitation.copy(status = InvitationStatus.ACCEPTED)
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

        `when`(mongoTemplate.findAndModify(any(), any(), any<FindAndModifyOptions>(), eq(Invitation::class.java)))
            .thenReturn(accepted)
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

        `when`(mongoTemplate.findAndModify(any(), any(), any<FindAndModifyOptions>(), eq(Invitation::class.java)))
            .thenReturn(null)

        val exception = assertThrows<IllegalArgumentException> { invitationService.acceptInvitation(request) }
        assertThat(exception.message).isEqualTo("Invalid or expired invitation token")
    }

    @Test
    fun `acceptInvitation should throw when new user missing required fields`() {
        val request = AcceptInvitationRequest(token = "raw-token")
        val invitation = createMockInvitation()

        `when`(mongoTemplate.findAndModify(any(), any(), any<FindAndModifyOptions>(), eq(Invitation::class.java)))
            .thenReturn(invitation.copy(status = InvitationStatus.ACCEPTED))
        `when`(userRepository.findByEmail(invitation.email)).thenReturn(Optional.empty())

        val exception = assertThrows<IllegalArgumentException> { invitationService.acceptInvitation(request) }
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

        `when`(mongoTemplate.findAndModify(any(), any(), any<FindAndModifyOptions>(), eq(Invitation::class.java)))
            .thenReturn(invitation.copy(status = InvitationStatus.ACCEPTED))
        `when`(userRepository.findByEmail(invitation.email)).thenReturn(Optional.empty())
        `when`(passwordEncoder.encode("SecurePass123!")).thenReturn("encodedPassword")
        `when`(userRepository.save(any<User>()))
            .thenThrow(DuplicateKeyException("E11000 duplicate key error collection: synectix.users index: username"))

        val exception = assertThrows<IllegalArgumentException> { invitationService.acceptInvitation(request) }
        assertThat(exception.message).isEqualTo("Username already exists")
    }

    @Test
    fun `revokeInvitation should update status to REVOKED`() {
        val invitation = createMockInvitation()

        `when`(invitationRepository.findById(invitation.id)).thenReturn(Optional.of(invitation))
        `when`(invitationRepository.save(any<Invitation>())).thenAnswer { it.arguments[0] }

        invitationService.revokeInvitation(invitation.id, "org-123")

        val captor = argumentCaptor<Invitation>()
        verify(invitationRepository).save(captor.capture())
        assertThat(captor.firstValue.status).isEqualTo(InvitationStatus.REVOKED)
    }

    @Test
    fun `revokeInvitation should throw when invitation not in same org`() {
        val invitation = createMockInvitation(organizationId = "other-org")

        `when`(invitationRepository.findById(invitation.id)).thenReturn(Optional.of(invitation))

        val exception =
            assertThrows<IllegalArgumentException> {
                invitationService.revokeInvitation(invitation.id, "org-123")
            }
        assertThat(exception.message).isEqualTo("Invitation not found")
    }

    @Test
    fun `revokeInvitation should throw when invitation is not pending`() {
        val invitation = createMockInvitation(status = InvitationStatus.ACCEPTED)

        `when`(invitationRepository.findById(invitation.id)).thenReturn(Optional.of(invitation))

        val exception =
            assertThrows<IllegalArgumentException> {
                invitationService.revokeInvitation(invitation.id, "org-123")
            }
        assertThat(exception.message).isEqualTo("Invitation is not pending")
    }

    @Test
    fun `listInvitations should return pending invitations for org`() {
        val invitations = listOf(createMockInvitation(), createMockInvitation(email = "other@example.com"))

        `when`(invitationRepository.findByOrganizationIdAndStatusAndExpiryAtAfter(eq("org-123"), eq(InvitationStatus.PENDING), any()))
            .thenReturn(invitations)

        val result = invitationService.listInvitations("org-123")

        assertThat(result.size).isEqualTo(2)
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
