package com.froilan.synectix.controller

import com.froilan.synectix.annotation.LogLevel
import com.froilan.synectix.annotation.Loggable
import com.froilan.synectix.dto.AcceptInvitationRequest
import com.froilan.synectix.dto.CreateInvitationRequest
import com.froilan.synectix.dto.InvitationResponse
import com.froilan.synectix.model.User
import com.froilan.synectix.service.InvitationService
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
) {
    @PostMapping
    @PreAuthorize("hasAuthority('invitation:write')")
    fun createInvitation(
        @Valid @RequestBody request: CreateInvitationRequest,
    ): ResponseEntity<Any> {
        val user = extractUser() ?: return unauthorized()

        return try {
            val token = invitationService.invite(request, user)
            ResponseEntity.status(HttpStatus.CREATED).body(
                mapOf("message" to "Invitation created", "token" to token),
            )
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "Invalid invitation request")))
        }
    }

    @GetMapping
    @PreAuthorize("hasAuthority('invitation:read')")
    fun listInvitations(): ResponseEntity<Any> {
        val user = extractUser() ?: return unauthorized()

        val invitations =
            invitationService.listInvitations(user.organizationId).map { invitation ->
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

    @PostMapping("/accept")
    fun acceptInvitation(
        @Valid @RequestBody request: AcceptInvitationRequest,
    ): ResponseEntity<Any> =
        try {
            invitationService.acceptInvitation(request)
            ResponseEntity.ok(mapOf("message" to "Invitation accepted successfully"))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "Invalid invitation token")))
        }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('invitation:write')")
    fun revokeInvitation(
        @PathVariable id: String,
    ): ResponseEntity<Any> {
        val user = extractUser() ?: return unauthorized()

        return try {
            invitationService.revokeInvitation(id, user)
            ResponseEntity.ok(mapOf("message" to "Invitation revoked"))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "Invalid invitation operation")))
        }
    }

    private fun extractUser(): User? {
        val authentication = SecurityContextHolder.getContext().authentication
        return authentication?.principal as? User
    }

    private fun unauthorized(): ResponseEntity<Any> =
        ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(mapOf("error" to "Authentication required"))
}
