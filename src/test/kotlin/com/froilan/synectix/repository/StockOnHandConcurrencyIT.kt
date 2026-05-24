package com.froilan.synectix.repository

import com.froilan.synectix.model.StockOnHand
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Query
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
    private lateinit var mongoTemplate: MongoTemplate

    @Autowired
    private lateinit var stockOnHandRepository: StockOnHandRepository

    private val orgId = "org-" + UUID.randomUUID()
    private val productId = "prod-" + UUID.randomUUID()
    private val warehouseId = "wh-" + UUID.randomUUID()

    @BeforeEach
    fun setup() {
        mongoTemplate.remove(Query(), StockOnHand::class.java)
        mongoTemplate.save(
            StockOnHand(
                organizationId = orgId,
                productId = productId,
                warehouseId = warehouseId,
                quantity = BigDecimal("100"),
            ),
        )
    }

    @AfterEach
    fun cleanup() {
        mongoTemplate.remove(Query(), StockOnHand::class.java)
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
    fun `applyDelta upserts when counter doc does not yet exist`() {
        val freshProductId = "fresh-" + UUID.randomUUID()
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
    fun `applyDelta rejects outbound when counter doc absent`() {
        val freshProductId = "fresh-" + UUID.randomUUID()
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
