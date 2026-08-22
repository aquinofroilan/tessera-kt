package com.aquinofroilan.tessera.graphql

import com.aquinofroilan.tessera.config.TestSecurityConfig
import com.aquinofroilan.tessera.domain.project.controller.TimeEntryController
import com.aquinofroilan.tessera.security.TesseraPermissionEvaluator
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.kotlin.anyOrNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest
import org.springframework.context.annotation.Import
import org.springframework.graphql.test.tester.GraphQlTester
import org.springframework.http.ResponseEntity
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.bean.override.mockito.MockitoBean

@GraphQlTest(controllers = [TimeEntryGraphqlController::class])
@Import(
    TestSecurityConfig::class,
    TesseraPermissionEvaluator::class,
    GraphqlExceptionResolver::class,
    GraphqlScalarConfig::class,
    GraphqlBridgeSupport::class,
)
class TimeEntryGraphqlControllerTest {
    @Autowired
    private lateinit var graphQlTester: GraphQlTester

    @MockitoBean
    private lateinit var timeEntryController: TimeEntryController

    @Test
    @WithMockUser(authorities = ["projects:read"])
    fun `timeEntries query should return json payload`() {
        `when`(timeEntryController.listTimeEntries(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull()))
            .thenReturn(ResponseEntity.ok(listOf(mapOf("id" to "00000000-0000-0000-0000-000000000199", "status" to "DRAFT"))))

        graphQlTester
            .document(
                """
                query {
                  timeEntries
                }
                """.trimIndent(),
            ).execute()
            .path("timeEntries[0].id")
            .entity(String::class.java)
            .isEqualTo("00000000-0000-0000-0000-000000000199")
    }

    @Test
    @WithMockUser(authorities = ["projects:approve"])
    fun `approveTimeEntry mutation should bridge to controller`() {
        `when`(timeEntryController.approveTimeEntry(anyOrNull(), anyOrNull()))
            .thenReturn(ResponseEntity.ok(mapOf("id" to "00000000-0000-0000-0000-000000000199", "status" to "APPROVED")))

        graphQlTester
            .document(
                """
                mutation {
                  approveTimeEntry(id: "00000000-0000-0000-0000-000000000199")
                }
                """.trimIndent(),
            ).execute()
            .path("approveTimeEntry.status")
            .entity(String::class.java)
            .isEqualTo("APPROVED")
    }

    @Test
    @WithMockUser(authorities = ["projects:read"])
    fun `createTimeEntry mutation should be denied without write authority`() {
        graphQlTester
            .document(
                """
                mutation(${'$'}input: JSON!) {
                  createTimeEntry(input: ${'$'}input)
                }
                """.trimIndent(),
            ).variable("input", mapOf("employeeId" to "e1", "projectId" to "p1"))
            .execute()
            .errors()
            .satisfy { errors ->
                org.assertj.core.api.Assertions
                    .assertThat(errors)
                    .isNotEmpty()
            }
    }
}
