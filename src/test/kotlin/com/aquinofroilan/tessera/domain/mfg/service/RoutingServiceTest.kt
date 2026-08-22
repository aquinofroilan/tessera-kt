package com.aquinofroilan.tessera.domain.mfg.service

import com.aquinofroilan.tessera.domain.inventory.model.Product
import com.aquinofroilan.tessera.domain.inventory.service.ProductService
import com.aquinofroilan.tessera.domain.mfg.dto.CreateRoutingOperationRequest
import com.aquinofroilan.tessera.domain.mfg.dto.CreateRoutingRequest
import com.aquinofroilan.tessera.domain.mfg.dto.UpdateRoutingRequest
import com.aquinofroilan.tessera.domain.mfg.model.Routing
import com.aquinofroilan.tessera.domain.mfg.model.RoutingStatus
import com.aquinofroilan.tessera.domain.mfg.model.WorkCenter
import com.aquinofroilan.tessera.domain.mfg.repository.RoutingRepository
import com.aquinofroilan.tessera.exception.BusinessRuleException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.util.Optional

class RoutingServiceTest {
    private lateinit var repository: RoutingRepository
    private lateinit var productService: ProductService
    private lateinit var workCenterService: WorkCenterService
    private lateinit var service: RoutingService

    private val orgId = java.util.UUID.fromString("550e8400-e29b-41d4-a716-446655440000")
    private val userId = java.util.UUID.fromString("550e8400-e29b-41d4-a716-446655440001")
    private val productId = java.util.UUID.fromString("550e8400-e29b-41d4-a716-446655440002")
    private val wcId = java.util.UUID.fromString("550e8400-e29b-41d4-a716-446655440003")

    @BeforeEach
    fun setup() {
        repository = mock(RoutingRepository::class.java)
        productService = mock(ProductService::class.java)
        workCenterService = mock(WorkCenterService::class.java)
        whenever(repository.save(any<Routing>())).thenAnswer { it.arguments[0] }
        whenever(repository.findByOrganizationIdAndCode(any(), any())).thenReturn(Optional.empty())
        whenever(repository.findByOrganizationIdAndProductId(any(), any())).thenReturn(emptyList())
        whenever(repository.findByOrganizationIdAndProductIdAndIsDefaultTrue(any(), any()))
            .thenReturn(Optional.empty())
        whenever(productService.getProduct(productId, orgId)).thenReturn(
            Product(
                id = productId,
                sku = "SKU",
                name = "Widget",
                listPrice = BigDecimal.ONE,
                priceCurrency = "USD",
                organizationId = orgId,
                isActive = true,
            ),
        )
        whenever(workCenterService.getWorkCenter(wcId, orgId)).thenReturn(
            WorkCenter(id = wcId, code = "CNC", name = "CNC", organizationId = orgId, isActive = true),
        )
        service = RoutingService(repository, productService, workCenterService)
    }

    @Test
    fun `create assigns operation numbers in increments of ten`() {
        val routing =
            service.createRouting(
                CreateRoutingRequest(
                    productId = productId,
                    code = "RT-1",
                    name = "Standard routing",
                    operations =
                        listOf(
                            op("Prep"),
                            op("Mill"),
                            op("Inspect"),
                        ),
                ),
                orgId,
                userId.toString(),
            )
        assertThat(routing.operations.map { it.operationNumber }).containsExactly(10, 20, 30)
        assertThat(routing.status).isEqualTo(RoutingStatus.DRAFT)
        assertThat(routing.version).isEqualTo(1)
    }

    @Test
    fun `create rejects an operation with zero setup and run time`() {
        val req =
            CreateRoutingRequest(
                productId = productId,
                code = "RT-1",
                name = "n",
                operations =
                    listOf(
                        CreateRoutingOperationRequest(
                            workCenterId = wcId,
                            description = "no-op",
                            setupMinutes = BigDecimal.ZERO,
                            runMinutesPerUnit = BigDecimal.ZERO,
                        ),
                    ),
            )
        assertThatThrownBy { service.createRouting(req, orgId, userId.toString()) }
            .isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `update is rejected when routing is not DRAFT`() {
        val active =
            Routing(
                id = java.util.UUID.fromString("00000000-0000-0000-0000-000000000011"),
                organizationId = orgId,
                productId = productId,
                code = "RT-1",
                name = "n",
                status = RoutingStatus.ACTIVE,
                operations = emptyList(),
                createdBy = userId.toString(),
            )
        whenever(repository.findById(java.util.UUID.fromString("00000000-0000-0000-0000-000000000011"))).thenReturn(Optional.of(active))
        assertThatThrownBy {
            service.updateRouting(
                java.util.UUID.fromString("00000000-0000-0000-0000-000000000011"),
                UpdateRoutingRequest(name = "renamed"),
                orgId,
            )
        }.isInstanceOf(BusinessRuleException::class.java)
    }

    private fun op(description: String) =
        CreateRoutingOperationRequest(
            workCenterId = wcId,
            description = description,
            setupMinutes = BigDecimal("5"),
            runMinutesPerUnit = BigDecimal("1.5"),
            queueMinutes = BigDecimal.ZERO,
        )
}
