package com.aquinofroilan.tessera.graphql

import com.aquinofroilan.tessera.config.TestSecurityConfig
import com.aquinofroilan.tessera.domain.project.controller.ProjectTaskController
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

@GraphQlTest(controllers = [ProjectTaskGraphqlController::class])
@Import(
    TestSecurityConfig::class,
    TesseraPermissionEvaluator::class,
    GraphqlExceptionResolver::class,
    GraphqlScalarConfig::class,
    GraphqlBridgeSupport::class,
)
class ProjectTaskGraphqlControllerTest {
    @Autowired
    private lateinit var graphQlTester: GraphQlTester

    @MockitoBean
    private lateinit var projectTaskController: ProjectTaskController

    @Test
    @WithMockUser(authorities = ["projects:read"])
    fun `projectTasks query should return json payload`() {
        `when`(projectTaskController.listTasks(anyOrNull(), anyOrNull()))
            .thenReturn(ResponseEntity.ok(listOf(mapOf("id" to "00000000-0000-0000-0000-000000000199", "status" to "TODO"))))

        graphQlTester
            .document(
                """
                query {
                  projectTasks(projectId: "00000000-0000-0000-0000-000000000199")
                }
                """.trimIndent(),
            ).execute()
            .path("projectTasks[0].id")
            .entity(String::class.java)
            .isEqualTo("00000000-0000-0000-0000-000000000199")
    }

    @Test
    @WithMockUser(authorities = ["projects:read"])
    fun `createProjectTask mutation should be denied without write authority`() {
        graphQlTester
            .document(
                """
                mutation(${'$'}input: JSON!) {
                  createProjectTask(projectId: "00000000-0000-0000-0000-000000000199", input: ${'$'}input)
                }
                """.trimIndent(),
            ).variable("input", mapOf("name" to "Design"))
            .execute()
            .errors()
            .satisfy { errors ->
                org.assertj.core.api.Assertions
                    .assertThat(errors)
                    .isNotEmpty()
            }
    }
}
