package com.aquinofroilan.tessera.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import org.slf4j.LoggerFactory
import org.springframework.dao.DataAccessException
import org.springframework.http.ResponseEntity
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime
import java.time.ZoneOffset

@RestController
@RequestMapping("/health")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.DEBUG)
class HealthController(
    private val jdbcTemplate: JdbcTemplate,
) {
    private val log = LoggerFactory.getLogger(HealthController::class.java)

    @GetMapping
    fun health(): ResponseEntity<Map<String, Any>> {
        val healthData =
            mutableMapOf<String, Any>(
                "status" to "UP",
                "timestamp" to LocalDateTime.now(ZoneOffset.UTC),
                "application" to "Tessera ERP System",
                "version" to "0.0.1-SNAPSHOT",
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
    fun simpleHealth(): ResponseEntity<Map<String, String>> =
        ResponseEntity.ok(
            mapOf(
                "status" to "UP",
                "message" to "Tessera application is running",
            ),
        )

    @GetMapping("/detailed")
    fun detailedHealth(): ResponseEntity<Map<String, Any>> {
        val detailedHealth =
            mutableMapOf<String, Any>(
                "status" to "UP",
                "timestamp" to LocalDateTime.now(ZoneOffset.UTC),
                "application" to
                    mapOf(
                        "name" to "Tessera ERP System",
                        "version" to "0.0.1-SNAPSHOT",
                        "description" to "Tessera an ERP System",
                    ),
                "system" to
                    mapOf(
                        "javaVersion" to System.getProperty("java.version"),
                        "osName" to System.getProperty("os.name"),
                        "osVersion" to System.getProperty("os.version"),
                        "availableProcessors" to Runtime.getRuntime().availableProcessors(),
                        "maxMemory" to "${Runtime.getRuntime().maxMemory() / 1024 / 1024} MB",
                        "totalMemory" to "${Runtime.getRuntime().totalMemory() / 1024 / 1024} MB",
                        "freeMemory" to "${Runtime.getRuntime().freeMemory() / 1024 / 1024} MB",
                    ),
            )

        detailedHealth["database"] = checkDatabaseHealth()

        detailedHealth["components"] =
            mapOf(
                "security" to mapOf("status" to "UP"),
                "logging" to mapOf("status" to "UP"),
                "aop" to mapOf("status" to "UP"),
            )

        return ResponseEntity.ok(detailedHealth)
    }

    private fun checkDatabaseHealth(): Map<String, Any> =
        try {
            jdbcTemplate.queryForObject("SELECT 1", Int::class.java)
            mapOf(
                "status" to "UP",
                "database" to "PostgreSQL",
                "status_detail" to "Connected",
            )
        } catch (e: DataAccessException) {
            mapOf(
                "status" to "DOWN",
                "error" to (e.message ?: "Unknown database error"),
            )
        } catch (e: Exception) {
            log.error("Unexpected error during health check", e)
            mapOf(
                "status" to "DOWN",
                "error" to (e.message ?: "Unknown error"),
            )
        }
}
