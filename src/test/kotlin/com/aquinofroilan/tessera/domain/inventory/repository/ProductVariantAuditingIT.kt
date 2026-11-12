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

    @Test
    fun `organization variant query applies tenant product and active filters`() {
        val organizationId = UUID.randomUUID()
        val otherOrganizationId = UUID.randomUUID()
        val productId = UUID.randomUUID()
        val otherProductId = UUID.randomUUID()
        val activeVariant = UUID.randomUUID()
        val inactiveVariant = UUID.randomUUID()
        val otherOrganizationVariant = UUID.randomUUID()

        insertOrganization(organizationId)
        insertOrganization(otherOrganizationId)
        insertProduct(productId, organizationId)
        insertProduct(otherProductId, otherOrganizationId)
        productVariantRepository.saveAllAndFlush(
            listOf(
                ProductVariant(
                    id = activeVariant,
                    organizationId = organizationId,
                    productId = productId,
                    code = "ACTIVE",
                    name = "Active variant",
                ),
                ProductVariant(
                    id = inactiveVariant,
                    organizationId = organizationId,
                    productId = productId,
                    code = "INACTIVE",
                    name = "Inactive variant",
                    isActive = false,
                ),
                ProductVariant(
                    id = otherOrganizationVariant,
                    organizationId = otherOrganizationId,
                    productId = otherProductId,
                    code = "OTHER-ORG",
                    name = "Other organization variant",
                ),
            ),
        )

        val organizationVariants = productVariantRepository.findScopedVariants(organizationId, null, null)
        val filteredVariants = productVariantRepository.findScopedVariants(organizationId, productId, true)

        assertThat(organizationVariants.map { it.id }).containsExactlyInAnyOrder(activeVariant, inactiveVariant)
        assertThat(filteredVariants.map { it.id }).containsExactly(activeVariant)
    }

    private fun insertOrganization(organizationId: UUID) {
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
    }

    private fun insertProduct(
        productId: UUID,
        organizationId: UUID,
    ) {
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
    }
}
