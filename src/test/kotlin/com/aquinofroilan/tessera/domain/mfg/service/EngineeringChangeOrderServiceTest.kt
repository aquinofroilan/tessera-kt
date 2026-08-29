package com.aquinofroilan.tessera.domain.mfg.service

import com.aquinofroilan.tessera.domain.mfg.dto.CreateEcoRequest
import com.aquinofroilan.tessera.domain.mfg.model.BillOfMaterials
import com.aquinofroilan.tessera.domain.mfg.model.BomStatus
import com.aquinofroilan.tessera.domain.mfg.model.EcoItemType
import com.aquinofroilan.tessera.domain.mfg.model.EcoStatus
import com.aquinofroilan.tessera.domain.mfg.model.EngineeringChangeOrder
import com.aquinofroilan.tessera.domain.mfg.repository.BillOfMaterialsRepository
import com.aquinofroilan.tessera.domain.mfg.repository.EngineeringChangeOrderRepository
import com.aquinofroilan.tessera.domain.mfg.repository.RoutingRepository
import com.aquinofroilan.tessera.exception.BusinessRuleException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

class EngineeringChangeOrderServiceTest {
    private val ecoRepository: EngineeringChangeOrderRepository = mock()
    private val bomRepository: BillOfMaterialsRepository = mock()
    private val routingRepository: RoutingRepository = mock()

    private val ecoService = EngineeringChangeOrderService(ecoRepository, bomRepository, routingRepository)

    @Test
    fun `should create new ECO successfully`() {
        val organizationId = UUID.randomUUID()
        val requestedBy = UUID.randomUUID()
        val request = CreateEcoRequest("ECO-001", "Update BOM", "Description", LocalDate.now())

        whenever(ecoRepository.findByOrganizationIdAndEcoNumber(organizationId, "ECO-001")).thenReturn(null)
        whenever(ecoRepository.save(any<EngineeringChangeOrder>())).thenAnswer { it.arguments[0] }

        val result = ecoService.createEco(organizationId, requestedBy, request)

        assertEquals("ECO-001", result.ecoNumber)
        assertEquals(EcoStatus.DRAFT, result.status)
        verify(ecoRepository).save(any())
    }

    @Test
    fun `should fail to create ECO if number exists`() {
        val organizationId = UUID.randomUUID()
        val requestedBy = UUID.randomUUID()
        val request = CreateEcoRequest("ECO-001", "Update BOM", "Description", LocalDate.now())

        whenever(ecoRepository.findByOrganizationIdAndEcoNumber(organizationId, "ECO-001"))
            .thenReturn(
                EngineeringChangeOrder(organizationId = organizationId, ecoNumber = "ECO-001", title = "T", requestedBy = requestedBy),
            )

        assertThrows(BusinessRuleException::class.java) {
            ecoService.createEco(organizationId, requestedBy, request)
        }
    }

    @Test
    fun `should apply ECO and activate new BOM`() {
        val organizationId = UUID.randomUUID()
        val ecoId = UUID.randomUUID()
        val oldBomId = UUID.randomUUID()
        val newBomId = UUID.randomUUID()
        val requestedBy = UUID.randomUUID()
        val appliedBy = UUID.randomUUID()

        val eco =
            EngineeringChangeOrder(
                id = ecoId,
                organizationId = organizationId,
                ecoNumber = "ECO-002",
                title = "Test",
                status = EcoStatus.APPROVED,
                requestedBy = requestedBy,
                effectiveDate = LocalDate.now(),
            )

        eco.addAffectedItem(
            com.aquinofroilan.tessera.domain.mfg.model.EcoAffectedItem(
                itemType = EcoItemType.BOM,
                oldVersionId = oldBomId,
                newVersionId = newBomId,
            ),
        )

        val oldBom =
            BillOfMaterials(
                id = oldBomId,
                organizationId = organizationId,
                productId = UUID.randomUUID(),
                code = "BOM-01",
                name = "Old BOM",
                status = BomStatus.ACTIVE,
                lines = emptyList(),
                createdBy = UUID.randomUUID(),
            )

        val newBom =
            BillOfMaterials(
                id = newBomId,
                organizationId = organizationId,
                productId = UUID.randomUUID(),
                code = "BOM-02",
                name = "New BOM",
                status = BomStatus.DRAFT,
                lines = emptyList(),
                createdBy = UUID.randomUUID(),
            )

        whenever(ecoRepository.findById(ecoId)).thenReturn(Optional.of(eco))
        whenever(bomRepository.findById(oldBomId)).thenReturn(Optional.of(oldBom))
        whenever(bomRepository.findById(newBomId)).thenReturn(Optional.of(newBom))
        whenever(ecoRepository.save(any<EngineeringChangeOrder>())).thenAnswer { it.arguments[0] }

        val result = ecoService.applyEco(organizationId, ecoId, appliedBy)

        assertEquals(EcoStatus.IMPLEMENTED, result.status)
        assertNotNull(result.implementedAt)
        assertEquals(BomStatus.OBSOLETE, oldBom.status)
        assertEquals(BomStatus.ACTIVE, newBom.status)

        verify(bomRepository).save(oldBom)
        verify(bomRepository).save(newBom)
    }
}
