package com.aquinofroilan.tessera.graphql

import com.aquinofroilan.tessera.config.TestSecurityConfig
import com.aquinofroilan.tessera.domain.hr.controller.AttendanceController
import com.aquinofroilan.tessera.domain.hr.dto.ClockRequest
import com.aquinofroilan.tessera.security.TesseraPermissionEvaluator
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest
import org.springframework.context.annotation.Import
import org.springframework.graphql.test.tester.GraphQlTester
import org.springframework.http.ResponseEntity
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.bean.override.mockito.MockitoBean

@GraphQlTest(controllers = [AttendanceGraphqlController::class])
@Import(
    TestSecurityConfig::class,
    TesseraPermissionEvaluator::class,
    GraphqlExceptionResolver::class,
    GraphqlScalarConfig::class,
    GraphqlBridgeSupport::class,
)
class AttendanceGraphqlControllerTest {
    @Autowired
    private lateinit var graphQlTester: GraphQlTester

    @MockitoBean
    private lateinit var attendanceController: AttendanceController

    @Test
    @WithMockUser(authorities = ["hr:read"])
    fun `attendance query should return json payload`() {
        `when`(attendanceController.listTimesheet(any(), null, null, null))
            .thenReturn(ResponseEntity.ok(listOf(mapOf("id" to "a1", "status" to "PRESENT"))))

        graphQlTester
            .document(
                """
                query {
                  attendance
                }
                """.trimIndent(),
            ).execute()
            .path("attendance[0].id")
            .entity(String::class.java)
            .isEqualTo("a1")
    }

    @Test
    @WithMockUser(authorities = ["hr:write"])
    fun `clockIn mutation should bridge to controller`() {
        `when`(attendanceController.clockIn(any(), any<ClockRequest>()))
            .thenReturn(ResponseEntity.ok(mapOf("id" to "a1", "status" to "PRESENT")))

        graphQlTester
            .document(
                """
                mutation(${'$'}input: JSON!) {
                  clockIn(input: ${'$'}input)
                }
                """.trimIndent(),
            ).variable("input", mapOf("employeeId" to "00000000-0000-0000-0000-000000000001"))
            .execute()
            .path("clockIn.status")
            .entity(String::class.java)
            .isEqualTo("PRESENT")
    }

    @Test
    @WithMockUser(authorities = ["hr:read"])
    fun `clockIn mutation should be denied without write authority`() {
        graphQlTester
            .document(
                """
                mutation(${'$'}input: JSON!) {
                  clockIn(input: ${'$'}input)
                }
                """.trimIndent(),
            ).variable("input", mapOf("employeeId" to "e1"))
            .execute()
            .errors()
            .satisfy { errors ->
                org.assertj.core.api.Assertions
                    .assertThat(errors)
                    .isNotEmpty()
            }
    }
}
