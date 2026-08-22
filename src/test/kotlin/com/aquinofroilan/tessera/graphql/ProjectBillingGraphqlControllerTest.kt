package com.aquinofroilan.tessera.graphql

import com.aquinofroilan.tessera.config.TestSecurityConfig
import com.aquinofroilan.tessera.domain.project.controller.ProjectBillingController
import com.aquinofroilan.tessera.security.TesseraPermissionEvaluator
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest
import org.springframework.context.annotation.Import
import org.springframework.graphql.test.tester.GraphQlTester
import org.springframework.http.ResponseEntity
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.util.UUID

@GraphQlTest(controllers = [ProjectBillingGraphqlController::class])
@Import(
    TestSecurityConfig::class,
    TesseraPermissionEvaluator::class,
    GraphqlExceptionResolver::class,
    GraphqlScalarConfig::class,
    GraphqlBridgeSupport::class,
)
class ProjectBillingGraphqlControllerTest {
    @Autowired
    private lateinit var graphQlTester: GraphQlTester

    @MockitoBean
    private lateinit var projectBillingController: ProjectBillingController

    @Test
    @WithMockUser(authorities = ["projects:write"])
    fun `generateProjectInvoice mutation should bridge to controller`() {
        whenever(projectBillingController.generateInvoice(any(), eq(UUID.fromString("00000000-0000-0000-0000-000000000199")), anyOrNull()))
            .thenReturn(ResponseEntity.ok(mapOf("id" to "00000000-0000-0000-0000-000000000199", "status" to "DRAFT")))

        graphQlTester
            .document(
                """
                mutation {
                  generateProjectInvoice(projectId: "00000000-0000-0000-0000-000000000199")
                }
                """.trimIndent(),
            ).execute()
            .path("generateProjectInvoice.id")
            .entity(String::class.java)
            .isEqualTo("00000000-0000-0000-0000-000000000199")
    }

    @Test
    @WithMockUser(authorities = ["projects:read"])
    fun `generateProjectInvoice mutation should be denied without write authority`() {
        graphQlTester
            .document(
                """
                mutation {
                  generateProjectInvoice(projectId: "00000000-0000-0000-0000-000000000199")
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
