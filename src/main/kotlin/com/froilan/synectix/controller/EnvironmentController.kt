package com.froilan.synectix.controller

import com.froilan.synectix.config.SynectixProperties
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Environment controller to demonstrate environment variable usage.
 */
@RestController
@RequestMapping("/environment")
class EnvironmentController(
    private val synectixProperties: SynectixProperties,
) {
    @Value("\${APP_ENVIRONMENT:development}")
    private lateinit var environment: String

    @Value("\${SERVER_PORT:8080}")
    private lateinit var serverPort: String

    /**
     * Get application environment information.
     */
    @GetMapping("/info")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('environment:read')")
    fun getEnvironmentInfo(): Map<String, Any> =
        mapOf(
            "application" to
                mapOf(
                    "name" to synectixProperties.name,
                    "version" to synectixProperties.version,
                    "description" to synectixProperties.description,
                    "environment" to synectixProperties.environment,
                ),
            "server" to
                mapOf(
                    "port" to serverPort,
                    "environment" to environment,
                ),
            "message" to "Environment variables loaded successfully!",
        )

    /**
     * Get all environment variables (for debugging - remove in production).
     */
    @GetMapping("/variables")
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('environment:read')")
    fun getAllEnvironmentVariables(): Map<String, String> =
        System.getenv().filterKeys { key ->
            key.startsWith("APP_") ||
                key.startsWith("SERVER_") ||
                key.startsWith("LOG_LEVEL_") ||
                key == "MONGODB_DATABASE"
        }
}
