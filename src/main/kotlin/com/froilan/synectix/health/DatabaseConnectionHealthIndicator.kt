package com.froilan.synectix.health

import org.springframework.boot.health.contributor.Health
import org.springframework.boot.health.contributor.HealthIndicator
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.stereotype.Component

@Component
class DatabaseConnectionHealthIndicator(
    private val mongoTemplate: MongoTemplate
) : HealthIndicator {

    override fun health(): Health {
        return try {
            val db = mongoTemplate.db
            db.runCommand(org.bson.Document("ping", 1))

            Health.up()
                .withDetail("database", "MongoDB")
                .withDetail("status", "Connected")
                .withDetail("databaseName", db.name)
                .build()
        } catch (e: Exception) {
            Health.down()
                .withDetail("error", e.message ?: "Unknown database error")
                .withException(e)
                .build()
        }
    }
}
