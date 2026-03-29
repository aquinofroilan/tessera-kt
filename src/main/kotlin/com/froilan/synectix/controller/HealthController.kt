package com.froilan.synectix.controller

import com.froilan.synectix.annotation.LogLevel
import com.froilan.synectix.annotation.Loggable
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.data.mongodb.core.MongoTemplate
import java.time.LocalDateTime

@RestController
@RequestMapping("/health")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.DEBUG)
class HealthController(
    @Autowired private val mongoTemplate: MongoTemplate
) {

    @GetMapping
    fun health(): ResponseEntity<Map<String, Any>> {
        val healthData = mutableMapOf<String, Any>(
            "status" to "UP",
            "timestamp" to LocalDateTime.now(),
            "application" to "Synectix ERP System",
            "version" to "0.0.1-SNAPSHOT"
        )

        val dbStatus = checkDatabaseHealth()
        healthData["database"] = dbStatus

        val isHealthy = dbStatus["status"] == "UP"
        if (!isHealthy) {
            healthData["status"] = "DOWN"
            return ResponseEntity.status(503).body(healthData)
        }

        return ResponseEntity.ok(healthData)
    }

    @GetMapping("/simple")
    fun simpleHealth(): ResponseEntity<Map<String, String>> {
        return ResponseEntity.ok(mapOf(
            "status" to "UP",
            "message" to "Synectix application is running"
        ))
    }

    @GetMapping("/detailed")
    fun detailedHealth(): ResponseEntity<Map<String, Any>> {
        val detailedHealth = mutableMapOf<String, Any>(
            "status" to "UP",
            "timestamp" to LocalDateTime.now(),
            "application" to mapOf(
                "name" to "Synectix ERP System",
                "version" to "0.0.1-SNAPSHOT",
                "description" to "Synectix an ERP System"
            ),
            "system" to mapOf(
                "javaVersion" to System.getProperty("java.version"),
                "osName" to System.getProperty("os.name"),
                "osVersion" to System.getProperty("os.version"),
                "availableProcessors" to Runtime.getRuntime().availableProcessors(),
                "maxMemory" to "${Runtime.getRuntime().maxMemory() / 1024 / 1024} MB",
                "totalMemory" to "${Runtime.getRuntime().totalMemory() / 1024 / 1024} MB",
                "freeMemory" to "${Runtime.getRuntime().freeMemory() / 1024 / 1024} MB"
            )
        )

        detailedHealth["database"] = checkDatabaseHealth()

        detailedHealth["components"] = mapOf(
            "security" to mapOf("status" to "UP"),
            "logging" to mapOf("status" to "UP"),
            "aop" to mapOf("status" to "UP")
        )

        return ResponseEntity.ok(detailedHealth)
    }

    private fun checkDatabaseHealth(): Map<String, Any> {
        return try {
            val db = mongoTemplate.db
            db.runCommand(org.bson.Document("ping", 1))

            mapOf(
                "status" to "UP",
                "database" to "MongoDB",
                "status_detail" to "Connected",
                "databaseName" to db.name
            )
        } catch (e: Exception) {
            mapOf(
                "status" to "DOWN",
                "error" to (e.message ?: "Unknown database error")
            )
        }
    }
}
