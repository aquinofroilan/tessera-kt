package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.CreateProductRequest
import com.aquinofroilan.tessera.dto.UpdateProductRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import com.aquinofroilan.tessera.model.Currency
import com.aquinofroilan.tessera.model.Organizations
import com.aquinofroilan.tessera.model.Product
import com.aquinofroilan.tessera.model.TaxGroup
import com.aquinofroilan.tessera.repository.OrganizationRepository
import com.aquinofroilan.tessera.repository.ProductRepository
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

    private val orgId = java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d")
    private val otherOrgId = java.util.UUID.fromString("8576b8f7-dd04-3e57-b849-081b3776f223")

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
        organizationId: java.util.UUID = orgId,
        isActive: Boolean = true,
        priceCurrency: String = "USD",
    ) = Product(
        id = java.util.UUID.fromString("5bb2c240-6e9f-3a0e-8813-ed834a8c079f"),
        sku = sku,
        name = "Widget",
        description = "A test widget",
        category = "Hardware",
        imageUrl = "https://example.com/image.jpg",
        listPrice = BigDecimal("99.99"),
        priceCurrency = priceCurrency,
        taxGroupId = java.util.UUID.fromString("e8092e91-5002-31da-a72f-782e3cddd45b"),
        organizationId = organizationId,
        isActive = isActive,
    )

    private fun mockCurrency(code: String = "USD") {
        `when`(currencyService.getCurrency(code))
            .thenReturn(Currency(code = code, name = "US Dollar", symbol = "$", decimalPlaces = 2))
    }

    private fun mockOrganization(
        id: java.util.UUID = orgId,
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
        `when`(taxGroupService.getTaxGroup(java.util.UUID.fromString("e8092e91-5002-31da-a72f-782e3cddd45b"), orgId))
            .thenReturn(
                TaxGroup(
                    id = java.util.UUID.fromString("e8092e91-5002-31da-a72f-782e3cddd45b"),
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
                taxGroupId = java.util.UUID.fromString("e8092e91-5002-31da-a72f-782e3cddd45b"),
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
        `when`(productRepository.findById(java.util.UUID.fromString("5bb2c240-6e9f-3a0e-8813-ed834a8c079f"))).thenReturn(Optional.of(product))

        val result = productService.getProduct(java.util.UUID.fromString("5bb2c240-6e9f-3a0e-8813-ed834a8c079f"), orgId)

        assertThat(result.id).isEqualTo(java.util.UUID.fromString("5bb2c240-6e9f-3a0e-8813-ed834a8c079f"))
        assertThat(result.organizationId).isEqualTo(orgId)
    }

    @Test
    fun `getProduct should throw 404 when product not found`() {
        `when`(productRepository.findById(java.util.UUID.fromString("85900132-4a97-3e48-b90b-cad212e94cac"))).thenReturn(Optional.empty())

        assertThrows<ResourceNotFoundException> {
            productService.getProduct(java.util.UUID.fromString("85900132-4a97-3e48-b90b-cad212e94cac"), orgId)
        }
    }

    @Test
    fun `getProduct should throw 404 when org does not match (cross-org isolation)`() {
        val product = createMockProduct(organizationId = otherOrgId)
        `when`(productRepository.findById(java.util.UUID.fromString("5bb2c240-6e9f-3a0e-8813-ed834a8c079f"))).thenReturn(Optional.of(product))

        assertThrows<ResourceNotFoundException> {
            productService.getProduct(java.util.UUID.fromString("5bb2c240-6e9f-3a0e-8813-ed834a8c079f"), orgId)
        }
    }

    @Test
    fun `listProducts defaults to active-only with no other filters`() {
        val product1 = createMockProduct(sku = "GADGET-001")
        val product2 = createMockProduct(sku = "WIDGET-001")
        `when`(productRepository.search(orgId, true, null, null)).thenReturn(listOf(product1, product2))

        val result = productService.listProducts(orgId)

        assertThat(result.map { it.sku }).containsExactly("GADGET-001", "WIDGET-001")
    }

    @Test
    fun `listProducts passes category through to repository`() {
        val product = createMockProduct()
        `when`(productRepository.search(orgId, true, "Hardware", null)).thenReturn(listOf(product))

        val result = productService.listProducts(orgId, category = "Hardware")

        assertThat(result).hasSize(1)
        assertThat(result[0].category).isEqualTo("Hardware")
    }

    @Test
    fun `listProducts passes explicit isActive false for soft-deleted listing`() {
        val inactive = createMockProduct(sku = "OLD-001", isActive = false)
        `when`(productRepository.search(orgId, false, null, null)).thenReturn(listOf(inactive))

        val result = productService.listProducts(orgId, isActive = false)

        assertThat(result).hasSize(1)
        assertThat(result[0].isActive).isFalse()
    }

    @Test
    fun `listProducts combines category and isActive filters`() {
        val product = createMockProduct()
        `when`(productRepository.search(orgId, true, "Hardware", null)).thenReturn(listOf(product))

        val result = productService.listProducts(orgId, category = "Hardware", isActive = true)

        assertThat(result).hasSize(1)
        assertThat(result[0].category).isEqualTo("Hardware")
    }

    @Test
    fun `listProducts passes search term through to repository`() {
        val product = createMockProduct(sku = "WIDGET-001")
        `when`(productRepository.search(orgId, true, null, "widget")).thenReturn(listOf(product))

        val result = productService.listProducts(orgId, search = "widget")

        assertThat(result).hasSize(1)
        assertThat(result[0].sku).isEqualTo("WIDGET-001")
    }

    @Test
    fun `listProducts returns empty when repository search yields nothing`() {
        `when`(productRepository.search(orgId, true, null, "NOMATCH")).thenReturn(emptyList())

        val result = productService.listProducts(orgId, search = "NOMATCH")

        assertThat(result).isEmpty()
    }

    @Test
    fun `updateProduct should perform partial update`() {
        val existing = createMockProduct()
        val updated = existing.apply { name = "Updated Widget" }
        `when`(productRepository.findById(java.util.UUID.fromString("5bb2c240-6e9f-3a0e-8813-ed834a8c079f"))).thenReturn(Optional.of(existing))
        mockCurrency("USD")
        `when`(productRepository.save(any<Product>())).thenReturn(updated)

        val request = UpdateProductRequest(name = "Updated Widget")
        val result = productService.updateProduct(java.util.UUID.fromString("5bb2c240-6e9f-3a0e-8813-ed834a8c079f"), request, orgId)

        assertThat(result.name).isEqualTo("Updated Widget")
        assertThat(result.sku).isEqualTo("WIDGET-001") // unchanged
    }

    @Test
    fun `updateProduct should throw when product inactive`() {
        val existing = createMockProduct(isActive = false)
        `when`(productRepository.findById(java.util.UUID.fromString("5bb2c240-6e9f-3a0e-8813-ed834a8c079f"))).thenReturn(Optional.of(existing))

        val request = UpdateProductRequest(name = "Updated Widget")

        assertThrows<BusinessRuleException> {
            productService.updateProduct(java.util.UUID.fromString("5bb2c240-6e9f-3a0e-8813-ed834a8c079f"), request, orgId)
        }
    }

    @Test
    fun `updateProduct should validate new tax group is active`() {
        val existing = createMockProduct()
        `when`(productRepository.findById(java.util.UUID.fromString("5bb2c240-6e9f-3a0e-8813-ed834a8c079f"))).thenReturn(Optional.of(existing))
        mockCurrency("USD")
        `when`(taxGroupService.getTaxGroup(java.util.UUID.fromString("b634f85e-352e-3c6a-98de-9237abf136c3"), orgId))
            .thenReturn(
                TaxGroup(
                    id = java.util.UUID.fromString("b634f85e-352e-3c6a-98de-9237abf136c3"),
                    code = "NT",
                    name = "New",
                    taxRateIds = listOf(),
                    combinedRate = BigDecimal("5.00"),
                    organizationId = orgId,
                    isActive = false,
                ),
            )

        val request = UpdateProductRequest(taxGroupId = java.util.UUID.fromString("b634f85e-352e-3c6a-98de-9237abf136c3"))

        assertThrows<BusinessRuleException> {
            productService.updateProduct(java.util.UUID.fromString("5bb2c240-6e9f-3a0e-8813-ed834a8c079f"), request, orgId)
        }
    }

    @Test
    fun `updateProduct should allow currency change`() {
        val existing = createMockProduct(priceCurrency = "USD")
        mockCurrency("EUR")
        `when`(productRepository.findById(java.util.UUID.fromString("5bb2c240-6e9f-3a0e-8813-ed834a8c079f"))).thenReturn(Optional.of(existing))
        val updated = existing.apply { priceCurrency = "EUR" }
        `when`(productRepository.save(any<Product>())).thenReturn(updated)

        val request = UpdateProductRequest(priceCurrency = "EUR")
        val result = productService.updateProduct(java.util.UUID.fromString("5bb2c240-6e9f-3a0e-8813-ed834a8c079f"), request, orgId)

        assertThat(result.priceCurrency).isEqualTo("EUR")
    }

    @Test
    fun `deleteProduct should set isActive to false (soft delete)`() {
        val existing = createMockProduct(isActive = true)
        val deleted = createMockProduct(isActive = false)
        `when`(productRepository.findById(java.util.UUID.fromString("5bb2c240-6e9f-3a0e-8813-ed834a8c079f"))).thenReturn(Optional.of(existing))
        `when`(productRepository.save(any<Product>())).thenReturn(deleted)

        val result = productService.deleteProduct(java.util.UUID.fromString("5bb2c240-6e9f-3a0e-8813-ed834a8c079f"), orgId)

        assertThat(result.isActive).isEqualTo(false)
    }

    @Test
    fun `deleteProduct should throw when already inactive`() {
        val existing = createMockProduct(isActive = false)
        `when`(productRepository.findById(java.util.UUID.fromString("5bb2c240-6e9f-3a0e-8813-ed834a8c079f"))).thenReturn(Optional.of(existing))

        assertThrows<BusinessRuleException> {
            productService.deleteProduct(java.util.UUID.fromString("5bb2c240-6e9f-3a0e-8813-ed834a8c079f"), orgId)
        }
    }

    @Test
    fun `deleteProduct should enforce cross-org isolation`() {
        val product = createMockProduct(organizationId = otherOrgId)
        `when`(productRepository.findById(java.util.UUID.fromString("5bb2c240-6e9f-3a0e-8813-ed834a8c079f"))).thenReturn(Optional.of(product))

        assertThrows<ResourceNotFoundException> {
            productService.deleteProduct(java.util.UUID.fromString("5bb2c240-6e9f-3a0e-8813-ed834a8c079f"), orgId)
        }
    }

    @Test
    fun `createProduct trims surrounding whitespace from sku and name`() {
        mockCurrency("USD")
        `when`(productRepository.save(any<Product>())).thenAnswer { it.arguments[0] }

        val request =
            CreateProductRequest(
                sku = "  WIDGET-001  ",
                name = "  Widget  ",
                listPrice = BigDecimal("1"),
                priceCurrency = "USD",
            )

        val result = productService.createProduct(request, orgId)

        assertThat(result.sku).isEqualTo("WIDGET-001")
        assertThat(result.name).isEqualTo("Widget")
    }

    @Test
    fun `createProduct rejects blank sku after trim`() {
        mockCurrency("USD")
        val request =
            CreateProductRequest(
                sku = "   ",
                name = "Widget",
                listPrice = BigDecimal("1"),
                priceCurrency = "USD",
            )
        val ex =
            assertThrows<BusinessRuleException> {
                productService.createProduct(request, orgId)
            }
        assertThat(ex.message).contains("SKU")
    }

    @Test
    fun `createProduct rejects blank name after trim`() {
        mockCurrency("USD")
        val request =
            CreateProductRequest(
                sku = "WIDGET-001",
                name = "   ",
                listPrice = BigDecimal("1"),
                priceCurrency = "USD",
            )
        val ex =
            assertThrows<BusinessRuleException> {
                productService.createProduct(request, orgId)
            }
        assertThat(ex.message).contains("name")
    }

    @Test
    fun `updateProduct trims name when provided`() {
        val existing = createMockProduct()
        `when`(productRepository.findById(java.util.UUID.fromString("5bb2c240-6e9f-3a0e-8813-ed834a8c079f"))).thenReturn(Optional.of(existing))
        mockCurrency("USD")
        `when`(productRepository.save(any<Product>())).thenAnswer { it.arguments[0] }

        val request = UpdateProductRequest(name = "  Renamed Widget  ")
        val result = productService.updateProduct(java.util.UUID.fromString("5bb2c240-6e9f-3a0e-8813-ed834a8c079f"), request, orgId)

        assertThat(result.name).isEqualTo("Renamed Widget")
    }

    @Test
    fun `updateProduct rejects blank name`() {
        val existing = createMockProduct()
        `when`(productRepository.findById(java.util.UUID.fromString("5bb2c240-6e9f-3a0e-8813-ed834a8c079f"))).thenReturn(Optional.of(existing))

        val request = UpdateProductRequest(name = "   ")
        assertThrows<BusinessRuleException> {
            productService.updateProduct(java.util.UUID.fromString("5bb2c240-6e9f-3a0e-8813-ed834a8c079f"), request, orgId)
        }
    }
}
