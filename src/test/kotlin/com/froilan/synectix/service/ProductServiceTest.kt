package com.froilan.synectix.service

import com.froilan.synectix.dto.CreateProductRequest
import com.froilan.synectix.dto.UpdateProductRequest
import com.froilan.synectix.exception.BusinessRuleException
import com.froilan.synectix.exception.ResourceNotFoundException
import com.froilan.synectix.model.Currency
import com.froilan.synectix.model.Organizations
import com.froilan.synectix.model.Product
import com.froilan.synectix.model.TaxGroup
import com.froilan.synectix.repository.OrganizationRepository
import com.froilan.synectix.repository.ProductRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.springframework.dao.DuplicateKeyException
import java.math.BigDecimal
import java.util.Optional

class ProductServiceTest {
    private lateinit var productService: ProductService
    private lateinit var productRepository: ProductRepository
    private lateinit var organizationRepository: OrganizationRepository
    private lateinit var currencyService: CurrencyService
    private lateinit var taxGroupService: TaxGroupService

    private val orgId = "org-123"
    private val otherOrgId = "org-456"

    @BeforeEach
    fun setup() {
        productRepository = mock(ProductRepository::class.java)
        organizationRepository = mock(OrganizationRepository::class.java)
        currencyService = mock(CurrencyService::class.java)
        taxGroupService = mock(TaxGroupService::class.java)
        productService =
            ProductService(productRepository, organizationRepository, currencyService, taxGroupService)
    }

    private fun createMockProduct(
        sku: String = "WIDGET-001",
        organizationId: String = orgId,
        isActive: Boolean = true,
        priceCurrency: String = "USD",
    ) = Product(
        id = "prod-123",
        sku = sku,
        name = "Widget",
        description = "A test widget",
        category = "Hardware",
        imageUrl = "https://example.com/image.jpg",
        listPrice = BigDecimal("99.99"),
        priceCurrency = priceCurrency,
        taxGroupId = "tax-123",
        organizationId = organizationId,
        isActive = isActive,
    )

    private fun mockCurrency(code: String = "USD") {
        `when`(currencyService.getCurrency(code))
            .thenReturn(Currency(code = code, name = "US Dollar", symbol = "$", decimalPlaces = 2))
    }

    private fun mockOrganization(
        id: String = orgId,
        baseCurrency: String = "USD",
    ) {
        `when`(organizationRepository.findById(id))
            .thenReturn(
                Optional.of(
                    Organizations(
                        uuid = id,
                        orgSlug = "test-org",
                        name = "Test Org",
                        legalName = "Test Organization LLC",
                        tradeName = "Test Org",
                        baseCurrency = baseCurrency,
                        fiscalYearStart = java.time.LocalDateTime.now(),
                        timezone = "UTC",
                    ),
                ),
            )
    }

    @Test
    fun `createProduct should save product with explicit currency`() {
        mockCurrency("USD")
        `when`(productRepository.save(any<Product>())).thenAnswer { it.arguments[0] }

        val request =
            CreateProductRequest(
                sku = "WIDGET-001",
                name = "Widget",
                listPrice = BigDecimal("99.99"),
                priceCurrency = "USD",
            )

        val result = productService.createProduct(request, orgId)

        assertThat(result.sku).isEqualTo("WIDGET-001")
        assertThat(result.priceCurrency).isEqualTo("USD")
        assertThat(result.organizationId).isEqualTo(orgId)
    }

    @Test
    fun `createProduct should default priceCurrency to org base currency`() {
        mockOrganization(baseCurrency = "EUR")
        mockCurrency("EUR")
        `when`(productRepository.save(any<Product>())).thenAnswer { it.arguments[0] }

        val request =
            CreateProductRequest(
                sku = "PRODUCT-001",
                name = "Product",
                listPrice = BigDecimal("50.00"),
            )

        val result = productService.createProduct(request, orgId)

        assertThat(result.priceCurrency).isEqualTo("EUR")
    }

    @Test
    fun `createProduct should uppercase currency code`() {
        mockCurrency("USD")
        `when`(productRepository.save(any<Product>())).thenAnswer { it.arguments[0] }

        val request =
            CreateProductRequest(
                sku = "WIDGET-001",
                name = "Widget",
                listPrice = BigDecimal("99.99"),
                priceCurrency = "usd",
            )

        val result = productService.createProduct(request, orgId)

        assertThat(result.priceCurrency).isEqualTo("USD")
    }

