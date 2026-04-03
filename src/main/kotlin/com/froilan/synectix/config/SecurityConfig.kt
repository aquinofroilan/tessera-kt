package com.froilan.synectix.config

import com.froilan.synectix.security.SynectixPermissionEvaluator
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig(
    private val tokenAuthenticationFilter: TokenAuthenticationFilter,
    @Value("\${spring.web.cors.allowed-origins:http://localhost:3000,http://localhost:8080}")
    private val corsAllowedOrigins: String,
) {
    @Bean
    fun passwordEncoder(): PasswordEncoder =
        Argon2PasswordEncoder(
            16, // saltLength
            32, // hashLength
            1, // parallelism
            65536, // memory (KB)
            3, // iterations
        )

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                it.requestMatchers("/auth/change-password").authenticated()
                it.requestMatchers("/auth/sessions", "/auth/sessions/**").authenticated()
                it.requestMatchers(HttpMethod.POST, "/auth/invitations").authenticated()
                it.requestMatchers(HttpMethod.GET, "/auth/invitations").authenticated()
                it.requestMatchers(HttpMethod.DELETE, "/auth/invitations/*").authenticated()
                it.requestMatchers("/auth/**").permitAll()
                it.requestMatchers("/health/**").permitAll()
                it.requestMatchers("/actuator/health/**").permitAll()
                it.requestMatchers("/actuator/info").permitAll()
                it.anyRequest().authenticated()
            }.exceptionHandling {
                it.accessDeniedHandler { _, resp, _ ->
                    resp.status = 403
                    resp.contentType = "application/json"
                    resp.writer.write("""{"error":"Insufficient permissions"}""")
                }
            }.addFilterBefore(tokenAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    @Bean
    fun methodSecurityExpressionHandler(permissionEvaluator: SynectixPermissionEvaluator): MethodSecurityExpressionHandler {
        val handler = DefaultMethodSecurityExpressionHandler()
        handler.setPermissionEvaluator(permissionEvaluator)
        return handler
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOrigins = corsAllowedOrigins.split(",").map { it.trim() }
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
        configuration.allowedHeaders = listOf("*")
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}
