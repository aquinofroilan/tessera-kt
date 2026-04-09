package com.froilan.synectix.service

import com.froilan.synectix.dto.AcceptInvitationRequest
import com.froilan.synectix.dto.CreateInvitationRequest
import com.froilan.synectix.dto.ValidateInvitationResponse
import com.froilan.synectix.model.Invitation
import com.froilan.synectix.model.InvitationStatus
import com.froilan.synectix.model.RoleAssignment
import com.froilan.synectix.model.RoleLevel
import com.froilan.synectix.model.User
import com.froilan.synectix.repository.InvitationRepository
import com.froilan.synectix.repository.RoleRepository
import com.froilan.synectix.repository.UserRepository
import com.froilan.synectix.util.TokenHasher
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.mongodb.core.FindAndModifyOptions
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
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
    private val mongoTemplate: MongoTemplate,
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
                IllegalArgumentException("Role '${request.role}' does not exist")
            }
        if (role.level != RoleLevel.ORGANIZATION) {
            throw IllegalArgumentException("Cannot invite with system-level role")
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
                throw IllegalArgumentException("An invitation has already been sent to this email")
            }
            invitationRepository.save(pendingInvitation.copy(status = InvitationStatus.EXPIRED))
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
            throw IllegalArgumentException("An invitation has already been sent to this email", e)
        }

        return rawToken
    }

    fun validateInvitation(token: String): ValidateInvitationResponse {
        val tokenHash = tokenHasher.hash(token)
        val invitation =
            invitationRepository.findByTokenHash(tokenHash).orElseThrow {
                IllegalArgumentException("Invalid or expired invitation token")
            }
        if (invitation.status != InvitationStatus.PENDING || !invitation.expiryAt.isAfter(LocalDateTime.now(ZoneOffset.UTC))) {
            throw IllegalArgumentException("Invalid or expired invitation token")
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

        val accepted =
            mongoTemplate.findAndModify(
                Query.query(
                    Criteria
                        .where("tokenHash")
                        .`is`(tokenHash)
                        .and("status")
                        .`is`(InvitationStatus.PENDING)
                        .and("expiryAt")
                        .gt(LocalDateTime.now(ZoneOffset.UTC)),
                ),
                Update.update("status", InvitationStatus.ACCEPTED),
                FindAndModifyOptions.options().returnNew(true),
                Invitation::class.java,
            ) ?: throw IllegalArgumentException("Invalid or expired invitation token")

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
            val updatedUser =
                user.copy(
                    roleAssignments =
                        user.roleAssignments +
                            RoleAssignment(
                                role = accepted.role,
                                organizationId = accepted.organizationId,
                            ),
                )
            return userRepository.save(updatedUser)
        }

        if (request.username.isNullOrBlank() ||
            request.password.isNullOrBlank() ||
            request.firstName.isNullOrBlank() ||
            request.lastName.isNullOrBlank()
        ) {
            throw IllegalArgumentException("Username, password, first name, and last name are required for new users")
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
                    throw IllegalArgumentException("Username already exists", e)
                errorMessage.contains("email", ignoreCase = true) ->
                    throw IllegalArgumentException("Email already exists", e)
                else ->
                    throw IllegalArgumentException("Account could not be created", e)
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
                IllegalArgumentException("Invitation not found")
            }
        if (invitation.organizationId != activeOrgId) {
            throw IllegalArgumentException("Invitation not found")
        }
        if (invitation.status != InvitationStatus.PENDING) {
            throw IllegalArgumentException("Invitation is not pending")
        }
        invitationRepository.save(invitation.copy(status = InvitationStatus.REVOKED))
    }

    fun listInvitations(organizationId: String): List<Invitation> =
        invitationRepository.findByOrganizationIdAndStatusAndExpiryAtAfter(
            organizationId,
            InvitationStatus.PENDING,
            LocalDateTime.now(ZoneOffset.UTC),
        )
}
