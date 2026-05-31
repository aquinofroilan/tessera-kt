package com.aquinofroilan.tessera.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

/**
 * Configuration properties for the Tessera application.
 * These properties are loaded from application.properties and can be overridden by environment variables.
 */
@Component
@ConfigurationProperties(prefix = "app")
data class TesseraProperties(
    var name: String = "Tessera ERP System",
    var version: String = "0.0.1-SNAPSHOT",
    var description: String = "Tessera an ERP System",
    var environment: String = "development",
)

/**
 * Database configuration properties.
 */
@Component
@ConfigurationProperties(prefix = "spring.data.mongodb")
data class DatabaseProperties(
    var uri: String = "mongodb://localhost:27017/tessera",
    var database: String = "tessera",
)

/**
 * Security configuration properties.
 */
@Component
@ConfigurationProperties(prefix = "security")
data class SecurityProperties(
    var jwt: JwtProperties = JwtProperties(),
) {
    data class JwtProperties(
        var secret: String = "\${JWT_SECRET:default-secret}",
        var expiration: Long = 86400000L,
    )
}
