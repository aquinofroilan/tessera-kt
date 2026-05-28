package com.loom.synectix.controller

import com.loom.synectix.annotation.LogLevel
import com.loom.synectix.annotation.Loggable
import com.loom.synectix.dto.AcceptInvitationRequest
import com.loom.synectix.dto.CreateInvitationRequest
import com.loom.synectix.dto.InvitationResponse
import com.loom.synectix.dto.ValidateInvitationRequest
import com.loom.synectix.model.User
import com.loom.synectix.security.AuthenticationContext
import com.loom.synectix.security.SessionContext
import com.loom.synectix.service.InvitationService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/auth/invitations")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class InvitationController(
    private val invitationService: InvitationService,
    private val authContext: AuthenticationContext,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('invitation:write')")
    fun createInvitation(
        @Valid @RequestBody request: CreateInvitationRequest,
    ): ResponseEntity<Any> {
        val (user, sessionContext) = extractUserAndContext() ?: return authContext.unauthorized()

        val token = invitationService.invite(request, user, sessionContext.organizationId)
        return ResponseEntity.status(HttpStatus.CREATED).body(
            mapOf("message" to "Invitation created", "token" to token),
        )
    }

    @GetMapping
    @PreAuthorize("hasAuthority('invitation:read')")
    fun listInvitations(): ResponseEntity<Any> {
        val (_, sessionContext) = extractUserAndContext() ?: return authContext.unauthorized()

        val invitations =
            invitationService.listInvitations(sessionContext.organizationId).map { invitation ->
                InvitationResponse(
                    id = invitation.id,
                    email = invitation.email,
                    role = invitation.role,
                    status = invitation.status.name,
                    invitedBy = invitation.invitedBy,
                    expiresAt = invitation.expiryAt.toString(),
                    createdAt = invitation.createdAt?.toString(),
                )
            }
        return ResponseEntity.ok(invitations)
    }

    @PostMapping("/validate")
    fun validateInvitation(
        @Valid @RequestBody request: ValidateInvitationRequest,
    ): ResponseEntity<Any> {
        val result = invitationService.validateInvitation(request.token)
        return ResponseEntity.ok(result)
    }

    @PostMapping("/accept")
    fun acceptInvitation(
        @Valid @RequestBody request: AcceptInvitationRequest,
    ): ResponseEntity<Any> {
        invitationService.acceptInvitation(request)
        return ResponseEntity.ok(mapOf("message" to "Invitation accepted successfully"))
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('invitation:write')")
    fun revokeInvitation(
        @PathVariable id: String,
    ): ResponseEntity<Any> {
        val (_, sessionContext) = extractUserAndContext() ?: return authContext.unauthorized()

        invitationService.revokeInvitation(id, sessionContext.organizationId)
        return ResponseEntity.ok(mapOf("message" to "Invitation revoked"))
    }

    private fun extractUserAndContext(): Pair<User, SessionContext>? {
        val authentication = SecurityContextHolder.getContext().authentication
        val user = authentication?.principal as? User ?: return null
        val sessionContext = authentication.details as? SessionContext ?: return null
        return user to sessionContext
    }
}
