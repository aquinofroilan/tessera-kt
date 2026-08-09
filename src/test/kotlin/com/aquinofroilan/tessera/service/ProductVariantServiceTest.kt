package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.CreateProductVariantRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.model.Product
import com.aquinofroilan.tessera.model.ProductVariant
import com.aquinofroilan.tessera.repository.ProductVariantRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.util.Optional

class ProductVariantServiceTest {
    private lateinit var repository: ProductVariantRepository
    private lateinit var productService: ProductService
    private lateinit var service: ProductVariantService

    private val orgId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val productId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000011")

    @BeforeEach
    fun setup() {
        repository = mock(ProductVariantRepository::class.java)
        productService = mock(ProductService::class.java)
        whenever(repository.save(any<ProductVariant>())).thenAnswer { it.arguments[0] }
        whenever(repository.findByProductIdAndCode(any(), any())).thenReturn(Optional.empty())
        whenever(productService.getProduct(productId, orgId)).thenReturn(
            Product(
                id = productId,
                sku = "P",
                name = "P",
                listPrice = BigDecimal.ONE,
                priceCurrency = "USD",
                organizationId = orgId,
                isActive = true,
            ),
        )
        service = ProductVariantService(repository, productService)
    }

    @Test
    fun `create normalises code and persists attributes`() {
        val v =
            service.createVariant(
                productId,
                CreateProductVariantRequest(
                    code = " red-l ",
                    name = "Red / Large",
                    attributes = mapOf("color" to "red", "size" to "L"),
                ),
                orgId,
            )
        assertThat(v.code).isEqualTo("RED-L")
        assertThat(v.attributes["color"]).isEqualTo("red")
    }

    @Test
    fun `create rejects duplicate code on the same product`() {
        whenever(repository.findByProductIdAndCode(productId, "RED-L")).thenReturn(
            Optional.of(
                ProductVariant(
                    organizationId = orgId,
                    productId = productId,
                    code = "RED-L",
                    name = "x",
                ),
            ),
        )
        assertThatThrownBy {
            service.createVariant(productId, CreateProductVariantRequest(code = "RED-L", name = "y"), orgId)
        }.isInstanceOf(BusinessRuleException::class.java)
    }
}
