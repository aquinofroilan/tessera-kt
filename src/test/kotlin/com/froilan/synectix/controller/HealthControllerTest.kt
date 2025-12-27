package com.froilan.synectix.controller

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import javax.sql.DataSource
import org.mockito.Mockito.`when`
import java.sql.Connection
import java.sql.DatabaseMetaData

@WebMvcTest(HealthController::class)
@ActiveProfiles("test")
class HealthControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var dataSource: DataSource

    @MockBean
    private lateinit var connection: Connection

    @MockBean
    private lateinit var metaData: DatabaseMetaData

    @Test
    fun `should return health status UP when database is healthy`() {
        `when`(dataSource.connection).thenReturn(connection)
        `when`(connection.isValid(5)).thenReturn(true)
        `when`(connection.metaData).thenReturn(metaData)
        `when`(metaData.databaseProductName).thenReturn("H2")
        `when`(metaData.driverName).thenReturn("H2 JDBC Driver")
        `when`(metaData.url).thenReturn("jdbc:h2:mem:testdb")

        mockMvc.perform(get("/api/health"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.application").value("Synectix ERP System"))
            .andExpect(jsonPath("$.version").value("0.0.1-SNAPSHOT"))
            .andExpect(jsonPath("$.database.status").value("UP"))
    }

    @Test
    fun `should return simple health status`() {
        mockMvc.perform(get("/api/health/simple"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.message").value("Synectix application is running"))
    }

    @Test
    fun `should return detailed health information`() {
        `when`(dataSource.connection).thenReturn(connection)
        `when`(connection.isValid(5)).thenReturn(true)
        `when`(connection.metaData).thenReturn(metaData)
        `when`(metaData.databaseProductName).thenReturn("H2")
        `when`(metaData.driverName).thenReturn("H2 JDBC Driver")
        `when`(metaData.url).thenReturn("jdbc:h2:mem:testdb")

        mockMvc.perform(get("/api/health/detailed"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.application.name").value("Synectix ERP System"))
            .andExpect(jsonPath("$.system.javaVersion").exists())
            .andExpect(jsonPath("$.database.status").value("UP"))
            .andExpect(jsonPath("$.components.security.status").value("UP"))
    }

    @Test
    fun `should return service unavailable when database is down`() {
        `when`(dataSource.connection).thenThrow(RuntimeException("Database connection failed"))

        mockMvc.perform(get("/api/health"))
            .andExpect(status().isServiceUnavailable)
            .andExpect(jsonPath("$.status").value("DOWN"))
            .andExpect(jsonPath("$.database.status").value("DOWN"))
    }
}
