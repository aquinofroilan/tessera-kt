package com.aquinofroilan.tessera.graphql

import com.aquinofroilan.tessera.config.TestSecurityConfig
import com.aquinofroilan.tessera.controller.QuotationController
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

@GraphQlTest(controllers = [QuotationGraphqlController::class])
@Import(
    TestSecurityConfig::class,
    TesseraPermissionEvaluator::class,
    GraphqlExceptionResolver::class,
    GraphqlScalarConfig::class,
    GraphqlBridgeSupport::class,
)
class QuotationGraphqlControllerTest {
    @Autowired
    private lateinit var graphQlTester: GraphQlTester

    @MockitoBean
    private lateinit var quotationController: QuotationController

    @Test
    @WithMockUser(authorities = ["sales:read"])
    fun `quotations query should return json payload`() {
        `when`(quotationController.listQuotations(null, null))
            .thenReturn(ResponseEntity.ok(listOf(mapOf("id" to "q1", "status" to "DRAFT"))))

        graphQlTester
            .document(
                """
                query {
                  quotations
                }
                """.trimIndent(),
            ).execute()
            .path("quotations[0].id")
            .entity(String::class.java)
            .isEqualTo("q1")
    }

    @Test
    @WithMockUser(authorities = ["sales:write"])
    fun `acceptQuotation mutation should bridge to controller`() {
        `when`(quotationController.acceptQuotation("q1"))
            .thenReturn(ResponseEntity.ok(mapOf("id" to "q1", "status" to "ACCEPTED")))

        graphQlTester
            .document(
                """
                mutation {
                  acceptQuotation(id: "q1")
                }
                """.trimIndent(),
            ).execute()
            .path("acceptQuotation.status")
            .entity(String::class.java)
            .isEqualTo("ACCEPTED")
    }

    @Test
    @WithMockUser(authorities = ["sales:read"])
    fun `createQuotation mutation should be denied without write authority`() {
        graphQlTester
            .document(
                """
                mutation(${'$'}input: JSON!) {
                  createQuotation(input: ${'$'}input)
                }
                """.trimIndent(),
            ).variable("input", mapOf("customerId" to "c1", "lines" to emptyList<Any>()))
            .execute()
            .errors()
            .satisfy { errors ->
                org.assertj.core.api.Assertions
                    .assertThat(errors)
                    .isNotEmpty()
            }
    }
}
