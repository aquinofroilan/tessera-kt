package com.froilan.synectix.repository

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import java.math.BigDecimal
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest
@ActiveProfiles("test")
class StockOnHandConcurrencyIT {
    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var stockOnHandRepository: StockOnHandRepository

    private val orgId = UUID.randomUUID().toString()
    private val productId = UUID.randomUUID().toString()
    private val warehouseId = UUID.randomUUID().toString()

    @BeforeEach
    fun setup() {
        // FK chain: stock_on_hand → organizations / products / warehouses,
        // products.price_currency → currencies, organizations.base_currency → currencies.
        // CurrencySeeder seeds USD on startup, so just bootstrap the rest.
        jdbcTemplate.update(
            """
            INSERT INTO organizations
              (uuid, org_slug, name, legal_name, trade_name, base_currency,
               fiscal_year_start, timezone, status, inventory_costing_method,
               is_active, created_at)
            VALUES (?::uuid, ?, ?, ?, ?, 'USD', current_timestamp, 'UTC',
                    'ACTIVE', 'WEIGHTED_AVERAGE', true, current_timestamp)
            """.trimIndent(),
            orgId,
            "slug-$orgId",
            "Org $orgId",
            "Legal $orgId",
            "Trade $orgId",
        )
        jdbcTemplate.update(
            """
            INSERT INTO products
              (id, sku, name, list_price, price_currency, organization_id,
               is_active, created_at)
            VALUES (?::uuid, ?, ?, 0, 'USD', ?::uuid, true, current_timestamp)
            """.trimIndent(),
            productId,
            "sku-$productId",
            "Product $productId",
            orgId,
        )
        jdbcTemplate.update(
            """
            INSERT INTO warehouses
              (id, code, name, allow_negative_stock, organization_id, is_active,
               created_at)
            VALUES (?::uuid, ?, ?, false, ?::uuid, true, current_timestamp)
            """.trimIndent(),
            warehouseId,
            "code-$warehouseId",
            "Warehouse $warehouseId",
            orgId,
        )
        jdbcTemplate.update(
            """
            INSERT INTO stock_on_hand (id, organization_id, product_id, warehouse_id, quantity, created_at)
            VALUES (?::uuid, ?::uuid, ?::uuid, ?::uuid, ?, current_timestamp)
            """.trimIndent(),
            UUID.randomUUID().toString(),
            orgId,
            productId,
            warehouseId,
            BigDecimal("100"),
        )
    }

    @AfterEach
    fun cleanup() {
        jdbcTemplate.update("DELETE FROM stock_on_hand WHERE organization_id = ?::uuid", orgId)
        jdbcTemplate.update("DELETE FROM warehouses WHERE organization_id = ?::uuid", orgId)
        jdbcTemplate.update("DELETE FROM products WHERE organization_id = ?::uuid", orgId)
        jdbcTemplate.update("DELETE FROM organizations WHERE uuid = ?::uuid", orgId)
    }

    @Test
    fun `20 parallel decrements of 10 against 100 on-hand never go negative`() {
        val threads = 20
        val perThreadDelta = BigDecimal("-10")
        val executor = Executors.newFixedThreadPool(threads)
        val start = CountDownLatch(1)
        val successes = AtomicInteger(0)
        val failures = AtomicInteger(0)
        val errors = ConcurrentLinkedQueue<Throwable>()

        repeat(threads) {
            executor.submit {
                try {
                    start.await()
                    val ok =
                        stockOnHandRepository.applyDelta(
                            organizationId = orgId,
                            productId = productId,
                            warehouseId = warehouseId,
                            delta = perThreadDelta,
                            allowNegative = false,
                        )
                    if (ok) successes.incrementAndGet() else failures.incrementAndGet()
                } catch (t: Throwable) {
                    errors.add(t)
                }
            }
        }

        start.countDown()
        executor.shutdown()
        check(executor.awaitTermination(30, TimeUnit.SECONDS)) { "test timed out" }

        assertThat(errors).withFailMessage("unexpected errors: %s", errors).isEmpty()
        assertThat(successes.get()).isEqualTo(10)
        assertThat(failures.get()).isEqualTo(10)
        val finalQty = stockOnHandRepository.get(orgId, productId, warehouseId)
        assertThat(finalQty).isEqualByComparingTo("0")
    }

    @Test
    fun `applyDelta with allowNegative true accepts past zero`() {
        val ok =
            stockOnHandRepository.applyDelta(
                organizationId = orgId,
                productId = productId,
                warehouseId = warehouseId,
                delta = BigDecimal("-150"),
                allowNegative = true,
            )
        assertThat(ok).isTrue()
        assertThat(stockOnHandRepository.get(orgId, productId, warehouseId)).isEqualByComparingTo("-50")
    }

    @Test
    fun `applyDelta upserts when counter row does not yet exist`() {
        val freshProductId = insertProduct()
        val ok =
            stockOnHandRepository.applyDelta(
                organizationId = orgId,
                productId = freshProductId,
                warehouseId = warehouseId,
                delta = BigDecimal("25"),
                allowNegative = false,
            )
        assertThat(ok).isTrue()
        assertThat(stockOnHandRepository.get(orgId, freshProductId, warehouseId)).isEqualByComparingTo("25")
    }

    @Test
    fun `applyDelta rejects outbound when counter row absent`() {
        val freshProductId = insertProduct()
        val ok =
            stockOnHandRepository.applyDelta(
                organizationId = orgId,
                productId = freshProductId,
                warehouseId = warehouseId,
                delta = BigDecimal("-5"),
                allowNegative = false,
            )
        assertThat(ok).isFalse()
    }

    private fun insertProduct(): String {
        val pid = UUID.randomUUID().toString()
        jdbcTemplate.update(
            """
            INSERT INTO products
              (id, sku, name, list_price, price_currency, organization_id,
               is_active, created_at)
            VALUES (?::uuid, ?, ?, 0, 'USD', ?::uuid, true, current_timestamp)
            """.trimIndent(),
            pid,
            "sku-$pid",
            "Product $pid",
            orgId,
        )
        return pid
    }
}
