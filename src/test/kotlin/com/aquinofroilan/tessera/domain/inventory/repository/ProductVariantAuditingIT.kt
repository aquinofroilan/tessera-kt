package com.aquinofroilan.tessera.domain.inventory.repository

import com.aquinofroilan.tessera.domain.inventory.model.ProductVariant
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProductVariantAuditingIT {
    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var productVariantRepository: ProductVariantRepository

    @Test
    fun `creating a variant populates its created timestamp`() {
        val organizationId = UUID.randomUUID()
        val productId = UUID.randomUUID()
        val variantId = UUID.randomUUID()

        jdbcTemplate.update(
            """
            INSERT INTO organizations
              (uuid, org_slug, name, legal_name, trade_name, base_currency,
               fiscal_year_start, timezone, status, inventory_costing_method,
               is_active, created_at)
            VALUES (?, ?, ?, ?, ?, 'USD', current_timestamp, 'UTC',
                    'ACTIVE', 'WEIGHTED_AVERAGE', true, current_timestamp)
            """.trimIndent(),
            organizationId,
            "slug-$organizationId",
            "Org $organizationId",
            "Legal $organizationId",
            "Trade $organizationId",
        )
        jdbcTemplate.update(
            """
            INSERT INTO products
              (id, sku, name, list_price, price_currency, organization_id,
               is_active, created_at)
            VALUES (?, ?, ?, 0, 'USD', ?, true, current_timestamp)
            """.trimIndent(),
            productId,
            "sku-$productId",
            "Product $productId",
            organizationId,
        )

        productVariantRepository.saveAndFlush(
            ProductVariant(
                id = variantId,
                organizationId = organizationId,
                productId = productId,
                code = "TEST-VARIANT",
                name = "Test variant",
            ),
        )

        val persistedTimestampCount =
            jdbcTemplate.queryForObject(
                "SELECT count(*) FROM inventory_product_variants WHERE id = ? AND created_at IS NOT NULL",
                Long::class.java,
                variantId,
            )

        assertThat(persistedTimestampCount).isEqualTo(1L)
    }
}
