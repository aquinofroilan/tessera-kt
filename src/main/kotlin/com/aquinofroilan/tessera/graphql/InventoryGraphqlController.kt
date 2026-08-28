package com.aquinofroilan.tessera.graphql

import com.aquinofroilan.tessera.domain.finance.controller.CurrencyController
import com.aquinofroilan.tessera.domain.finance.controller.ExchangeRateController
import com.aquinofroilan.tessera.domain.inventory.controller.InventoryReorderRuleController
import com.aquinofroilan.tessera.domain.inventory.controller.InventoryReportsController
import com.aquinofroilan.tessera.domain.inventory.controller.ProductController
import com.aquinofroilan.tessera.domain.inventory.controller.StockMovementController
import com.aquinofroilan.tessera.domain.inventory.controller.WarehouseController
import com.aquinofroilan.tessera.domain.inventory.dto.CreateProductRequest
import com.aquinofroilan.tessera.domain.inventory.dto.CreateReorderRuleRequest
import com.aquinofroilan.tessera.domain.inventory.dto.CreateStockMovementRequest
import com.aquinofroilan.tessera.domain.inventory.dto.CreateWarehouseRequest
import com.aquinofroilan.tessera.domain.inventory.dto.UpdateProductRequest
import com.aquinofroilan.tessera.domain.inventory.dto.UpdateReorderRuleRequest
import com.aquinofroilan.tessera.domain.inventory.dto.UpdateWarehouseRequest
import com.aquinofroilan.tessera.domain.inventory.model.StockMovementType
import com.aquinofroilan.tessera.domain.platform.dto.CreateExchangeRateRequest
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

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
    ): Any = support.unwrap(exchangeRateController.listRates(support.orgId(), from, to, asOfDate?.let(LocalDate::parse)))

    @MutationMapping
    @PreAuthorize("hasAuthority('fx:create')")
    fun createExchangeRate(
        @Argument input: Any,
    ): Any = support.unwrap(exchangeRateController.createRate(support.orgId(), support.toRequest<CreateExchangeRateRequest>(input)))

    @MutationMapping
    @PreAuthorize("hasAuthority('fx:create')")
    fun deleteExchangeRate(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(exchangeRateController.deleteRate(support.orgId(), id))

    @MutationMapping
    @PreAuthorize("hasAuthority('inventory:write')")
    fun createProduct(
        @Argument input: Any,
    ): Any = support.unwrap(productController.createProduct(support.orgId(), support.toRequest<CreateProductRequest>(input)))

    @QueryMapping
    @PreAuthorize("hasAuthority('inventory:read')")
    fun products(
        @Argument category: String?,
        @Argument isActive: Boolean?,
        @Argument search: String?,
    ): Any = support.unwrap(productController.listProducts(support.orgId(), category, isActive ?: true, search))

    @QueryMapping
    @PreAuthorize("hasAuthority('inventory:read')")
    fun product(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(productController.getProduct(support.orgId(), id))

    @MutationMapping
    @PreAuthorize("hasAuthority('inventory:write')")
    fun updateProduct(
        @Argument id: java.util.UUID,
        @Argument input: Any,
    ): Any = support.unwrap(productController.updateProduct(support.orgId(), id, support.toRequest<UpdateProductRequest>(input)))

    @MutationMapping
    @PreAuthorize("hasAuthority('inventory:write')")
    fun deleteProduct(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(productController.deleteProduct(support.orgId(), id))

    @MutationMapping
    @PreAuthorize("hasAuthority('inventory:write')")
    fun createWarehouse(
        @Argument input: Any,
    ): Any = support.unwrap(warehouseController.createWarehouse(support.orgId(), support.toRequest<CreateWarehouseRequest>(input)))

    @QueryMapping
    @PreAuthorize("hasAuthority('inventory:read')")
    fun warehouses(
        @Argument isActive: Boolean?,
        @Argument search: String?,
    ): Any = support.unwrap(warehouseController.listWarehouses(support.orgId(), isActive ?: true, search))

    @QueryMapping
    @PreAuthorize("hasAuthority('inventory:read')")
    fun warehouse(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(warehouseController.getWarehouse(support.orgId(), id))

    @MutationMapping
    @PreAuthorize("hasAuthority('inventory:write')")
    fun updateWarehouse(
        @Argument id: java.util.UUID,
        @Argument input: Any,
    ): Any = support.unwrap(warehouseController.updateWarehouse(support.orgId(), id, support.toRequest<UpdateWarehouseRequest>(input)))

    @MutationMapping
    @PreAuthorize("hasAuthority('inventory:write')")
    fun deleteWarehouse(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(warehouseController.deleteWarehouse(support.orgId(), id))

    @MutationMapping
    @PreAuthorize("hasAuthority('inventory:write')")
    fun createStockMovement(
        @Argument input: Any,
    ): Any =
        support.unwrap(
            stockMovementController.createMovement(support.userId(), support.orgId(), support.toRequest<CreateStockMovementRequest>(input)),
        )

    @QueryMapping
    @PreAuthorize("hasAuthority('inventory:read')")
    fun stockMovements(
        @Argument productId: java.util.UUID?,
        @Argument warehouseId: java.util.UUID?,
        @Argument type: String?,
        @Argument from: String?,
        @Argument to: String?,
    ): Any =
        support.unwrap(
            stockMovementController.listMovements(
                support.orgId(),
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
        @Argument productId: java.util.UUID,
        @Argument warehouseId: java.util.UUID,
    ): Any = support.unwrap(stockMovementController.onHand(support.orgId(), productId, warehouseId))

    @MutationMapping
    @PreAuthorize("hasAuthority('inventory:write')")
    fun createReorderRule(
        @Argument input: Any,
    ): Any = support.unwrap(reorderRuleController.createRule(support.orgId(), support.toRequest<CreateReorderRuleRequest>(input)))

    @QueryMapping
    @PreAuthorize("hasAuthority('inventory:read')")
    fun reorderRules(): Any = support.unwrap(reorderRuleController.listRules(support.orgId()))

    @QueryMapping
    @PreAuthorize("hasAuthority('inventory:read')")
    fun reorderRule(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(reorderRuleController.getRule(support.orgId(), id))

    @MutationMapping
    @PreAuthorize("hasAuthority('inventory:write')")
    fun updateReorderRule(
        @Argument id: java.util.UUID,
        @Argument input: Any,
    ): Any = support.unwrap(reorderRuleController.updateRule(support.orgId(), id, support.toRequest<UpdateReorderRuleRequest>(input)))

    @MutationMapping
    @PreAuthorize("hasAuthority('inventory:write')")
    fun deleteReorderRule(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(reorderRuleController.deleteRule(support.orgId(), id))

    @QueryMapping
    @PreAuthorize("hasAuthority('inventory:read')")
    fun inventoryValuation(): Any = support.unwrap(inventoryReportsController.valuation(support.orgId()))

    @QueryMapping
    @PreAuthorize("hasAuthority('inventory:read')")
    fun inventoryStockOnHand(
        @Argument productId: java.util.UUID?,
        @Argument warehouseId: java.util.UUID?,
        @Argument asOfDate: String?,
    ): Any =
        support.unwrap(
            inventoryReportsController.stockOnHand(
                support.orgId(),
                productId,
                warehouseId,
                asOfDate?.let(LocalDateTime::parse),
            ),
        )

    @QueryMapping
    @PreAuthorize("hasAuthority('inventory:read')")
    fun inventoryMovementHistory(
        @Argument productId: java.util.UUID?,
        @Argument warehouseId: java.util.UUID?,
        @Argument from: String?,
        @Argument to: String?,
    ): Any =
        support.unwrap(
            inventoryReportsController.movementHistory(
                support.orgId(),
                productId,
                warehouseId,
                from?.let(LocalDateTime::parse),
                to?.let(LocalDateTime::parse),
            ),
        )

    @QueryMapping
    @PreAuthorize("hasAuthority('inventory:read')")
    fun lowStockReport(): Any = support.unwrap(inventoryReportsController.lowStock(support.orgId()))
}
