package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.CreateBomLineRequest
import com.aquinofroilan.tessera.dto.CreateBomRequest
import com.aquinofroilan.tessera.dto.UpdateBomRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.model.BillOfMaterials
import com.aquinofroilan.tessera.model.BomLine
import com.aquinofroilan.tessera.model.BomStatus
import com.aquinofroilan.tessera.model.Product
import com.aquinofroilan.tessera.repository.BillOfMaterialsRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.util.Optional
import java.util.UUID

class BillOfMaterialsServiceTest {
    private lateinit var bomRepository: BillOfMaterialsRepository
    private lateinit var productService: ProductService
    private lateinit var service: BillOfMaterialsService

    private val orgId = java.util.UUID.fromString("e5628ca4-87a8-3e6f-8ae2-20213cc7ef92")
    private val userId  = java.util.UUID.fromString("1db2395f-13ba-3d37-9d2b-f77d3eb3aa2e")
    private val parentId = java.util.UUID.fromString("8460eb67-cc2b-3de5-bdba-6c4320d2c3de")
    private val compAId = java.util.UUID.fromString("97c02c2b-db1d-3201-b200-5645c65c4ecc")
    private val compBId = java.util.UUID.fromString("f411ffb7-66f2-3534-92de-46af447dbec3")

    @BeforeEach
    fun setup() {
        bomRepository = mock(BillOfMaterialsRepository::class.java)
        productService = mock(ProductService::class.java)
        whenever(bomRepository.save(any<BillOfMaterials>())).thenAnswer { it.arguments[0] }
        whenever(bomRepository.findByOrganizationIdAndCode(any(), any())).thenReturn(Optional.empty())
        whenever(bomRepository.findByOrganizationIdAndProductId(any(), any())).thenReturn(emptyList())
        whenever(bomRepository.findByOrganizationIdAndProductIdAndIsDefaultTrue(any(), any()))
            .thenReturn(Optional.empty())
        whenever(productService.getProduct(parentId, orgId)).thenReturn(product(parentId, "65817213-08c3-311c-bd60-044bb2ac84d8"))
        whenever(productService.getProduct(compAId, orgId)).thenReturn(product(compAId, "3d79a2eb-7221-3c73-aab1-048d4848d923"))
        whenever(productService.getProduct(compBId, orgId)).thenReturn(product(compBId, "d44a6bcf-e191-3b25-b7c8-60f8c11b710f"))
        service = BillOfMaterialsService(bomRepository, productService)
    }

    @Test
    fun `create persists a draft bom with auto-incremented version`() {
        whenever(bomRepository.findByOrganizationIdAndProductId(orgId, parentId))
            .thenReturn(listOf(bom(version = 3)))

        val bom = service.createBom(request(), orgId, userId)

        assertThat(bom.status).isEqualTo(BomStatus.DRAFT)
        assertThat(bom.version).isEqualTo(4)
        assertThat(bom.lines).hasSize(2)
        assertThat(bom.lines[0].lineNumber).isEqualTo(1)
        assertThat(bom.lines[1].componentSku).isEqualTo("d44a6bcf-e191-3b25-b7c8-60f8c11b710f")
    }

    @Test
    fun `create rejects duplicate code`() {
        whenever(bomRepository.findByOrganizationIdAndCode(orgId, "BOM-1"))
            .thenReturn(Optional.of(bom()))

        assertThatThrownBy { service.createBom(request(), orgId, userId) }
            .isInstanceOf(BusinessRuleException::class.java)
            .hasMessageContaining("already exists")
    }

    @Test
    fun `create rejects component equal to parent`() {
        val req = request(lines = listOf(line(parentId, BigDecimal.ONE)))
        assertThatThrownBy { service.createBom(req, orgId, userId) }
            .isInstanceOf(BusinessRuleException::class.java)
            .hasMessageContaining("its own product")
    }

    @Test
    fun `create rejects duplicate component lines`() {
        val req = request(lines = listOf(line(compAId, BigDecimal.ONE), line(compAId, BigDecimal.TWO)))
        assertThatThrownBy { service.createBom(req, orgId, userId) }
            .isInstanceOf(BusinessRuleException::class.java)
            .hasMessageContaining("Duplicate component")
    }

