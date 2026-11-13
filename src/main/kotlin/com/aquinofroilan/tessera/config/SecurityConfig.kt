package com.aquinofroilan.tessera.config

import com.aquinofroilan.tessera.security.TesseraPermissionEvaluator
import com.aquinofroilan.tessera.exception.ApiProblemDetails
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.core.env.Profiles
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
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
import tools.jackson.databind.ObjectMapper

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig(
    private val tokenAuthenticationFilter: TokenAuthenticationFilter,
    private val environment: Environment,
    private val objectMapper: ObjectMapper,
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
                it.requestMatchers("/api/v1/auth/change-password", "/auth/change-password").authenticated()
                it
                    .requestMatchers(
                        "/api/v1/auth/sessions",
                        "/api/v1/auth/sessions/**",
                        "/auth/sessions",
                        "/auth/sessions/**",
                    ).authenticated()
                it.requestMatchers(HttpMethod.POST, "/api/v1/auth/invitations", "/auth/invitations").authenticated()
                it.requestMatchers(HttpMethod.GET, "/api/v1/auth/invitations", "/auth/invitations").authenticated()
                it.requestMatchers(HttpMethod.DELETE, "/api/v1/auth/invitations/*", "/auth/invitations/*").authenticated()
                it
                    .requestMatchers(
                        "/api/v1/auth/organizations",
                        "/api/v1/auth/organizations/**",
                        "/auth/organizations",
                        "/auth/organizations/**",
                    ).authenticated()
                it
                    .requestMatchers(
                        "/api/v1/auth/api-keys",
                        "/api/v1/auth/api-keys/**",
                        "/auth/api-keys",
                        "/auth/api-keys/**",
                    ).authenticated()
                it.requestMatchers("/api/v1/finance/**", "/finance/**").authenticated()
                it.requestMatchers(HttpMethod.POST, "/api/v1/auth/signin").permitAll()
                it.requestMatchers("/api/v1/auth/**", "/auth/**").permitAll()
                it.requestMatchers(HttpMethod.POST, "/api/v1/oauth2/token").permitAll()
                it.requestMatchers(HttpMethod.POST, "/api/v1/integrations/payments/webhooks/**").permitAll()
                it.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html", "/docs").permitAll()
                it.requestMatchers("/health/**").permitAll()
                it.requestMatchers("/graphql").permitAll()
                if (environment.acceptsProfiles(Profiles.of("dev", "test"))) {
                    it.requestMatchers("/graphiql", "/graphiql/**").permitAll()
                } else {
                    it.requestMatchers("/graphiql", "/graphiql/**").denyAll()
                }
                it.requestMatchers("/actuator/health/**").permitAll()
                it.requestMatchers("/actuator/info").permitAll()
                it.requestMatchers("/actuator/prometheus").permitAll()
                it.anyRequest().authenticated()
            }.exceptionHandling {
                it.authenticationEntryPoint { req, resp, _ ->
                    resp.status = HttpStatus.UNAUTHORIZED.value()
                    resp.contentType = MediaType.APPLICATION_PROBLEM_JSON_VALUE
                    objectMapper.writeValue(
                        resp.outputStream,
                        ApiProblemDetails.create(
                            HttpStatus.UNAUTHORIZED,
                            "AUTHENTICATION_REQUIRED",
                            "Authentication required",
                            req.requestURI,
                        ),
                    )
                }
                it.accessDeniedHandler { req, resp, _ ->
                    resp.status = HttpStatus.FORBIDDEN.value()
                    resp.contentType = MediaType.APPLICATION_PROBLEM_JSON_VALUE
                    objectMapper.writeValue(
                        resp.outputStream,
                        ApiProblemDetails.create(
                            HttpStatus.FORBIDDEN,
                            "ACCESS_DENIED",
                            "Insufficient permissions",
                            req.requestURI,
                        ),
                    )
                }
            }.addFilterBefore(tokenAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    @Bean
    fun methodSecurityExpressionHandler(permissionEvaluator: TesseraPermissionEvaluator): MethodSecurityExpressionHandler {
        val handler = DefaultMethodSecurityExpressionHandler()
        handler.setPermissionEvaluator(permissionEvaluator)
        return handler
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOrigins = corsAllowedOrigins.split(",").map { it.trim() }
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        configuration.allowedHeaders = listOf("*")
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}
