package com.froilan.synectix.config

import com.froilan.synectix.repository.SessionTokenRepository
import com.froilan.synectix.repository.UserRepository
import com.froilan.synectix.security.RolePermissionCache
import com.froilan.synectix.security.SessionContext
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.time.LocalDateTime

@Component
class TokenAuthenticationFilter(
    private val sessionTokenRepository: SessionTokenRepository,
    private val userRepository: UserRepository,
    private val rolePermissionCache: RolePermissionCache,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val authHeader = request.getHeader("Authorization")

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            val token = authHeader.substring(7)
            val sessionTokenOpt = sessionTokenRepository.findByToken(token)

            if (sessionTokenOpt.isPresent) {
                val sessionToken = sessionTokenOpt.get()
                if (sessionToken.expiryAt.isAfter(LocalDateTime.now())) {
                    val userOpt = userRepository.findById(sessionToken.userId)
                    if (userOpt.isPresent && userOpt.get().isActive) {
                        val user = userOpt.get()
                        val activeOrgId = sessionToken.organizationId ?: user.organizationId
                        val activeRoleAssignments =
                            user.roleAssignments.filter {
                                it.organizationId == activeOrgId || it.organizationId == null
                            }
                        val roleAuthorities =
                            activeRoleAssignments
                                .map { it.role }
                                .distinct()
                                .map { SimpleGrantedAuthority("ROLE_$it") }
                        val permissionAuthorities =
                            activeRoleAssignments
                                .flatMap { rolePermissionCache.getPermissions(it.role) }
                                .distinct()
                                .map { SimpleGrantedAuthority(it) }
                        val authorities = roleAuthorities + permissionAuthorities
                        val authentication = UsernamePasswordAuthenticationToken(user, null, authorities)
                        authentication.details =
                            SessionContext(
                                sessionId = sessionToken.id,
                                organizationId = activeOrgId,
                            )
                        SecurityContextHolder.getContext().authentication = authentication
                    }
                } else {
                    sessionTokenRepository.delete(sessionToken)
                }
            }
        }

        filterChain.doFilter(request, response)
    }
}
