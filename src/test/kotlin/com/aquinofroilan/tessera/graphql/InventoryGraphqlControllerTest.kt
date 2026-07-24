package com.aquinofroilan.tessera.graphql

import com.aquinofroilan.tessera.config.TestSecurityConfig
import com.aquinofroilan.tessera.controller.CurrencyController
import com.aquinofroilan.tessera.controller.ExchangeRateController
import com.aquinofroilan.tessera.controller.InventoryReorderRuleController
import com.aquinofroilan.tessera.controller.InventoryReportsController
import com.aquinofroilan.tessera.controller.ProductController
import com.aquinofroilan.tessera.controller.StockMovementController
import com.aquinofroilan.tessera.controller.WarehouseController
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

@GraphQlTest(controllers = [InventoryGraphqlController::class])
@Import(
    TestSecurityConfig::class,
    TesseraPermissionEvaluator::class,
    GraphqlExceptionResolver::class,
    GraphqlScalarConfig::class,
    GraphqlBridgeSupport::class,
)
class InventoryGraphqlControllerTest {
    @Autowired
    private lateinit var graphQlTester: GraphQlTester

    @MockitoBean
    private lateinit var currencyController: CurrencyController

    @MockitoBean
    private lateinit var exchangeRateController: ExchangeRateController

    @MockitoBean
    private lateinit var productController: ProductController

    @MockitoBean
    private lateinit var warehouseController: WarehouseController

    @MockitoBean
    private lateinit var stockMovementController: StockMovementController

    @MockitoBean
    private lateinit var reorderRuleController: InventoryReorderRuleController

    @MockitoBean
    private lateinit var inventoryReportsController: InventoryReportsController

    @Test
    @WithMockUser(authorities = ["inventory:read"])
    fun `products query should return json payload`() {
        `when`(productController.listProducts(null, true, null))
            .thenReturn(
                ResponseEntity.ok(
                    listOf(
                        mapOf(
                            "id" to "p1",
                            "sku" to java.util.UUID.fromString("318e4195-194f-e73c-1e71-d2d29630c151"),
                        ),
                    ),
                ),
            )

        graphQlTester
            .document(
                """
                query {
                  products
                }
                """.trimIndent(),
            ).execute()
            .path("products[0].sku")
            .entity(String::class.java)
            .isEqualTo("318e4195-194f-e73c-1e71-d2d29630c151")
    }

    @Test
    @WithMockUser(authorities = ["inventory:read"])
    fun `lowStockReport query should return json payload`() {
        `when`(inventoryReportsController.lowStock())
            .thenReturn(ResponseEntity.ok(listOf(mapOf("productId" to "p1", "shortfall" to 5))))

        graphQlTester
            .document(
                """
                query {
                  lowStockReport
                }
                """.trimIndent(),
            ).execute()
            .path("lowStockReport[0].productId")
            .entity(String::class.java)
            .isEqualTo("p1")
    }
}
