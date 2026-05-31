package com.aquinofroilan.tessera.health

import org.springframework.boot.health.contributor.Health
import org.springframework.boot.health.contributor.HealthIndicator
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Component

@Component
class DatabaseConnectionHealthIndicator(
    private val jdbcTemplate: JdbcTemplate,
) : HealthIndicator {
    override fun health(): Health =
        try {
            jdbcTemplate.queryForObject("SELECT 1", Int::class.java)
            Health
                .up()
                .withDetail("database", "PostgreSQL")
                .withDetail("status", "Connected")
                .build()
        } catch (e: Exception) {
            Health
                .down()
                .withDetail("error", e.message ?: "Unknown database error")
                .withException(e)
                .build()
        }
}
