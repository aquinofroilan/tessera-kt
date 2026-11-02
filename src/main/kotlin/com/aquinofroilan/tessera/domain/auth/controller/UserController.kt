package com.aquinofroilan.tessera.domain.auth.controller

import com.aquinofroilan.tessera.domain.auth.model.User
import com.aquinofroilan.tessera.domain.auth.repository.UserRepository
import com.aquinofroilan.tessera.security.ApiKeyPrincipal
import com.aquinofroilan.tessera.security.AuthenticationContext
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

data class CurrentUserResponse(
    val id: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val role: String,
)

@RestController
@RequestMapping("/api/v1/users")
class UserController(
    private val authenticationContext: AuthenticationContext,
    private val userRepository: UserRepository,
) {
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    fun me(): ResponseEntity<Any> {
        val principal = SecurityContextHolder.getContext().authentication?.principal
        val user =
            when (principal) {
                is User -> principal
                is ApiKeyPrincipal ->
                    userRepository.findById(principal.createdBy).orElse(null)
                else -> null
            } ?: return authenticationContext.unauthorized()

        return ResponseEntity.ok(
            CurrentUserResponse(
                id = user.uuid.toString(),
                email = user.email,
                firstName = user.firstName,
                lastName = user.lastName,
                role =
                    user.roleAssignments
                        .filter { it.organizationId == authenticationContext.organizationId() || it.organizationId == null }
                        .map { it.role }
                        .firstOrNull()
                        ?: authenticationContext.roles().firstOrNull().orEmpty(),
            ),
        )
    }
}
