package com.aquinofroilan.tessera.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.springframework.dao.DataAccessResourceFailureException
import org.springframework.jdbc.core.JdbcTemplate

class HealthControllerTest {
    private lateinit var jdbcTemplate: JdbcTemplate
    private lateinit var controller: HealthController

    @BeforeEach
    fun setup() {
        jdbcTemplate = mock(JdbcTemplate::class.java)
        controller = HealthController(jdbcTemplate)
    }

    @Test
    fun `health returns 200 when DB ping succeeds`() {
        `when`(jdbcTemplate.queryForObject(eq("SELECT 1"), eq(Int::class.java))).thenReturn(1)
        val response = controller.health()
        assertThat(response.statusCode.value()).isEqualTo(200)
        val body = response.body!!
        assertThat(body["status"]).isEqualTo("UP")
        @Suppress("UNCHECKED_CAST")
        val db = body["database"] as Map<String, Any>
        assertThat(db["status"]).isEqualTo("UP")
        assertThat(db["database"]).isEqualTo("PostgreSQL")
    }

    @Test
    fun `health returns 503 when DB ping fails`() {
        `when`(jdbcTemplate.queryForObject(any<String>(), any<Class<Int>>()))
            .thenThrow(DataAccessResourceFailureException("connection refused"))
        val response = controller.health()
        assertThat(response.statusCode.value()).isEqualTo(503)
        val body = response.body!!
        assertThat(body["status"]).isEqualTo("DOWN")
    }

    @Test
    fun `simpleHealth returns 200`() {
        val response = controller.simpleHealth()
        assertThat(response.statusCode.value()).isEqualTo(200)
        assertThat(response.body!!["status"]).isEqualTo("UP")
    }

    @Test
    fun `detailedHealth includes system info`() {
        `when`(jdbcTemplate.queryForObject(eq("SELECT 1"), eq(Int::class.java))).thenReturn(1)
        val response = controller.detailedHealth()
        assertThat(response.statusCode.value()).isEqualTo(200)
        val body = response.body!!
        assertThat(body).containsKey("system")
        assertThat(body).containsKey("database")
        assertThat(body).containsKey("components")
    }
}
