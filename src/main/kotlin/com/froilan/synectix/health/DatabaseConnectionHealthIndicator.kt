package com.froilan.synectix.health

import org.springframework.boot.health.contributor.Health
import org.springframework.boot.health.contributor.HealthIndicator
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.stereotype.Component
import com.mongodb.client.MongoDatabase

@Component
class DatabaseConnectionHealthIndicator(
    private val mongoTemplate: MongoTemplate
) : HealthIndicator {

    override fun health(): Health {
        return try {
            val db: MongoDatabase = mongoTemplate.db
            val serverStatus = db.runCommand(org.bson.Document("serverStatus", 1))

            Health.up()
                .withDetail("database", "MongoDB")
                .withDetail("version", serverStatus.get("version")?.toString() ?: "unknown")
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
