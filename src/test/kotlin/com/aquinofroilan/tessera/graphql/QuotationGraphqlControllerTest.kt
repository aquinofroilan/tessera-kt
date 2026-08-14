package com.aquinofroilan.tessera.graphql

import com.aquinofroilan.tessera.config.TestSecurityConfig
import com.aquinofroilan.tessera.domain.sales.controller.QuotationController
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
            .thenReturn(ResponseEntity.ok(listOf(mapOf("id" to "00000000-0000-0000-0000-000000000199", "status" to "DRAFT"))))

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
            .isEqualTo("00000000-0000-0000-0000-000000000199")
    }

    @Test
    @WithMockUser(authorities = ["sales:write"])
    fun `acceptQuotation mutation should bridge to controller`() {
        `when`(quotationController.acceptQuotation(UUID.fromString("00000000-0000-0000-0000-000000000199")))
            .thenReturn(ResponseEntity.ok(mapOf("id" to "00000000-0000-0000-0000-000000000199", "status" to "ACCEPTED")))

        graphQlTester
            .document(
                """
                mutation {
                  acceptQuotation(id: "00000000-0000-0000-0000-000000000199")
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
