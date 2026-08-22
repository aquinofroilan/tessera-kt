package com.aquinofroilan.tessera.graphql

import com.aquinofroilan.tessera.config.TestSecurityConfig
import com.aquinofroilan.tessera.domain.procurement.controller.PurchaseOrderController
import com.aquinofroilan.tessera.domain.sales.controller.SalesOrderController
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
import java.util.UUID

@GraphQlTest(controllers = [OrderGraphqlController::class])
@Import(
    TestSecurityConfig::class,
    TesseraPermissionEvaluator::class,
    GraphqlExceptionResolver::class,
    GraphqlScalarConfig::class,
    GraphqlBridgeSupport::class,
)
class OrderGraphqlControllerTest {
    @Autowired
    private lateinit var graphQlTester: GraphQlTester

    @MockitoBean
    private lateinit var purchaseOrderController: PurchaseOrderController

    @MockitoBean
    private lateinit var salesOrderController: SalesOrderController

    @Test
    @WithMockUser(authorities = ["procurement:read"])
    fun `purchaseOrders query should return json payload`() {
        `when`(purchaseOrderController.listPurchaseOrders(any(), null, null))
            .thenReturn(ResponseEntity.ok(listOf(mapOf("id" to "00000000-0000-0000-0000-000000000199", "status" to "DRAFT"))))

        graphQlTester
            .document(
                """
                query {
                  purchaseOrders
                }
                """.trimIndent(),
            ).execute()
            .path("purchaseOrders[0].id")
            .entity(String::class.java)
            .isEqualTo("00000000-0000-0000-0000-000000000199")
    }

    @Test
    @WithMockUser(authorities = ["sales:approve"])
    fun `approveSalesOrder mutation should bridge to controller`() {
        `when`(salesOrderController.approveSalesOrder(any(), UUID.fromString("00000000-0000-0000-0000-000000000199")))
            .thenReturn(ResponseEntity.ok(mapOf("id" to "00000000-0000-0000-0000-000000000199", "status" to "APPROVED")))

        graphQlTester
            .document(
                """
                mutation {
                  approveSalesOrder(id: "00000000-0000-0000-0000-000000000199")
                }
                """.trimIndent(),
            ).execute()
            .path("approveSalesOrder.status")
            .entity(String::class.java)
            .isEqualTo("APPROVED")
    }

    @Test
    fun `purchaseOrders query should be denied without authority`() {
        graphQlTester
            .document(
                """
                query {
                  purchaseOrders
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
