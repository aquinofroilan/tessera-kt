package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.model.BillOfMaterials
import com.aquinofroilan.tessera.model.BomLine
import com.aquinofroilan.tessera.model.BomStatus
import com.aquinofroilan.tessera.model.MpsEntry
import com.aquinofroilan.tessera.model.MpsStatus
import com.aquinofroilan.tessera.model.StockOnHand
import com.aquinofroilan.tessera.repository.MpsEntryRepository
import com.aquinofroilan.tessera.repository.StockOnHandRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.LocalDate

class MrpServiceTest {
    private lateinit var mpsRepository: MpsEntryRepository
    private lateinit var bomService: BillOfMaterialsService
    private lateinit var sohRepository: StockOnHandRepository
    private lateinit var service: MrpService

    private val orgId = "org-1"
    private val parentId = "prod-parent"
    private val compId = "prod-comp"

    @BeforeEach
    fun setup() {
        mpsRepository = mock(MpsEntryRepository::class.java)
        bomService = mock(BillOfMaterialsService::class.java)
        sohRepository = mock(StockOnHandRepository::class.java)
        service = MrpService(mpsRepository, bomService, sohRepository)
    }

    @Test
    fun `empty MPS returns empty plan`() {
        whenever(mpsRepository.findByOrganizationIdOrderByRequiredByAsc(orgId)).thenReturn(emptyList())
        val plan = service.run(orgId, null)
        assertThat(plan.requirements).isEmpty()
        assertThat(plan.mpsEntriesConsidered).isEqualTo(0)
    }

    @Test
    fun `MRP nets gross requirement against on-hand and flags unresolved`() {
        val mps =
            listOf(
                mpsEntry(productId = parentId, qty = BigDecimal("10"), date = LocalDate.of(2026, 7, 1)),
                mpsEntry(productId = "prod-orphan", qty = BigDecimal("5"), date = LocalDate.of(2026, 7, 5)),
            )
        whenever(mpsRepository.findByOrganizationIdOrderByRequiredByAsc(orgId)).thenReturn(mps)
        whenever(bomService.listBoms(orgId, BomStatus.ACTIVE, parentId)).thenReturn(listOf(bom()))
        whenever(bomService.listBoms(orgId, BomStatus.ACTIVE, "prod-orphan")).thenReturn(emptyList())
        whenever(sohRepository.findByOrganizationId(orgId)).thenReturn(
            listOf(StockOnHand(organizationId = orgId, productId = compId, warehouseId = "wh1", quantity = BigDecimal("8"))),
        )

        val plan = service.run(orgId, null)

        assertThat(plan.mpsEntriesConsidered).isEqualTo(2)
        assertThat(plan.unresolved).hasSize(1)
        assertThat(plan.requirements).hasSize(1)
        val req = plan.requirements.single()
        // gross = 10 (parent) * 2.0 (bom qty) * 1.05 (5% scrap) = 21.0000
        assertThat(req.grossRequirement).isEqualByComparingTo(BigDecimal("21.0000"))
        assertThat(req.onHand).isEqualByComparingTo(BigDecimal("8.0000"))
        assertThat(req.netRequirement).isEqualByComparingTo(BigDecimal("13.0000"))
    }

    private fun mpsEntry(
        productId: String,
        qty: BigDecimal,
        date: LocalDate,
    ) = MpsEntry(
        organizationId = orgId,
        productId = productId,
        productSku = productId,
        productName = productId,
        quantity = qty,
        requiredBy = date,
        status = MpsStatus.PLANNED,
        createdBy = "u",
    )

    private fun bom() =
        BillOfMaterials(
            id = "bom-1",
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
            createdBy = "u",
        )
}
