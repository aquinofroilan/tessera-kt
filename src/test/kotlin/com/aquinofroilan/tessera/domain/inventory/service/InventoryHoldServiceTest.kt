package com.aquinofroilan.tessera.domain.inventory.service

import com.aquinofroilan.tessera.domain.inventory.dto.PlaceHoldRequest
import com.aquinofroilan.tessera.domain.inventory.model.HoldStatus
import com.aquinofroilan.tessera.domain.inventory.model.Product
import com.aquinofroilan.tessera.domain.inventory.model.ProductSerial
import com.aquinofroilan.tessera.domain.inventory.model.SerialStatus
import com.aquinofroilan.tessera.domain.inventory.repository.InventoryHoldRepository
import com.aquinofroilan.tessera.domain.inventory.repository.ProductRepository
import com.aquinofroilan.tessera.domain.inventory.repository.ProductSerialRepository
import com.aquinofroilan.tessera.domain.inventory.repository.StockOnHandRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import java.math.BigDecimal
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class InventoryHoldServiceTest {
    @Mock
    private lateinit var inventoryHoldRepository: InventoryHoldRepository

    @Mock
    private lateinit var productRepository: ProductRepository

    @Mock
    private lateinit var productSerialRepository: ProductSerialRepository

    @Mock
    private lateinit var stockOnHandQueries: StockOnHandRepository

    private lateinit var service: InventoryHoldService

    private val orgId = UUID.randomUUID()
    private val userId = UUID.randomUUID()
    private val productId = UUID.randomUUID()
    private val warehouseId = UUID.randomUUID()

    @BeforeEach
    fun setup() {
        service = InventoryHoldService(inventoryHoldRepository, productRepository, productSerialRepository, stockOnHandQueries)
    }

    @Test
    fun `should place hold on non-serialized product`() {
        val product =
            Product(id = productId, organizationId = orgId, sku = "P1", name = "Prod", listPrice = BigDecimal.TEN, priceCurrency = "USD")
        `when`(productRepository.findById(productId)).thenReturn(Optional.of(product))
        `when`(stockOnHandQueries.applyHold(orgId, productId, warehouseId, "", BigDecimal.TEN)).thenReturn(true)
        `when`(inventoryHoldRepository.save(any())).thenAnswer { it.arguments[0] }

        val request = PlaceHoldRequest(productId, warehouseId, quantity = BigDecimal.TEN)
        val hold = service.placeOnHold(request, orgId, userId)

        assertEquals(HoldStatus.ACTIVE, hold.status)
        assertEquals(BigDecimal.TEN, hold.quantity)
        verify(stockOnHandQueries).applyHold(orgId, productId, warehouseId, "", BigDecimal.TEN)
    }

    @Test
    fun `should place hold on serialized product`() {
        val product =
            Product(
                id = productId,
                organizationId = orgId,
                sku = "P1",
                name = "Prod",
                listPrice = BigDecimal.TEN,
                priceCurrency = "USD",
                isSerialized = true,
            )
        val serials = listOf("SN1", "SN2")
        `when`(productRepository.findById(productId)).thenReturn(Optional.of(product))
        val s1 =
            ProductSerial(
                organizationId = orgId,
                productId = productId,
                serialNumber = "SN1",
                status = SerialStatus.IN_STOCK,
                currentWarehouseId = warehouseId,
            )
        val s2 =
            ProductSerial(
                organizationId = orgId,
                productId = productId,
                serialNumber = "SN2",
                status = SerialStatus.IN_STOCK,
                currentWarehouseId = warehouseId,
            )

        `when`(
            productSerialRepository.findByOrganizationIdAndProductIdAndSerialNumberIn(orgId, productId, serials),
        ).thenReturn(listOf(s1, s2))
        `when`(stockOnHandQueries.applyHold(orgId, productId, warehouseId, "", BigDecimal(2))).thenReturn(true)
        `when`(inventoryHoldRepository.save(any())).thenAnswer { it.arguments[0] }

        val request = PlaceHoldRequest(productId, warehouseId, serialNumbers = serials)
        val hold = service.placeOnHold(request, orgId, userId)

        assertEquals(HoldStatus.ACTIVE, hold.status)
        assertEquals(BigDecimal(2), hold.quantity)
        assertEquals(SerialStatus.QUARANTINED, s1.status)
        assertEquals(SerialStatus.QUARANTINED, s2.status)
    }
}
