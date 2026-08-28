package com.aquinofroilan.tessera.domain.mfg.service

import com.aquinofroilan.tessera.domain.inventory.model.Product
import com.aquinofroilan.tessera.domain.inventory.service.ProductService
import com.aquinofroilan.tessera.domain.mfg.dto.ManualStandardCostRequest
import com.aquinofroilan.tessera.domain.mfg.dto.RollupRequest
import com.aquinofroilan.tessera.domain.mfg.model.BillOfMaterials
import com.aquinofroilan.tessera.domain.mfg.model.BomLine
import com.aquinofroilan.tessera.domain.mfg.model.BomStatus
import com.aquinofroilan.tessera.domain.mfg.model.ProductStandardCost
import com.aquinofroilan.tessera.domain.mfg.repository.ProductStandardCostRepository
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

class StandardCostServiceTest {
    private lateinit var repository: ProductStandardCostRepository
    private lateinit var productService: ProductService
    private lateinit var bomService: BillOfMaterialsService
    private lateinit var routingService: RoutingService
    private lateinit var workCenterService: WorkCenterService
    private lateinit var service: StandardCostService

    private val orgId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val userId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000002")
    private val parentId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000003")
    private val compId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000004")

    @BeforeEach
    fun setup() {
        repository = mock(ProductStandardCostRepository::class.java)
        productService = mock(ProductService::class.java)
        bomService = mock(BillOfMaterialsService::class.java)
        routingService = mock(RoutingService::class.java)
        workCenterService = mock(WorkCenterService::class.java)
        whenever(repository.save(any<ProductStandardCost>())).thenAnswer { it.arguments[0] }
        whenever(productService.getProduct(parentId, orgId))
            .thenReturn(product(parentId, "PARENT"))
        whenever(repository.findByOrganizationIdAndProductId(orgId, compId))
            .thenReturn(Optional.of(componentCost(BigDecimal("4.00"))))
        whenever(repository.findByOrganizationIdAndProductId(orgId, parentId))
            .thenReturn(Optional.empty())
        whenever(bomService.listBoms(orgId, BomStatus.ACTIVE, parentId))
            .thenReturn(listOf(defaultBom()))
        whenever(routingService.listRoutings(any(), any(), any())).thenReturn(emptyList())
        service = StandardCostService(repository, productService, bomService, routingService, workCenterService)
    }

    @Test
    fun `rollup computes material cost including scrap`() {
        val record = service.rollup(parentId, RollupRequest(), orgId, userId.toString())
        // 4.00 (component standard) * 2.0 quantity * (1 + 5/100 scrap) = 8.4000
        assertThat(record.materialCost).isEqualByComparingTo(BigDecimal("8.4000"))
        assertThat(record.laborCost).isEqualByComparingTo(BigDecimal.ZERO)
        assertThat(record.totalCost).isEqualByComparingTo(BigDecimal("8.4000"))
        assertThat(record.source).isEqualTo("rollup")
    }

    @Test
    fun `rollup applies overhead rate`() {
        val record = service.rollup(parentId, RollupRequest(overheadRatePct = BigDecimal("10")), orgId, userId.toString())
        // material 8.4 * 10% = 0.84 overhead, total 9.24
        assertThat(record.overheadCost).isEqualByComparingTo(BigDecimal("0.8400"))
        assertThat(record.totalCost).isEqualByComparingTo(BigDecimal("9.2400"))
    }

    @Test
    fun `rollup fails when a component has no standard cost`() {
        whenever(repository.findByOrganizationIdAndProductId(orgId, compId)).thenReturn(Optional.empty())
        assertThatThrownBy { service.rollup(parentId, RollupRequest(), orgId, userId.toString()) }
            .isInstanceOf(BusinessRuleException::class.java)
            .hasMessageContaining("roll it up first")
    }

    @Test
    fun `manual override sets source to manual and clears bom-routing refs`() {
        val record =
            service.setManual(
                parentId,
                ManualStandardCostRequest(
                    materialCost = BigDecimal("5"),
                    laborCost = BigDecimal("3"),
                    overheadCost = BigDecimal("1"),
                ),
                orgId,
                userId.toString(),
            )
        assertThat(record.source).isEqualTo("manual")
        assertThat(record.bomId).isNull()
        assertThat(record.totalCost).isEqualByComparingTo(BigDecimal("9.0000"))
    }

    private fun defaultBom() =
        BillOfMaterials(
            id = java.util.UUID.fromString("00000000-0000-0000-0000-000000000012"),
            organizationId = orgId,
            productId = parentId,
            code = "BOM-1",
            name = "B",
            status = BomStatus.ACTIVE,
            isDefault = true,
            lines =
                listOf(
                    BomLine(
                        lineNumber = 1,
                        componentProductId = compId,
                        componentSku = "COMP",
                        componentName = "Component",
                        quantity = BigDecimal("2.0"),
                        scrapPct = BigDecimal("5"),
                    ),
                ),
            createdBy = userId,
        )

    private fun componentCost(total: BigDecimal) =
        ProductStandardCost(
            organizationId = orgId,
            productId = compId,
            materialCost = total,
            totalCost = total,
            calculatedBy = userId.toString(),
        )

    private fun product(
        id: java.util.UUID,
        sku: String,
    ) = Product(
        id = id,
        sku = sku,
        name = sku,
        listPrice = BigDecimal.ONE,
        priceCurrency = "USD",
        organizationId = orgId,
        isActive = true,
    )
}
