package com.aquinofroilan.tessera.domain.sales.service

import com.aquinofroilan.tessera.domain.inventory.model.Product
import com.aquinofroilan.tessera.domain.inventory.repository.ProductRepository
import com.aquinofroilan.tessera.domain.sales.dto.CalculatePriceRequest
import com.aquinofroilan.tessera.domain.sales.model.Customer
import com.aquinofroilan.tessera.domain.sales.model.CustomerSegment
import com.aquinofroilan.tessera.domain.sales.model.DiscountRule
import com.aquinofroilan.tessera.domain.sales.model.DiscountType
import com.aquinofroilan.tessera.domain.sales.model.PriceList
import com.aquinofroilan.tessera.domain.sales.model.PriceListLine
import com.aquinofroilan.tessera.domain.sales.repository.CustomerRepository
import com.aquinofroilan.tessera.domain.sales.repository.DiscountRuleRepository
import com.aquinofroilan.tessera.domain.sales.repository.PriceListLineRepository
import com.aquinofroilan.tessera.domain.sales.repository.PriceListRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import java.util.Optional
import java.util.UUID

class PricingCalculationServiceTest {
    private lateinit var productRepository: ProductRepository
    private lateinit var customerRepository: CustomerRepository
    private lateinit var priceListRepository: PriceListRepository
    private lateinit var priceListLineRepository: PriceListLineRepository
    private lateinit var discountRuleRepository: DiscountRuleRepository
    private lateinit var service: PricingCalculationService

    private val orgId = UUID.fromString("4abe9f6d-6df3-6e5c-953e-3695db9a5216")
    private val productId = UUID.fromString("11111111-2222-3333-4444-555555555555")
    private val customerId = UUID.fromString("22222222-3333-4444-5555-666666666666")
    private val priceListId = UUID.fromString("33333333-4444-5555-6666-777777777777")

    @BeforeEach
    fun setUp() {
        productRepository = mock(ProductRepository::class.java)
        customerRepository = mock(CustomerRepository::class.java)
        priceListRepository = mock(PriceListRepository::class.java)
        priceListLineRepository = mock(PriceListLineRepository::class.java)
        discountRuleRepository = mock(DiscountRuleRepository::class.java)
        service =
            PricingCalculationService(
                productRepository,
                customerRepository,
                priceListRepository,
                priceListLineRepository,
                discountRuleRepository,
            )
    }

    private fun createProduct() =
        Product(
            id = productId,
            sku = "SKU-001",
            name = "Test Product",
            listPrice = BigDecimal("100.00"),
            priceCurrency = "USD",
            organizationId = orgId,
        )

    private fun createCustomer(segment: CustomerSegment = CustomerSegment.RETAIL) =
        Customer(
            id = customerId,
            name = "Acme Corp",
            organizationId = orgId,
            customerSegment = segment,
        )

    private fun createPriceList() =
        PriceList(
            id = priceListId,
            organizationId = orgId,
            name = "Wholesale USD",
            code = "PL-WHOLESALE-USD",
            currency = "USD",
            customerSegment = CustomerSegment.WHOLESALE,
        )

    @Test
    fun `calculatePrice returns catalog price when no price list or discount matches`() {
        `when`(productRepository.findById(productId)).thenReturn(Optional.of(createProduct()))
        `when`(customerRepository.findByIdAndOrganizationId(customerId, orgId)).thenReturn(Optional.of(createCustomer()))
        `when`(
            priceListRepository.findByOrganizationIdAndCurrencyAndCustomerSegmentAndIsActive(
                orgId,
                "USD",
                CustomerSegment.RETAIL,
                true,
            ),
        ).thenReturn(emptyList())
        `when`(
            priceListRepository.findByOrganizationIdAndCurrencyAndIsDefaultAndIsActive(
                orgId,
                "USD",
                true,
                true,
            ),
        ).thenReturn(Optional.empty())
        `when`(discountRuleRepository.findByOrganizationIdAndIsActiveOrderByPriorityDesc(orgId, true))
            .thenReturn(emptyList())

        val request =
            CalculatePriceRequest(
                productId = productId,
                customerId = customerId,
                quantity = BigDecimal("2.0"),
            )

        val response = service.calculatePrice(orgId, request)

        assertEquals(BigDecimal("100.00"), response.baseCatalogPrice)
        assertEquals(BigDecimal("100.0000"), response.baseUnitPrice)
        assertEquals(BigDecimal("100.0000"), response.effectiveUnitPrice)
        assertEquals(BigDecimal("200.00"), response.totalAmount)
        assertNull(response.appliedDiscountRuleCode)
    }

    @Test
    fun `calculatePrice applies price list unit price and volume discount rule`() {
        val pl = createPriceList()
        val customer = createCustomer(CustomerSegment.WHOLESALE)

        `when`(productRepository.findById(productId)).thenReturn(Optional.of(createProduct()))
        `when`(customerRepository.findByIdAndOrganizationId(customerId, orgId)).thenReturn(Optional.of(customer))
        `when`(
            priceListRepository.findByOrganizationIdAndCurrencyAndCustomerSegmentAndIsActive(
                orgId,
                "USD",
                CustomerSegment.WHOLESALE,
                true,
            ),
        ).thenReturn(listOf(pl))

        val line =
            PriceListLine(
                priceListId = priceListId,
                productId = productId,
                productSku = "SKU-001",
                unitPrice = BigDecimal("80.00"),
                minQuantity = BigDecimal("10.0"),
            )
        `when`(
            priceListLineRepository.findByPriceListIdAndProductIdAndMinQuantityLessThanEqualOrderByMinQuantityDesc(
                priceListId,
                productId,
                BigDecimal("15.0"),
            ),
        ).thenReturn(listOf(line))

        val discountRule =
            DiscountRule(
                organizationId = orgId,
                name = "Wholesale Volume 10% Off",
                code = "DISC-VOL-10",
                discountType = DiscountType.PERCENTAGE,
                discountValue = BigDecimal("10.00"),
                minQuantity = BigDecimal("10.0"),
                priority = 5,
            )
        `when`(discountRuleRepository.findByOrganizationIdAndIsActiveOrderByPriorityDesc(orgId, true))
            .thenReturn(listOf(discountRule))

        val request =
            CalculatePriceRequest(
                productId = productId,
                customerId = customerId,
                quantity = BigDecimal("15.0"),
            )

        val response = service.calculatePrice(orgId, request)

        assertEquals(priceListId, response.priceListId)
        assertEquals("Wholesale USD", response.priceListName)
        assertEquals(BigDecimal("80.0000"), response.baseUnitPrice)
        // 80 * 10% = 8.0000 discount per unit -> 72.0000 effective price
        assertEquals(BigDecimal("8.0000"), response.discountAmountPerUnit)
        assertEquals(BigDecimal("72.0000"), response.effectiveUnitPrice)
        // 72 * 15 = 1080.00
        assertEquals(BigDecimal("1080.00"), response.totalAmount)
        assertEquals("DISC-VOL-10", response.appliedDiscountRuleCode)
    }
}
