package com.aquinofroilan.tessera.domain.sales.service

import com.aquinofroilan.tessera.domain.inventory.model.Product
import com.aquinofroilan.tessera.domain.inventory.repository.ProductRepository
import com.aquinofroilan.tessera.domain.sales.dto.CreatePriceListLineRequest
import com.aquinofroilan.tessera.domain.sales.dto.CreatePriceListRequest
import com.aquinofroilan.tessera.domain.sales.dto.UpdatePriceListRequest
import com.aquinofroilan.tessera.domain.sales.model.CustomerSegment
import com.aquinofroilan.tessera.domain.sales.model.PriceList
import com.aquinofroilan.tessera.domain.sales.repository.PriceListLineRepository
import com.aquinofroilan.tessera.domain.sales.repository.PriceListRepository
import com.aquinofroilan.tessera.exception.BusinessRuleException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import java.math.BigDecimal
import java.util.Optional
import java.util.UUID

class PriceListServiceTest {
    private lateinit var priceListRepository: PriceListRepository
    private lateinit var priceListLineRepository: PriceListLineRepository
    private lateinit var productRepository: ProductRepository
    private lateinit var service: PriceListService

    private val orgId = UUID.fromString("4abe9f6d-6df3-6e5c-953e-3695db9a5216")
    private val productId = UUID.fromString("11111111-2222-3333-4444-555555555555")
    private val priceListId = UUID.fromString("22222222-3333-4444-5555-666666666666")

    @BeforeEach
    fun setUp() {
        priceListRepository = mock(PriceListRepository::class.java)
        priceListLineRepository = mock(PriceListLineRepository::class.java)
        productRepository = mock(ProductRepository::class.java)
        service = PriceListService(priceListRepository, priceListLineRepository, productRepository)
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

    private fun createPriceList() =
        PriceList(
            id = priceListId,
            organizationId = orgId,
            name = "Wholesale USD",
            code = "PL-WHOLESALE-USD",
            currency = "USD",
            customerSegment = CustomerSegment.WHOLESALE,
            isDefault = true,
        )

    @Test
    fun `createPriceList creates price list with lines successfully`() {
        val request =
            CreatePriceListRequest(
                name = "Wholesale USD",
                code = "PL-WHOLESALE-USD",
                currency = "USD",
                customerSegment = CustomerSegment.WHOLESALE,
                isDefault = true,
                lines =
                    listOf(
                        CreatePriceListLineRequest(
                            productId = productId,
                            unitPrice = BigDecimal("85.00"),
                            minQuantity = BigDecimal("10.0"),
                        ),
                    ),
            )

        `when`(priceListRepository.existsByOrganizationIdAndCode(orgId, "PL-WHOLESALE-USD")).thenReturn(false)
        `when`(productRepository.findById(productId)).thenReturn(Optional.of(createProduct()))
        `when`(priceListRepository.save(any<PriceList>())).thenAnswer { it.arguments[0] }

        val response = service.createPriceList(orgId, request)

        assertEquals("Wholesale USD", response.name)
        assertEquals("PL-WHOLESALE-USD", response.code)
        assertEquals("USD", response.currency)
        assertEquals(CustomerSegment.WHOLESALE, response.customerSegment)
        assertTrue(response.isDefault)
        assertEquals(1, response.lines.size)
        assertEquals(BigDecimal("85.00"), response.lines[0].unitPrice)
        assertEquals(BigDecimal("10.0"), response.lines[0].minQuantity)
    }

    @Test
    fun `createPriceList throws BusinessRuleException on duplicate code`() {
        val request =
            CreatePriceListRequest(
                name = "Wholesale USD",
                code = "PL-WHOLESALE-USD",
                currency = "USD",
            )

        `when`(priceListRepository.existsByOrganizationIdAndCode(orgId, "PL-WHOLESALE-USD")).thenReturn(true)

        assertThrows<BusinessRuleException> {
            service.createPriceList(orgId, request)
        }
    }

    @Test
    fun `addOrUpdateLine updates existing line or creates new line`() {
        val pl = createPriceList()
        `when`(priceListRepository.findByIdAndOrganizationId(priceListId, orgId)).thenReturn(Optional.of(pl))
        `when`(productRepository.findById(productId)).thenReturn(Optional.of(createProduct()))
        `when`(
            priceListLineRepository.findByPriceListIdAndProductIdAndMinQuantity(
                priceListId,
                productId,
                BigDecimal("5.0"),
            ),
        ).thenReturn(Optional.empty())
        `when`(priceListRepository.save(any<PriceList>())).thenAnswer { it.arguments[0] }

        val lineReq =
            CreatePriceListLineRequest(
                productId = productId,
                unitPrice = BigDecimal("90.00"),
                minQuantity = BigDecimal("5.0"),
            )

        val lineDto = service.addOrUpdateLine(priceListId, orgId, lineReq)

        assertNotNull(lineDto.id)
        assertEquals(productId, lineDto.productId)
        assertEquals(BigDecimal("90.00"), lineDto.unitPrice)
        assertEquals(BigDecimal("5.0"), lineDto.minQuantity)
    }

    @Test
    fun `updatePriceList modifies attributes`() {
        val pl = createPriceList()
        `when`(priceListRepository.findByIdAndOrganizationId(priceListId, orgId)).thenReturn(Optional.of(pl))
        `when`(priceListRepository.save(any<PriceList>())).thenAnswer { it.arguments[0] }

        val updateReq =
            UpdatePriceListRequest(
                name = "Updated Wholesale",
                customerSegment = CustomerSegment.DISTRIBUTOR,
                isActive = false,
            )

        val response = service.updatePriceList(priceListId, orgId, updateReq)

        assertEquals("Updated Wholesale", response.name)
        assertEquals(CustomerSegment.DISTRIBUTOR, response.customerSegment)
        assertFalse(response.isActive)
    }
}