    @Test
    fun `create rejects inverted effective window`() {
        val req =
            request(
                effectiveFrom = java.time.LocalDate.of(2026, 6, 1),
                effectiveTo = java.time.LocalDate.of(2026, 5, 1),
            )
        assertThatThrownBy { service.createBom(req, orgId, userId) }
            .isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `activate marks bom ACTIVE and stamps audit fields`() {
        val draft = bom().copy(id = java.util.UUID.fromString("10c2deb1-ad3c-35e5-b931-4fa8a1ecd140"))
        whenever(bomRepository.findById(java.util.UUID.fromString("10c2deb1-ad3c-35e5-b931-4fa8a1ecd140"))).thenReturn(Optional.of(draft))

        val activated =
            service.activateBom(
                java.util.UUID.fromString("10c2deb1-ad3c-35e5-b931-4fa8a1ecd140"),
                orgId,
                userId,
                makeDefault = false,
            )

        assertThat(activated.status).isEqualTo(BomStatus.ACTIVE)
        assertThat(activated.activatedBy).isEqualTo(userId)
        assertThat(activated.isDefault).isFalse()
    }

    @Test
    fun `activate with makeDefault demotes the previous default`() {
        val draft = bom().copy(id = java.util.UUID.fromString("89cc91a6-b578-318a-81f6-141ba86d1362"))
        val priorDefault =
            bom().copy(
                id = java.util.UUID.fromString("10c2deb1-ad3c-35e5-b931-4fa8a1ecd140"),
                isDefault = true,
                status = BomStatus.ACTIVE,
            )
        whenever(bomRepository.findById(java.util.UUID.fromString("89cc91a6-b578-318a-81f6-141ba86d1362"))).thenReturn(Optional.of(draft))
        whenever(bomRepository.findByOrganizationIdAndProductIdAndIsDefaultTrue(orgId, parentId))
            .thenReturn(Optional.of(priorDefault))

        val activated =
            service.activateBom(
                java.util.UUID.fromString("89cc91a6-b578-318a-81f6-141ba86d1362"),
                orgId,
                userId,
                makeDefault = true,
            )

        assertThat(activated.isDefault).isTrue()
        assertThat(activated.status).isEqualTo(BomStatus.ACTIVE)
    }

    @Test
    fun `update is rejected when bom is not DRAFT`() {
        val active = bom().copy(id = java.util.UUID.fromString("10c2deb1-ad3c-35e5-b931-4fa8a1ecd140"), status = BomStatus.ACTIVE)
        whenever(bomRepository.findById(java.util.UUID.fromString("10c2deb1-ad3c-35e5-b931-4fa8a1ecd140"))).thenReturn(Optional.of(active))

        assertThatThrownBy {
            service.updateBom(java.util.UUID.fromString("10c2deb1-ad3c-35e5-b931-4fa8a1ecd140"), UpdateBomRequest(name = "renamed"), orgId)
        }.isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `obsolete clears default flag`() {
        val active =
            bom().copy(
                id = java.util.UUID.fromString("10c2deb1-ad3c-35e5-b931-4fa8a1ecd140"),
                status = BomStatus.ACTIVE,
                isDefault = true,
            )
        whenever(bomRepository.findById(java.util.UUID.fromString("10c2deb1-ad3c-35e5-b931-4fa8a1ecd140"))).thenReturn(Optional.of(active))

        val obsoleted = service.obsoleteBom(java.util.UUID.fromString("10c2deb1-ad3c-35e5-b931-4fa8a1ecd140"), orgId, userId)

        assertThat(obsoleted.status).isEqualTo(BomStatus.OBSOLETE)
        assertThat(obsoleted.isDefault).isFalse()
        assertThat(obsoleted.obsoletedBy).isEqualTo(userId)
    }

    @Test
    fun `delete is rejected when bom is not DRAFT`() {
        val active = bom().copy(id = java.util.UUID.fromString("10c2deb1-ad3c-35e5-b931-4fa8a1ecd140"), status = BomStatus.ACTIVE)
        whenever(bomRepository.findById(java.util.UUID.fromString("10c2deb1-ad3c-35e5-b931-4fa8a1ecd140"))).thenReturn(Optional.of(active))

        assertThatThrownBy { service.deleteBom(java.util.UUID.fromString("10c2deb1-ad3c-35e5-b931-4fa8a1ecd140"), orgId) }
            .isInstanceOf(BusinessRuleException::class.java)
    }

    private fun request(
        lines: List<CreateBomLineRequest> = listOf(line(compAId, BigDecimal("2.0")), line(compBId, BigDecimal("1.5"))),
        effectiveFrom: java.time.LocalDate? = null,
        effectiveTo: java.time.LocalDate? = null,
    ) = CreateBomRequest(
        productId = parentId,
        code = "BOM-1",
        name = "Parent BOM",
        version = null,
        isDefault = false,
        effectiveFrom = effectiveFrom,
        effectiveTo = effectiveTo,
        notes = null,
        lines = lines,
    )

    private fun line(
        productId: UUID,
        quantity: BigDecimal,
    ) = CreateBomLineRequest(
        componentProductId = productId,
        quantity = quantity,
        uom = "EA",
        scrapPct = BigDecimal.ZERO,
        notes = null,
    )

    private fun bom(version: Int = 1) =
        BillOfMaterials(
            organizationId = orgId,
            productId = parentId,
            code = "BOM-0",
            name = "Parent BOM",
            version = version,
            status = BomStatus.DRAFT,
            isDefault = false,
            lines =
                listOf(
                    BomLine(
                        lineNumber = 1,
                        componentProductId = compAId,
                        componentSku = "COMP-A",
                        componentName = "Component A",
                        quantity = BigDecimal.ONE,
                    ),
                ),
            createdBy = userId,
        )

    private fun product(
        id: UUID,
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