    @Test
    fun `createProduct should validate tax group exists and is active`() {
        mockOrganization()
        mockCurrency("USD")
        `when`(taxGroupService.getTaxGroup("tax-123", orgId))
            .thenReturn(
                TaxGroup(
                    id = "tax-123",
                    code = "TS",
                    name = "Test",
                    taxRateIds = listOf(),
                    combinedRate = BigDecimal("5.00"),
                    organizationId = orgId,
                    isActive = false,
                ),
            )

        val request =
            CreateProductRequest(
                sku = "WIDGET-001",
                name = "Widget",
                listPrice = BigDecimal("99.99"),
                taxGroupId = "tax-123",
            )

        val exception =
            assertThrows<BusinessRuleException> {
                productService.createProduct(request, orgId)
            }
        assertThat(exception.message).contains("inactive")
    }

    @Test
    fun `createProduct should throw on duplicate SKU in organization`() {
        mockOrganization()
        mockCurrency("USD")
        `when`(productRepository.save(any<Product>()))
            .thenThrow(DuplicateKeyException("duplicate key error"))

        val request =
            CreateProductRequest(
                sku = "WIDGET-001",
                name = "Widget",
                listPrice = BigDecimal("99.99"),
            )

        val exception =
            assertThrows<BusinessRuleException> {
                productService.createProduct(request, orgId)
            }
        assertThat(exception.message)
            .contains("Product with SKU 'WIDGET-001' already exists in this organization")
    }

    @Test
    fun `getProduct should return product when org matches`() {
        val product = createMockProduct()
        `when`(productRepository.findById("prod-123")).thenReturn(Optional.of(product))

        val result = productService.getProduct("prod-123", orgId)

        assertThat(result.id).isEqualTo("prod-123")
        assertThat(result.organizationId).isEqualTo(orgId)
    }

    @Test
    fun `getProduct should throw 404 when product not found`() {
        `when`(productRepository.findById("nonexistent")).thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> {
            productService.getProduct("nonexistent", orgId)
        }
    }

    @Test
    fun `getProduct should throw 404 when org does not match (cross-org isolation)`() {
        val product = createMockProduct(organizationId = otherOrgId)
        `when`(productRepository.findById("prod-123")).thenReturn(Optional.of(product))

        assertThrows<ResourceNotFoundException> {
            productService.getProduct("prod-123", orgId)
        }
    }

    @Test
    fun `listProducts should return all products for org when no filters`() {
        val product1 = createMockProduct(sku = "WIDGET-001")
        val product2 = createMockProduct(sku = "GADGET-001")
        `when`(productRepository.findByOrganizationIdAndIsActive(orgId, true)).thenReturn(listOf(product1, product2))

        val result = productService.listProducts(orgId)

        assertThat(result).hasSize(2)
        assertThat(result.map { it.sku }).containsExactly("GADGET-001", "WIDGET-001") // sorted
    }

    @Test
    fun `listProducts should filter by category`() {
        val product = createMockProduct()
        `when`(productRepository.findByOrganizationIdAndCategoryAndIsActive(orgId, "Hardware", true))
            .thenReturn(listOf(product))

        val result = productService.listProducts(orgId, category = "Hardware")

        assertThat(result).hasSize(1)
        assertThat(result[0].category).isEqualTo("Hardware")
    }

    @Test
    fun `listProducts should filter by isActive`() {
        val active = createMockProduct(sku = "ACTIVE-001", isActive = true)
        `when`(productRepository.findByOrganizationIdAndIsActive(orgId, true))
            .thenReturn(listOf(active))

        val result = productService.listProducts(orgId, isActive = true)

        assertThat(result).hasSize(1)
        assertThat(result[0].isActive).isEqualTo(true)
    }

    @Test
    fun `listProducts should filter by category and isActive`() {
        val product = createMockProduct(isActive = true)
        `when`(
            productRepository.findByOrganizationIdAndCategoryAndIsActive(
                orgId,
                "Hardware",
                true,
            ),
        ).thenReturn(listOf(product))

        val result = productService.listProducts(orgId, category = "Hardware", isActive = true)

        assertThat(result).hasSize(1)
        assertThat(result[0].category).isEqualTo("Hardware")
        assertThat(result[0].isActive).isEqualTo(true)
    }

