package com.froilan.synectix.controller

import com.froilan.synectix.aspect.LoggingAspect
import com.froilan.synectix.config.TestSecurityConfig
import com.froilan.synectix.repository.SessionTokenRepository
import com.froilan.synectix.repository.UserRepository
import com.froilan.synectix.security.RolePermissionCache
import com.froilan.synectix.security.SynectixPermissionEvaluator
import com.mongodb.client.MongoDatabase
import org.bson.Document
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(controllers = [HealthController::class])
@Import(LoggingAspect::class, TestSecurityConfig::class, SynectixPermissionEvaluator::class)
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

    @MockitoBean
    private lateinit var rolePermissionCache: RolePermissionCache

    @Test
    fun `should return health status UP when database is healthy`() {
        val buildInfo = Document("version", "7.0.0")
        `when`(mongoTemplate.db).thenReturn(mongoDatabase)
        `when`(mongoDatabase.name).thenReturn("synectix-test")
        `when`(mongoDatabase.runCommand(ArgumentMatchers.any(Document::class.java))).thenReturn(buildInfo)

        mockMvc
            .perform(get("/health"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.application").value("Synectix ERP System"))
            .andExpect(jsonPath("$.version").value("0.0.1-SNAPSHOT"))
            .andExpect(jsonPath("$.database.status").value("UP"))
    }

    @Test
    fun `should return simple health status`() {
        mockMvc
            .perform(get("/health/simple"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.message").value("Synectix application is running"))
    }

    @Test
    fun `should return detailed health information`() {
        val buildInfo = Document("version", "7.0.0")
        `when`(mongoTemplate.db).thenReturn(mongoDatabase)
        `when`(mongoDatabase.name).thenReturn("synectix-test")
        `when`(mongoDatabase.runCommand(ArgumentMatchers.any(Document::class.java))).thenReturn(buildInfo)

        mockMvc
            .perform(get("/health/detailed"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.application.name").value("Synectix ERP System"))
            .andExpect(jsonPath("$.system.javaVersion").exists())
            .andExpect(jsonPath("$.database.status").value("UP"))
            .andExpect(jsonPath("$.components.security.status").value("UP"))
    }

    @Test
    fun `should return service unavailable when database is down`() {
        `when`(mongoTemplate.db).thenThrow(RuntimeException("Database connection failed"))

        mockMvc
            .perform(get("/health"))
            .andExpect(status().isServiceUnavailable)
            .andExpect(jsonPath("$.status").value("DOWN"))
            .andExpect(jsonPath("$.database.status").value("DOWN"))
    }
}
