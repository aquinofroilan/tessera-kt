package com.froilan.synectix.graphql

import com.froilan.synectix.controller.CurrencyController
import com.froilan.synectix.controller.ExchangeRateController
import com.froilan.synectix.controller.InventoryReorderRuleController
import com.froilan.synectix.controller.InventoryReportsController
import com.froilan.synectix.controller.ProductController
import com.froilan.synectix.controller.StockMovementController
import com.froilan.synectix.controller.WarehouseController
import com.froilan.synectix.dto.CreateExchangeRateRequest
import com.froilan.synectix.dto.CreateProductRequest
import com.froilan.synectix.dto.CreateReorderRuleRequest
import com.froilan.synectix.dto.CreateStockMovementRequest
import com.froilan.synectix.dto.CreateWarehouseRequest
import com.froilan.synectix.dto.UpdateProductRequest
import com.froilan.synectix.dto.UpdateReorderRuleRequest
import com.froilan.synectix.dto.UpdateWarehouseRequest
import com.froilan.synectix.exception.AuthenticationException
import com.froilan.synectix.exception.ResourceNotFoundException
import com.froilan.synectix.model.StockMovementType
import jakarta.validation.Validator
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import tools.jackson.databind.ObjectMapper
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
    private val objectMapper: ObjectMapper,
    private val validator: Validator,
) {
    @QueryMapping
    @PreAuthorize("hasAuthority('fx:read')")
    fun currencies(): Any = unwrap(currencyController.listCurrencies())

    @QueryMapping
    @PreAuthorize("hasAuthority('fx:read')")
    fun exchangeRates(
        @Argument from: String?,
        @Argument to: String?,
        @Argument asOfDate: String?,
    ): Any = unwrap(exchangeRateController.listRates(from, to, asOfDate?.let(LocalDate::parse)))

    @MutationMapping
    @PreAuthorize("hasAuthority('fx:create')")
    fun createExchangeRate(
        @Argument input: Any,
    ): Any = unwrap(exchangeRateController.createRate(toRequest<CreateExchangeRateRequest>(input)))

    @MutationMapping
    @PreAuthorize("hasAuthority('fx:create')")
    fun deleteExchangeRate(
        @Argument id: String,
    ): Any = unwrap(exchangeRateController.deleteRate(id))

    @MutationMapping
    @PreAuthorize("hasAuthority('inventory:write')")
    fun createProduct(
        @Argument input: Any,
    ): Any = unwrap(productController.createProduct(toRequest<CreateProductRequest>(input)))

    @QueryMapping
    @PreAuthorize("hasAuthority('inventory:read')")
    fun products(
        @Argument category: String?,
        @Argument isActive: Boolean?,
        @Argument search: String?,
    ): Any = unwrap(productController.listProducts(category, isActive ?: true, search))

    @QueryMapping
    @PreAuthorize("hasAuthority('inventory:read')")
    fun product(
        @Argument id: String,
    ): Any = unwrap(productController.getProduct(id))

    @MutationMapping
    @PreAuthorize("hasAuthority('inventory:write')")
    fun updateProduct(
        @Argument id: String,
        @Argument input: Any,
    ): Any = unwrap(productController.updateProduct(id, toRequest<UpdateProductRequest>(input)))

    @MutationMapping
    @PreAuthorize("hasAuthority('inventory:write')")
    fun deleteProduct(
        @Argument id: String,
    ): Any = unwrap(productController.deleteProduct(id))

    @MutationMapping
    @PreAuthorize("hasAuthority('inventory:write')")
    fun createWarehouse(
        @Argument input: Any,
    ): Any = unwrap(warehouseController.createWarehouse(toRequest<CreateWarehouseRequest>(input)))

    @QueryMapping
    @PreAuthorize("hasAuthority('inventory:read')")
    fun warehouses(
        @Argument isActive: Boolean?,
        @Argument search: String?,
    ): Any = unwrap(warehouseController.listWarehouses(isActive ?: true, search))

    @QueryMapping
    @PreAuthorize("hasAuthority('inventory:read')")
    fun warehouse(
        @Argument id: String,
    ): Any = unwrap(warehouseController.getWarehouse(id))

    @MutationMapping
    @PreAuthorize("hasAuthority('inventory:write')")
    fun updateWarehouse(
        @Argument id: String,
        @Argument input: Any,
    ): Any = unwrap(warehouseController.updateWarehouse(id, toRequest<UpdateWarehouseRequest>(input)))

    @MutationMapping
    @PreAuthorize("hasAuthority('inventory:write')")
    fun deleteWarehouse(
        @Argument id: String,
    ): Any = unwrap(warehouseController.deleteWarehouse(id))

    @MutationMapping
    @PreAuthorize("hasAuthority('inventory:write')")
    fun createStockMovement(
        @Argument input: Any,
    ): Any = unwrap(stockMovementController.createMovement(toRequest<CreateStockMovementRequest>(input)))

    @QueryMapping
    @PreAuthorize("hasAuthority('inventory:read')")
    fun stockMovements(
        @Argument productId: String?,
        @Argument warehouseId: String?,
        @Argument type: String?,
        @Argument from: String?,
        @Argument to: String?,
    ): Any =
        unwrap(
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
    ): Any = unwrap(stockMovementController.onHand(productId, warehouseId))

    @MutationMapping
    @PreAuthorize("hasAuthority('inventory:write')")
    fun createReorderRule(
        @Argument input: Any,
    ): Any = unwrap(reorderRuleController.createRule(toRequest<CreateReorderRuleRequest>(input)))

    @QueryMapping
    @PreAuthorize("hasAuthority('inventory:read')")
    fun reorderRules(): Any = unwrap(reorderRuleController.listRules())

    @QueryMapping
    @PreAuthorize("hasAuthority('inventory:read')")
    fun reorderRule(
        @Argument id: String,
    ): Any = unwrap(reorderRuleController.getRule(id))

    @MutationMapping
    @PreAuthorize("hasAuthority('inventory:write')")
    fun updateReorderRule(
        @Argument id: String,
        @Argument input: Any,
    ): Any = unwrap(reorderRuleController.updateRule(id, toRequest<UpdateReorderRuleRequest>(input)))

    @MutationMapping
    @PreAuthorize("hasAuthority('inventory:write')")
    fun deleteReorderRule(
        @Argument id: String,
    ): Any = unwrap(reorderRuleController.deleteRule(id))

    @QueryMapping
    @PreAuthorize("hasAuthority('inventory:read')")
    fun inventoryValuation(): Any = unwrap(inventoryReportsController.valuation())

    @QueryMapping
    @PreAuthorize("hasAuthority('inventory:read')")
    fun inventoryStockOnHand(
        @Argument productId: String?,
        @Argument warehouseId: String?,
        @Argument asOfDate: String?,
    ): Any =
        unwrap(
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
        unwrap(
            inventoryReportsController.movementHistory(
                productId,
                warehouseId,
                from?.let(LocalDateTime::parse),
                to?.let(LocalDateTime::parse),
            ),
        )

    @QueryMapping
    @PreAuthorize("hasAuthority('inventory:read')")
    fun lowStockReport(): Any = unwrap(inventoryReportsController.lowStock())

    private inline fun <reified T : Any> toRequest(input: Any): T {
        val request = objectMapper.convertValue(input, T::class.java)
        val violations = validator.validate(request)
        if (violations.isNotEmpty()) {
            val message = violations.joinToString(", ") { it.message }
            throw IllegalArgumentException(message)
        }
        return request
    }

    private fun unwrap(response: ResponseEntity<*>): Any {
        if (response.statusCode.is2xxSuccessful) {
            return response.body ?: emptyMap<String, Any>()
        }

        val message =
            when (val body = response.body) {
                is Map<*, *> -> body["error"]?.toString() ?: body["message"]?.toString()
                else -> body?.toString()
            } ?: "Request failed with status ${response.statusCode.value()}"

        when (response.statusCode.value()) {
            401 -> throw AuthenticationException(message)
            404 -> throw ResourceNotFoundException(message)
            422 -> throw IllegalStateException(message)
            else -> throw IllegalArgumentException(message)
        }
    }
}
