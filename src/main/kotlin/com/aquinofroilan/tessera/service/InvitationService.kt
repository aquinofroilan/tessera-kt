package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.AcceptInvitationRequest
import com.aquinofroilan.tessera.dto.CreateInvitationRequest
import com.aquinofroilan.tessera.dto.ValidateInvitationResponse
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import com.aquinofroilan.tessera.model.Invitation
import com.aquinofroilan.tessera.model.InvitationStatus
import com.aquinofroilan.tessera.model.RoleAssignment
import com.aquinofroilan.tessera.model.RoleLevel
import com.aquinofroilan.tessera.model.User
import com.aquinofroilan.tessera.repository.InvitationRepository
import com.aquinofroilan.tessera.repository.RoleRepository
import com.aquinofroilan.tessera.repository.UserRepository
import com.aquinofroilan.tessera.util.TokenHasher
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.DuplicateKeyException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Locale

@Service
class InvitationService(
    private val invitationRepository: InvitationRepository,
    private val userRepository: UserRepository,
    private val roleRepository: RoleRepository,
    private val jdbcTemplate: JdbcTemplate,
    private val tokenHasher: TokenHasher,
    private val passwordEncoder: PasswordEncoder,
    @Value("\${security.invitation.expiration-hours:72}")
    private val invitationExpiryHours: Long,
) {
    @Transactional
    fun invite(
        request: CreateInvitationRequest,
        inviter: User,
        activeOrgId: String,
    ): String {
        val normalizedEmail = request.email.lowercase(Locale.ROOT)

        val role =
            roleRepository.findByName(request.role).orElseThrow {
                BusinessRuleException("Role '${request.role}' does not exist")
            }
        if (role.level != RoleLevel.ORGANIZATION) {
            throw BusinessRuleException("Cannot invite with system-level role")
        }

        val existing =
            invitationRepository.findByEmailAndOrganizationIdAndStatus(
                normalizedEmail,
                activeOrgId,
                InvitationStatus.PENDING,
            )
        if (existing.isPresent) {
            val pendingInvitation = existing.get()
            if (pendingInvitation.expiryAt.isAfter(LocalDateTime.now(ZoneOffset.UTC))) {
                throw BusinessRuleException("An invitation has already been sent to this email")
            }
            pendingInvitation.status = InvitationStatus.EXPIRED
            invitationRepository.save(pendingInvitation)
        }

        val rawToken = tokenHasher.generate(32)
        val invitation =
            Invitation(
                email = normalizedEmail,
                organizationId = activeOrgId,
                role = role.name,
                tokenHash = tokenHasher.hash(rawToken),
                invitedBy = inviter.uuid,
                expiryAt = LocalDateTime.now(ZoneOffset.UTC).plusHours(invitationExpiryHours),
            )
        try {
            invitationRepository.save(invitation)
        } catch (e: DuplicateKeyException) {
            throw BusinessRuleException("An invitation has already been sent to this email", e)
        }

        return rawToken
    }

    fun validateInvitation(token: String): ValidateInvitationResponse {
        val tokenHash = tokenHasher.hash(token)
        val invitation =
            invitationRepository.findByTokenHash(tokenHash).orElseThrow {
                BusinessRuleException("Invalid or expired invitation token")
            }
        if (invitation.status != InvitationStatus.PENDING || !invitation.expiryAt.isAfter(LocalDateTime.now(ZoneOffset.UTC))) {
            throw BusinessRuleException("Invalid or expired invitation token")
        }
        val existingUser = userRepository.findByEmail(invitation.email).isPresent
        return ValidateInvitationResponse(
            email = invitation.email,
            role = invitation.role,
            organizationId = invitation.organizationId,
            existingUser = existingUser,
        )
    }

    @Transactional
    fun acceptInvitation(request: AcceptInvitationRequest): User {
        val tokenHash = tokenHasher.hash(request.token)

        val now = LocalDateTime.now(ZoneOffset.UTC)
        val updated =
            jdbcTemplate.update(
                """
                UPDATE invitations
                   SET status = 'ACCEPTED'
                 WHERE token_hash = ?
                   AND status = 'PENDING'
                   AND expiry_at > ?
                """.trimIndent(),
                tokenHash,
                now,
            )
        if (updated == 0) {
            throw BusinessRuleException("Invalid or expired invitation token")
        }
        val accepted =
            invitationRepository
                .findByTokenHash(tokenHash)
                .orElseThrow { BusinessRuleException("Invalid or expired invitation token") }

        val existingUser = userRepository.findByEmail(accepted.email)
        if (existingUser.isPresent) {
            val user = existingUser.get()
            val alreadyHasRole =
                user.roleAssignments.any {
                    it.role == accepted.role && it.organizationId == accepted.organizationId
                }
            if (alreadyHasRole) {
                return user
            }
            user.roleAssignments =
                user.roleAssignments +
                RoleAssignment(
                    role = accepted.role,
                    organizationId = accepted.organizationId,
                )
            return userRepository.save(user)
        }

        if (request.username.isNullOrBlank() ||
            request.password.isNullOrBlank() ||
            request.firstName.isNullOrBlank() ||
            request.lastName.isNullOrBlank()
        ) {
            throw BusinessRuleException("Username, password, first name, and last name are required for new users")
        }

        val newUser =
            User(
                username = request.username,
                email = accepted.email,
                firstName = request.firstName,
                lastName = request.lastName,
                passwordHash = passwordEncoder.encode(request.password) as String,
                organizationId = accepted.organizationId,
                roleAssignments =
                    listOf(
                        RoleAssignment(
                            role = accepted.role,
                            organizationId = accepted.organizationId,
                        ),
                    ),
            )
        return try {
            userRepository.save(newUser)
        } catch (e: DuplicateKeyException) {
            val errorMessage = e.message ?: ""
            when {
                errorMessage.contains("username", ignoreCase = true) ->
                    throw BusinessRuleException("Username already exists", e)
                errorMessage.contains("email", ignoreCase = true) ->
                    throw BusinessRuleException("Email already exists", e)
                else ->
                    throw BusinessRuleException("Account could not be created", e)
            }
        }
    }

    @Transactional
    fun revokeInvitation(
        invitationId: String,
        activeOrgId: String,
    ) {
        val invitation =
            invitationRepository.findById(invitationId).orElseThrow {
                ResourceNotFoundException("Invitation not found")
            }
        if (invitation.organizationId != activeOrgId) {
            throw ResourceNotFoundException("Invitation not found")
        }
        if (invitation.status != InvitationStatus.PENDING) {
            throw BusinessRuleException("Invitation is not pending")
        }
        invitation.status = InvitationStatus.REVOKED
        invitationRepository.save(invitation)
    }

    fun listInvitations(organizationId: String): List<Invitation> =
        invitationRepository.findByOrganizationIdAndStatusAndExpiryAtAfter(
            organizationId,
            InvitationStatus.PENDING,
            LocalDateTime.now(ZoneOffset.UTC),
        )
}
