package com.aquinofroilan.tessera.graphql

import com.aquinofroilan.tessera.config.TestSecurityConfig
import com.aquinofroilan.tessera.domain.project.controller.ProjectBudgetController
import com.aquinofroilan.tessera.security.TesseraPermissionEvaluator
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest
import org.springframework.context.annotation.Import
import org.springframework.graphql.test.tester.GraphQlTester
import org.springframework.http.ResponseEntity
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.util.UUID

@GraphQlTest(controllers = [ProjectBudgetGraphqlController::class])
@Import(
    TestSecurityConfig::class,
    TesseraPermissionEvaluator::class,
    GraphqlExceptionResolver::class,
    GraphqlScalarConfig::class,
    GraphqlBridgeSupport::class,
)
class ProjectBudgetGraphqlControllerTest {
    @Autowired
    private lateinit var graphQlTester: GraphQlTester

    @MockitoBean
    private lateinit var projectBudgetController: ProjectBudgetController

    @Test
    @WithMockUser(authorities = ["projects:read"])
    fun `projectBudgets query should return json payload`() {
        `when`(projectBudgetController.listBudgets(UUID.fromString("00000000-0000-0000-0000-000000000199")))
            .thenReturn(ResponseEntity.ok(listOf(mapOf("category" to "LABOR", "budgetAmount" to "1000"))))

        graphQlTester
            .document(
                """
                query {
                  projectBudgets(projectId: "00000000-0000-0000-0000-000000000199")
                }
                """.trimIndent(),
            ).execute()
            .path("projectBudgets[0].category")
            .entity(String::class.java)
            .isEqualTo("LABOR")
    }

    @Test
    @WithMockUser(authorities = ["projects:read"])
    fun `setProjectBudget mutation should be denied without write authority`() {
        graphQlTester
            .document(
                """
                mutation(${'$'}input: JSON!) {
                  setProjectBudget(projectId: "00000000-0000-0000-0000-000000000199", input: ${'$'}input)
                }
                """.trimIndent(),
            ).variable("input", mapOf("category" to "LABOR", "budgetAmount" to "1000"))
            .execute()
            .errors()
            .satisfy { errors ->
                org.assertj.core.api.Assertions
                    .assertThat(errors)
                    .isNotEmpty()
            }
    }
}
