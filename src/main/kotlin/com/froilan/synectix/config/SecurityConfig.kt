package com.froilan.synectix.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
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
class SecurityConfig(
    private val tokenAuthenticationFilter: TokenAuthenticationFilter,
    @Value("\${spring.web.cors.allowed-origins:http://localhost:3000,http://localhost:8080}")
    private val corsAllowedOrigins: String,
) {
    @Bean
    fun passwordEncoder(): PasswordEncoder =
        Argon2PasswordEncoder(
            16,
            32,
            1,
            65536,
            3,
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
                it.requestMatchers("/auth/**").permitAll()
                it.requestMatchers("/health/**").permitAll()
                it.requestMatchers("/actuator/health/**").permitAll()
                it.requestMatchers("/actuator/info").permitAll()
                it.anyRequest().authenticated()
            }.addFilterBefore(tokenAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
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
