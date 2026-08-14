package com.aquinofroilan.tessera.graphql

import com.aquinofroilan.tessera.config.TestSecurityConfig
import com.aquinofroilan.tessera.domain.hr.controller.SelfServiceController
import com.aquinofroilan.tessera.domain.platform.dto.SubmitSelfLeaveRequest
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

@GraphQlTest(controllers = [SelfServiceGraphqlController::class])
@Import(
    TestSecurityConfig::class,
    TesseraPermissionEvaluator::class,
    GraphqlExceptionResolver::class,
    GraphqlScalarConfig::class,
    GraphqlBridgeSupport::class,
)
class SelfServiceGraphqlControllerTest {
    @Autowired
    private lateinit var graphQlTester: GraphQlTester

    @MockitoBean
    private lateinit var selfServiceController: SelfServiceController

    @Test
    @WithMockUser(authorities = ["hr:read"])
    fun `me query returns the caller profile`() {
        `when`(selfServiceController.myProfile())
            .thenReturn(ResponseEntity.ok(mapOf("id" to "e1", "firstName" to "Ada")))

        graphQlTester
            .document(
                """
                query {
                  me
                }
                """.trimIndent(),
            ).execute()
            .path("me.id")
            .entity(String::class.java)
            .isEqualTo("e1")
    }

    @Test
    @WithMockUser(authorities = ["hr:read"])
    fun `submitMyLeave mutation bridges to controller`() {
        `when`(selfServiceController.submitLeave(any<SubmitSelfLeaveRequest>()))
            .thenReturn(ResponseEntity.ok(mapOf("id" to "lr1", "status" to "PENDING")))

        graphQlTester
            .document(
                """
                mutation(${'$'}input: JSON!) {
                  submitMyLeave(input: ${'$'}input)
                }
                """.trimIndent(),
            ).variable(
                "input",
                mapOf(
                    "leaveTypeId" to "82d745af-a33b-3e13-adff-05141b0d976d",
                    "startDate" to "2026-05-01",
                    "endDate" to "2026-05-03",
                ),
            ).execute()
            .path("submitMyLeave.status")
            .entity(String::class.java)
            .isEqualTo("PENDING")
    }

    @Test
    fun `me query is denied for an unauthenticated caller`() {
        graphQlTester
            .document(
                """
                query {
                  me
                }
                """.trimIndent(),
            ).execute()
            .errors()
            .satisfy { errors ->
                org.assertj.core.api.Assertions
                    .assertThat(errors)
                    .isNotEmpty()
            }
    }
}
