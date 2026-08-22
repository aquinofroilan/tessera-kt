package com.aquinofroilan.tessera.graphql

import com.aquinofroilan.tessera.config.TestSecurityConfig
import com.aquinofroilan.tessera.domain.project.controller.ProjectController
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

@GraphQlTest(controllers = [ProjectGraphqlController::class])
@Import(
    TestSecurityConfig::class,
    TesseraPermissionEvaluator::class,
    GraphqlExceptionResolver::class,
    GraphqlScalarConfig::class,
    GraphqlBridgeSupport::class,
)
class ProjectGraphqlControllerTest {
    @Autowired
    private lateinit var graphQlTester: GraphQlTester

    @MockitoBean
    private lateinit var projectController: ProjectController

    @Test
    @WithMockUser(authorities = ["projects:read"])
    fun `projects query should return json payload`() {
        `when`(projectController.listProjects(null, null))
            .thenReturn(ResponseEntity.ok(listOf(mapOf("id" to "00000000-0000-0000-0000-000000000199", "status" to "PLANNED"))))

        graphQlTester
            .document(
                """
                query {
                  projects
                }
                """.trimIndent(),
            ).execute()
            .path("projects[0].id")
            .entity(String::class.java)
            .isEqualTo("00000000-0000-0000-0000-000000000199")
    }

    @Test
    @WithMockUser(authorities = ["projects:write"])
    fun `activateProject mutation should bridge to controller`() {
        `when`(projectController.activateProject(UUID.fromString("00000000-0000-0000-0000-000000000199")))
            .thenReturn(ResponseEntity.ok(mapOf("id" to "00000000-0000-0000-0000-000000000199", "status" to "ACTIVE")))

        graphQlTester
            .document(
                """
                mutation {
                  activateProject(id: "00000000-0000-0000-0000-000000000199")
                }
                """.trimIndent(),
            ).execute()
            .path("activateProject.status")
            .entity(String::class.java)
            .isEqualTo("ACTIVE")
    }

    @Test
    @WithMockUser(authorities = ["projects:read"])
    fun `createProject mutation should be denied without write authority`() {
        graphQlTester
            .document(
                """
                mutation(${'$'}input: JSON!) {
                  createProject(input: ${'$'}input)
                }
                """.trimIndent(),
            ).variable("input", mapOf("name" to "Apollo"))
            .execute()
            .errors()
            .satisfy { errors ->
                org.assertj.core.api.Assertions
                    .assertThat(errors)
                    .isNotEmpty()
            }
    }
}
