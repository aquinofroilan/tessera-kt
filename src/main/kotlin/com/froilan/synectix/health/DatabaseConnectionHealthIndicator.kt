package com.froilan.synectix.health

import org.springframework.boot.health.contributor.Health
import org.springframework.boot.health.contributor.HealthIndicator
import org.springframework.stereotype.Component

@Component
class DatabaseConnectionHealthIndicator(
    private val dataSource: javax.sql.DataSource
) : HealthIndicator {

    override fun health(): Health {
        return try {
            dataSource.connection.use { connection ->
                if (connection.isValid(5)) {
                    Health.up()
                        .withDetail("database", connection.metaData.databaseProductName)
                        .withDetail("driver", connection.metaData.driverName)
                        .withDetail("status", "Connected")
                        .build()
                } else {
                    Health.down()
                        .withDetail("error", "Database connection is not valid")
                        .build()
                }
            }
        } catch (e: Exception) {
            Health.down()
                .withDetail("error", e.message ?: "Unknown database error")
                .withException(e)
                .build()
        }
    }
}
