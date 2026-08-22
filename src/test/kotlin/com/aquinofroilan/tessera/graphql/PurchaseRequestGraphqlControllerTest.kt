package com.aquinofroilan.tessera.graphql

import com.aquinofroilan.tessera.config.TestSecurityConfig
import com.aquinofroilan.tessera.domain.procurement.controller.PurchaseRequestController
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

@GraphQlTest(controllers = [PurchaseRequestGraphqlController::class])
@Import(
    TestSecurityConfig::class,
    TesseraPermissionEvaluator::class,
    GraphqlExceptionResolver::class,
    GraphqlScalarConfig::class,
    GraphqlBridgeSupport::class,
)
class PurchaseRequestGraphqlControllerTest {
    @Autowired
    private lateinit var graphQlTester: GraphQlTester

    @MockitoBean
    private lateinit var purchaseRequestController: PurchaseRequestController

    @Test
    @WithMockUser(authorities = ["procurement:read"])
    fun `purchaseRequests query should return json payload`() {
        `when`(purchaseRequestController.listPurchaseRequests(anyOrNull(), anyOrNull(), anyOrNull()))
            .thenReturn(ResponseEntity.ok(listOf(mapOf("id" to "00000000-0000-0000-0000-000000000199", "status" to "DRAFT"))))

        graphQlTester
            .document(
                """
                query {
                  purchaseRequests
                }
                """.trimIndent(),
            ).execute()
            .path("purchaseRequests[0].id")
            .entity(String::class.java)
            .isEqualTo("00000000-0000-0000-0000-000000000199")
    }

    @Test
    @WithMockUser(authorities = ["procurement:approve"])
    fun `approvePurchaseRequest mutation should bridge to controller`() {
        `when`(purchaseRequestController.approvePurchaseRequest(anyOrNull(), anyOrNull(), anyOrNull()))
            .thenReturn(ResponseEntity.ok(mapOf("id" to "00000000-0000-0000-0000-000000000199", "status" to "APPROVED")))

        graphQlTester
            .document(
                """
                mutation {
                  approvePurchaseRequest(id: "00000000-0000-0000-0000-000000000199")
                }
                """.trimIndent(),
            ).execute()
            .path("approvePurchaseRequest.status")
            .entity(String::class.java)
            .isEqualTo("APPROVED")
    }

    @Test
    @WithMockUser(authorities = ["procurement:read"])
    fun `createPurchaseRequest mutation should be denied without write authority`() {
        graphQlTester
            .document(
                """
                mutation(${'$'}input: JSON!) {
                  createPurchaseRequest(input: ${'$'}input)
                }
                """.trimIndent(),
            ).variable("input", mapOf("lines" to emptyList<Any>()))
            .execute()
            .errors()
            .satisfy { errors ->
                org.assertj.core.api.Assertions
                    .assertThat(errors)
                    .isNotEmpty()
            }
    }
}
