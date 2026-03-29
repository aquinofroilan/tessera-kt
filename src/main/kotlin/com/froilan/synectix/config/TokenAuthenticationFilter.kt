package com.froilan.synectix.config

import com.froilan.synectix.repository.SessionTokenRepository
import com.froilan.synectix.repository.UserRepository
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
                        val authorities = user.roles.map { SimpleGrantedAuthority("ROLE_$it") }
                        val authentication = UsernamePasswordAuthenticationToken(user, null, authorities)
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
