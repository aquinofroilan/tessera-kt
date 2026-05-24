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
        jdbcTemplate.update("DELETE FROM stock_on_hand WHERE organization_id = ?::uuid", orgId)
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
        val freshProductId = UUID.randomUUID().toString()
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
        val freshProductId = UUID.randomUUID().toString()
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
}