    @Test
    fun `listProducts should search by SKU case-insensitive`() {
        val product = createMockProduct(sku = "WIDGET-001")
        `when`(productRepository.findByOrganizationIdAndIsActive(orgId, true)).thenReturn(listOf(product))

        val result = productService.listProducts(orgId, search = "widget")

        assertThat(result).hasSize(1)
        assertThat(result[0].sku).isEqualTo("WIDGET-001")
    }

    @Test
    fun `listProducts should search by name case-insensitive`() {
        val product = createMockProduct()
        `when`(productRepository.findByOrganizationIdAndIsActive(orgId, true)).thenReturn(listOf(product))

        val result = productService.listProducts(orgId, search = "WIDGET")

        assertThat(result).hasSize(1)
        assertThat(result[0].name).isEqualTo("Widget")
    }

    @Test
    fun `listProducts should return empty when search matches nothing`() {
        val product = createMockProduct()
        `when`(productRepository.findByOrganizationIdAndIsActive(orgId, true)).thenReturn(listOf(product))

        val result = productService.listProducts(orgId, search = "NOMATCH")

        assertThat(result).isEmpty()
    }

    @Test
    fun `updateProduct should perform partial update`() {
        val existing = createMockProduct()
        val updated = existing.copy(name = "Updated Widget")
        `when`(productRepository.findById("prod-123")).thenReturn(Optional.of(existing))
        mockCurrency("USD")
        `when`(productRepository.save(any<Product>())).thenReturn(updated)

        val request = UpdateProductRequest(name = "Updated Widget")
        val result = productService.updateProduct("prod-123", request, orgId)

        assertThat(result.name).isEqualTo("Updated Widget")
        assertThat(result.sku).isEqualTo("WIDGET-001") // unchanged
    }

    @Test
    fun `updateProduct should throw when product inactive`() {
        val existing = createMockProduct(isActive = false)
        `when`(productRepository.findById("prod-123")).thenReturn(Optional.of(existing))

        val request = UpdateProductRequest(name = "Updated Widget")

        assertThrows<BusinessRuleException> {
            productService.updateProduct("prod-123", request, orgId)
        }
    }

    @Test
    fun `updateProduct should validate new tax group is active`() {
        val existing = createMockProduct()
        `when`(productRepository.findById("prod-123")).thenReturn(Optional.of(existing))
        mockCurrency("USD")
        `when`(taxGroupService.getTaxGroup("new-tax", orgId))
            .thenReturn(
                TaxGroup(
                    id = "new-tax",
                    code = "NT",
                    name = "New",
                    taxRateIds = listOf(),
                    combinedRate = BigDecimal("5.00"),
                    organizationId = orgId,
                    isActive = false,
                ),
            )

        val request = UpdateProductRequest(taxGroupId = "new-tax")

        assertThrows<BusinessRuleException> {
            productService.updateProduct("prod-123", request, orgId)
        }
    }

    @Test
    fun `updateProduct should allow currency change`() {
        val existing = createMockProduct(priceCurrency = "USD")
        mockCurrency("EUR")
        `when`(productRepository.findById("prod-123")).thenReturn(Optional.of(existing))
        val updated = existing.copy(priceCurrency = "EUR")
        `when`(productRepository.save(any<Product>())).thenReturn(updated)

        val request = UpdateProductRequest(priceCurrency = "EUR")
        val result = productService.updateProduct("prod-123", request, orgId)

        assertThat(result.priceCurrency).isEqualTo("EUR")
    }

    @Test
    fun `deleteProduct should set isActive to false (soft delete)`() {
        val existing = createMockProduct(isActive = true)
        val deleted = existing.copy(isActive = false)
        `when`(productRepository.findById("prod-123")).thenReturn(Optional.of(existing))
        `when`(productRepository.save(any<Product>())).thenReturn(deleted)

        val result = productService.deleteProduct("prod-123", orgId)

        assertThat(result.isActive).isEqualTo(false)
    }

    @Test
    fun `deleteProduct should throw when already inactive`() {
        val existing = createMockProduct(isActive = false)
        `when`(productRepository.findById("prod-123")).thenReturn(Optional.of(existing))

        assertThrows<BusinessRuleException> {
            productService.deleteProduct("prod-123", orgId)
        }
    }

    @Test
    fun `deleteProduct should enforce cross-org isolation`() {
        val product = createMockProduct(organizationId = otherOrgId)
        `when`(productRepository.findById("prod-123")).thenReturn(Optional.of(product))

        assertThrows<ResourceNotFoundException> {
            productService.deleteProduct("prod-123", orgId)
        }
    }
}
