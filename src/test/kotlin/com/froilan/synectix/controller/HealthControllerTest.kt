package com.froilan.synectix.controller

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.data.mongodb.core.MongoTemplate
import org.mockito.Mockito.`when`
import com.mongodb.client.MongoDatabase
import org.bson.Document
import org.springframework.context.annotation.Import
import com.froilan.synectix.aspect.LoggingAspect
import com.froilan.synectix.repository.SessionTokenRepository
import com.froilan.synectix.repository.UserRepository

@WebMvcTest(controllers = [HealthController::class])
@Import(LoggingAspect::class, com.froilan.synectix.config.TestSecurityConfig::class)
@ActiveProfiles("test")
class HealthControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var mongoTemplate: MongoTemplate

    @MockitoBean
    private lateinit var mongoDatabase: MongoDatabase

    @MockitoBean
    private lateinit var sessionTokenRepository: SessionTokenRepository

    @MockitoBean
    private lateinit var userRepository: UserRepository

    @Test
    fun `should return health status UP when database is healthy`() {
        // Given
        val buildInfo = Document("version", "7.0.0")
        `when`(mongoTemplate.db).thenReturn(mongoDatabase)
        `when`(mongoDatabase.name).thenReturn("synectix-test")
        `when`(mongoDatabase.runCommand(org.mockito.ArgumentMatchers.any(Document::class.java))).thenReturn(buildInfo)

        // When & Then
        mockMvc.perform(get("/health"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.application").value("Synectix ERP System"))
            .andExpect(jsonPath("$.version").value("0.0.1-SNAPSHOT"))
            .andExpect(jsonPath("$.database.status").value("UP"))
    }

    @Test
    fun `should return simple health status`() {
        mockMvc.perform(get("/health/simple"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.message").value("Synectix application is running"))
    }

    @Test
    fun `should return detailed health information`() {
        // Given
        val buildInfo = Document("version", "7.0.0")
        `when`(mongoTemplate.db).thenReturn(mongoDatabase)
        `when`(mongoDatabase.name).thenReturn("synectix-test")
        `when`(mongoDatabase.runCommand(org.mockito.ArgumentMatchers.any(Document::class.java))).thenReturn(buildInfo)

        // When & Then
        mockMvc.perform(get("/health/detailed"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.application.name").value("Synectix ERP System"))
            .andExpect(jsonPath("$.system.javaVersion").exists())
            .andExpect(jsonPath("$.database.status").value("UP"))
            .andExpect(jsonPath("$.components.security.status").value("UP"))
    }

    @Test
    fun `should return service unavailable when database is down`() {
        // Given
        `when`(mongoTemplate.db).thenThrow(RuntimeException("Database connection failed"))

        // When & Then
        mockMvc.perform(get("/health"))
            .andExpect(status().isServiceUnavailable)
            .andExpect(jsonPath("$.status").value("DOWN"))
            .andExpect(jsonPath("$.database.status").value("DOWN"))
    }
}
