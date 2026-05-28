package com.loom.synectix.graphql

import com.loom.synectix.controller.CurrencyController
import com.loom.synectix.controller.ExchangeRateController
import com.loom.synectix.controller.InventoryReorderRuleController
import com.loom.synectix.controller.InventoryReportsController
import com.loom.synectix.controller.ProductController
import com.loom.synectix.controller.StockMovementController
import com.loom.synectix.controller.WarehouseController
import com.loom.synectix.dto.CreateExchangeRateRequest
import com.loom.synectix.dto.CreateProductRequest
import com.loom.synectix.dto.CreateReorderRuleRequest
import com.loom.synectix.dto.CreateStockMovementRequest
import com.loom.synectix.dto.CreateWarehouseRequest
import com.loom.synectix.dto.UpdateProductRequest
import com.loom.synectix.dto.UpdateReorderRuleRequest
import com.loom.synectix.dto.UpdateWarehouseRequest
import com.loom.synectix.model.StockMovementType
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import java.time.LocalDate
import java.time.LocalDateTime

@Controller
class InventoryGraphqlController(
    private val currencyController: CurrencyController,
    private val exchangeRateController: ExchangeRateController,
    private val productController: ProductController,
    private val warehouseController: WarehouseController,
    private val stockMovementController: StockMovementController,
    private val reorderRuleController: InventoryReorderRuleController,
    private val inventoryReportsController: InventoryReportsController,
    private val support: GraphqlBridgeSupport,
) {
    @QueryMapping
    @PreAuthorize("hasAuthority('fx:read')")
    fun currencies(): Any = support.unwrap(currencyController.listCurrencies())

    @QueryMapping
    @PreAuthorize("hasAuthority('fx:read')")
    fun exchangeRates(
        @Argument from: String?,
        @Argument to: String?,
        @Argument asOfDate: String?,
    ): Any = support.unwrap(exchangeRateController.listRates(from, to, asOfDate?.let(LocalDate::parse)))

    @MutationMapping
    @PreAuthorize("hasAuthority('fx:create')")
    fun createExchangeRate(
        @Argument input: Any,
    ): Any = support.unwrap(exchangeRateController.createRate(support.toRequest<CreateExchangeRateRequest>(input)))

    @MutationMapping
    @PreAuthorize("hasAuthority('fx:create')")
    fun deleteExchangeRate(
        @Argument id: String,
    ): Any = support.unwrap(exchangeRateController.deleteRate(id))

    @MutationMapping
    @PreAuthorize("hasAuthority('inventory:write')")
    fun createProduct(
        @Argument input: Any,
    ): Any = support.unwrap(productController.createProduct(support.toRequest<CreateProductRequest>(input)))

    @QueryMapping
    @PreAuthorize("hasAuthority('inventory:read')")
    fun products(
        @Argument category: String?,
        @Argument isActive: Boolean?,
        @Argument search: String?,
    ): Any = support.unwrap(productController.listProducts(category, isActive ?: true, search))

    @QueryMapping
    @PreAuthorize("hasAuthority('inventory:read')")
    fun product(
        @Argument id: String,
    ): Any = support.unwrap(productController.getProduct(id))

    @MutationMapping
    @PreAuthorize("hasAuthority('inventory:write')")
    fun updateProduct(
        @Argument id: String,
        @Argument input: Any,
    ): Any = support.unwrap(productController.updateProduct(id, support.toRequest<UpdateProductRequest>(input)))

    @MutationMapping
    @PreAuthorize("hasAuthority('inventory:write')")
    fun deleteProduct(
        @Argument id: String,
    ): Any = support.unwrap(productController.deleteProduct(id))

    @MutationMapping
    @PreAuthorize("hasAuthority('inventory:write')")
    fun createWarehouse(
        @Argument input: Any,
    ): Any = support.unwrap(warehouseController.createWarehouse(support.toRequest<CreateWarehouseRequest>(input)))

    @QueryMapping
    @PreAuthorize("hasAuthority('inventory:read')")
    fun warehouses(
        @Argument isActive: Boolean?,
        @Argument search: String?,
    ): Any = support.unwrap(warehouseController.listWarehouses(isActive ?: true, search))

    @QueryMapping
    @PreAuthorize("hasAuthority('inventory:read')")
    fun warehouse(
        @Argument id: String,
    ): Any = support.unwrap(warehouseController.getWarehouse(id))

    @MutationMapping
    @PreAuthorize("hasAuthority('inventory:write')")
    fun updateWarehouse(
        @Argument id: String,
        @Argument input: Any,
    ): Any = support.unwrap(warehouseController.updateWarehouse(id, support.toRequest<UpdateWarehouseRequest>(input)))

    @MutationMapping
    @PreAuthorize("hasAuthority('inventory:write')")
    fun deleteWarehouse(
        @Argument id: String,
    ): Any = support.unwrap(warehouseController.deleteWarehouse(id))

    @MutationMapping
    @PreAuthorize("hasAuthority('inventory:write')")
    fun createStockMovement(
        @Argument input: Any,
    ): Any = support.unwrap(stockMovementController.createMovement(support.toRequest<CreateStockMovementRequest>(input)))

    @QueryMapping
    @PreAuthorize("hasAuthority('inventory:read')")
    fun stockMovements(
        @Argument productId: String?,
        @Argument warehouseId: String?,
        @Argument type: String?,
        @Argument from: String?,
        @Argument to: String?,
    ): Any =
        support.unwrap(
            stockMovementController.listMovements(
                productId,
                warehouseId,
                type?.let { StockMovementType.valueOf(it) },
                from?.let(LocalDateTime::parse),
                to?.let(LocalDateTime::parse),
            ),
        )

    @QueryMapping
    @PreAuthorize("hasAuthority('inventory:read')")
    fun stockOnHand(
        @Argument productId: String,
        @Argument warehouseId: String,
    ): Any = support.unwrap(stockMovementController.onHand(productId, warehouseId))

    @MutationMapping
    @PreAuthorize("hasAuthority('inventory:write')")
    fun createReorderRule(
        @Argument input: Any,
    ): Any = support.unwrap(reorderRuleController.createRule(support.toRequest<CreateReorderRuleRequest>(input)))

    @QueryMapping
    @PreAuthorize("hasAuthority('inventory:read')")
    fun reorderRules(): Any = support.unwrap(reorderRuleController.listRules())

    @QueryMapping
    @PreAuthorize("hasAuthority('inventory:read')")
    fun reorderRule(
        @Argument id: String,
    ): Any = support.unwrap(reorderRuleController.getRule(id))

    @MutationMapping
    @PreAuthorize("hasAuthority('inventory:write')")
    fun updateReorderRule(
        @Argument id: String,
        @Argument input: Any,
    ): Any = support.unwrap(reorderRuleController.updateRule(id, support.toRequest<UpdateReorderRuleRequest>(input)))

    @MutationMapping
    @PreAuthorize("hasAuthority('inventory:write')")
    fun deleteReorderRule(
        @Argument id: String,
    ): Any = support.unwrap(reorderRuleController.deleteRule(id))

    @QueryMapping
    @PreAuthorize("hasAuthority('inventory:read')")
    fun inventoryValuation(): Any = support.unwrap(inventoryReportsController.valuation())

    @QueryMapping
    @PreAuthorize("hasAuthority('inventory:read')")
    fun inventoryStockOnHand(
        @Argument productId: String?,
        @Argument warehouseId: String?,
        @Argument asOfDate: String?,
    ): Any =
        support.unwrap(
            inventoryReportsController.stockOnHand(
                productId,
                warehouseId,
                asOfDate?.let(LocalDateTime::parse),
            ),
        )

    @QueryMapping
    @PreAuthorize("hasAuthority('inventory:read')")
    fun inventoryMovementHistory(
        @Argument productId: String?,
        @Argument warehouseId: String?,
        @Argument from: String?,
        @Argument to: String?,
    ): Any =
        support.unwrap(
            inventoryReportsController.movementHistory(
                productId,
                warehouseId,
                from?.let(LocalDateTime::parse),
                to?.let(LocalDateTime::parse),
            ),
        )

    @QueryMapping
    @PreAuthorize("hasAuthority('inventory:read')")
    fun lowStockReport(): Any = support.unwrap(inventoryReportsController.lowStock())
}
