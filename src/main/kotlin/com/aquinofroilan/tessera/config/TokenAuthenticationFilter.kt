package com.aquinofroilan.tessera.config

import com.aquinofroilan.tessera.domain.auth.repository.SessionTokenRepository
import com.aquinofroilan.tessera.domain.auth.repository.UserRepository
import com.aquinofroilan.tessera.domain.auth.service.ApiKeyService
import com.aquinofroilan.tessera.security.ApiKeyContext
import com.aquinofroilan.tessera.security.ApiKeyPrincipal
import com.aquinofroilan.tessera.security.RolePermissionCache
import com.aquinofroilan.tessera.security.SessionContext
import com.github.benmanes.caffeine.cache.Caffeine
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit

@Component
class TokenAuthenticationFilter(
    private val sessionTokenRepository: SessionTokenRepository,
    private val userRepository: UserRepository,
    private val rolePermissionCache: RolePermissionCache,
    private val apiKeyService: ApiKeyService,
) : OncePerRequestFilter() {
    private val apiKeyAttemptCounts =
        Caffeine
            .newBuilder()
            .expireAfterWrite(15, TimeUnit.MINUTES)
            .maximumSize(10_000)
            .build<String, Int>()

    private val apiKeyBlockedIps =
        Caffeine
            .newBuilder()
            .expireAfterWrite(15, TimeUnit.MINUTES)
            .maximumSize(10_000)
            .build<String, Boolean>()

    companion object {
        private const val MAX_API_KEY_ATTEMPTS = 10
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val authHeader = request.getHeader("Authorization")

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            val token = authHeader.substring(7)
            val sessionTokenOpt = sessionTokenRepository.findByToken(token)

            val path = request.requestURI.removePrefix(request.contextPath)

            if (sessionTokenOpt.isPresent) {
                val sessionToken = sessionTokenOpt.get()
                if (sessionToken.expiryAt.isAfter(LocalDateTime.now(ZoneOffset.UTC))) {
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
            } else if (!path.startsWith("/auth")) {
                val clientIp = request.remoteAddr ?: "unknown"
                if (apiKeyBlockedIps.getIfPresent(clientIp) != null) {
                    filterChain.doFilter(request, response)
                    return
                }
                val apiKey = apiKeyService.authenticateByApiKey(token)
                if (apiKey != null) {
                    val authorities = apiKey.permissions.map { SimpleGrantedAuthority(it) }
                    val principal = ApiKeyPrincipal(apiKey.id, apiKey.name, apiKey.organizationId)
                    val authentication = UsernamePasswordAuthenticationToken(principal, null, authorities)
                    authentication.details = ApiKeyContext(apiKey.id, apiKey.organizationId)
                    SecurityContextHolder.getContext().authentication = authentication
                } else {
                    val count = apiKeyAttemptCounts.asMap().merge(clientIp, 1, Int::plus) ?: 1
                    if (count >= MAX_API_KEY_ATTEMPTS) {
                        apiKeyBlockedIps.put(clientIp, true)
                        apiKeyAttemptCounts.invalidate(clientIp)
                    }
                }
            }
        }

        filterChain.doFilter(request, response)
    }
}
