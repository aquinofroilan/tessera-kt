package com.froilan.synectix.config

import com.froilan.synectix.repository.SessionTokenRepository
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
    private val sessionTokenRepository: SessionTokenRepository
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authHeader = request.getHeader("Authorization")

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            val token = authHeader.substring(7)
            val sessionTokenOpt = sessionTokenRepository.findByToken(token)

            if (sessionTokenOpt.isPresent) {
                val sessionToken = sessionTokenOpt.get()
                if (sessionToken.expiryAt.isAfter(LocalDateTime.now())) {
                    val user = sessionToken.user
                    val authorities = user.roles.map { SimpleGrantedAuthority("ROLE_$it") }
                    val authentication = UsernamePasswordAuthenticationToken(user, null, authorities)
                    SecurityContextHolder.getContext().authentication = authentication
                } else {
                    // Token expired, maybe delete it?
                    sessionTokenRepository.delete(sessionToken)
                }
            }
        }

        filterChain.doFilter(request, response)
    }
}
