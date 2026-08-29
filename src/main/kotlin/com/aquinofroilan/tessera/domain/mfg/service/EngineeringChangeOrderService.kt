package com.aquinofroilan.tessera.domain.mfg.service

import com.aquinofroilan.tessera.domain.mfg.dto.AddEcoItemRequest
import com.aquinofroilan.tessera.domain.mfg.dto.CreateEcoRequest
import com.aquinofroilan.tessera.domain.mfg.dto.EcoAffectedItemDto
import com.aquinofroilan.tessera.domain.mfg.dto.EngineeringChangeOrderDto
import com.aquinofroilan.tessera.domain.mfg.model.BomStatus
import com.aquinofroilan.tessera.domain.mfg.model.EcoAffectedItem
import com.aquinofroilan.tessera.domain.mfg.model.EcoItemType
import com.aquinofroilan.tessera.domain.mfg.model.EcoStatus
import com.aquinofroilan.tessera.domain.mfg.model.EngineeringChangeOrder
import com.aquinofroilan.tessera.domain.mfg.model.RoutingStatus
import com.aquinofroilan.tessera.domain.mfg.repository.BillOfMaterialsRepository
import com.aquinofroilan.tessera.domain.mfg.repository.EngineeringChangeOrderRepository
import com.aquinofroilan.tessera.domain.mfg.repository.RoutingRepository
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class EngineeringChangeOrderService(
    private val ecoRepository: EngineeringChangeOrderRepository,
    private val bomRepository: BillOfMaterialsRepository,
    private val routingRepository: RoutingRepository,
) {
    @Transactional
    fun createEco(
        organizationId: UUID,
        requestedBy: UUID,
        request: CreateEcoRequest,
    ): EngineeringChangeOrderDto {
        val existingEco = ecoRepository.findByOrganizationIdAndEcoNumber(organizationId, request.ecoNumber)
        if (existingEco != null) {
            throw BusinessRuleException("ECO number ${request.ecoNumber} already exists.")
        }

        val eco =
            EngineeringChangeOrder(
                organizationId = organizationId,
                ecoNumber = request.ecoNumber,
                title = request.title,
                description = request.description,
                effectiveDate = request.effectiveDate,
                requestedBy = requestedBy,
            )

        val savedEco = ecoRepository.save(eco)
        return mapToDto(savedEco)
    }

    @Transactional
    fun addAffectedItem(
        organizationId: UUID,
        ecoId: UUID,
        request: AddEcoItemRequest,
    ): EngineeringChangeOrderDto {
        val eco = getEcoOrThrow(organizationId, ecoId)

        if (eco.status != EcoStatus.DRAFT) {
            throw BusinessRuleException("Cannot add items to an ECO that is not in DRAFT status.")
        }

        // Validate that new version exists and is in DRAFT
        when (request.itemType) {
            EcoItemType.BOM -> {
                val bom =
                    bomRepository
                        .findById(request.newVersionId)
                        .orElseThrow { ResourceNotFoundException("BOM not found") }
                if (bom.status != BomStatus.DRAFT) throw BusinessRuleException("New BOM version must be in DRAFT status.")
            }
            EcoItemType.ROUTING -> {
                val routing =
                    routingRepository
                        .findById(request.newVersionId)
                        .orElseThrow { ResourceNotFoundException("Routing not found") }
                if (routing.status != RoutingStatus.DRAFT) throw BusinessRuleException("New Routing version must be in DRAFT status.")
            }
        }

        val item =
            EcoAffectedItem(
                itemType = request.itemType,
                oldVersionId = request.oldVersionId,
                newVersionId = request.newVersionId,
                notes = request.notes,
            )

        eco.addAffectedItem(item)
        val savedEco = ecoRepository.save(eco)
        return mapToDto(savedEco)
    }

    @Transactional
    fun submitForReview(
        organizationId: UUID,
        ecoId: UUID,
    ): EngineeringChangeOrderDto {
        val eco = getEcoOrThrow(organizationId, ecoId)
        if (eco.status != EcoStatus.DRAFT) {
            throw BusinessRuleException("Only DRAFT ECOs can be submitted for review.")
        }
        eco.status = EcoStatus.PENDING_REVIEW
        return mapToDto(ecoRepository.save(eco))
    }

    @Transactional
    fun approveEco(
        organizationId: UUID,
        ecoId: UUID,
        approvedBy: UUID,
    ): EngineeringChangeOrderDto {
        val eco = getEcoOrThrow(organizationId, ecoId)
        if (eco.status != EcoStatus.PENDING_REVIEW) {
            throw BusinessRuleException("Only PENDING_REVIEW ECOs can be approved.")
        }
        eco.status = EcoStatus.APPROVED
        eco.approvedBy = approvedBy
        eco.approvedAt = LocalDateTime.now()
        return mapToDto(ecoRepository.save(eco))
    }

    @Transactional
    fun applyEco(
        organizationId: UUID,
        ecoId: UUID,
        appliedBy: UUID,
    ): EngineeringChangeOrderDto {
        val eco = getEcoOrThrow(organizationId, ecoId)
        if (eco.status != EcoStatus.APPROVED) {
            throw BusinessRuleException("Only APPROVED ECOs can be applied.")
        }

        val now = LocalDateTime.now()

        for (item in eco.affectedItems) {
            when (item.itemType) {
                EcoItemType.BOM -> {
                    item.oldVersionId?.let { oldId ->
                        val oldBom = bomRepository.findById(oldId).orElse(null)
                        if (oldBom != null) {
                            oldBom.status = BomStatus.OBSOLETE
                            oldBom.effectiveTo = eco.effectiveDate ?: now.toLocalDate()
                            oldBom.obsoletedAt = now
                            oldBom.obsoletedBy = appliedBy
                            bomRepository.save(oldBom)
                        }
                    }
                    val newBom =
                        bomRepository
                            .findById(
                                item.newVersionId,
                            ).orElseThrow { ResourceNotFoundException("New BOM version not found") }
                    newBom.status = BomStatus.ACTIVE
                    newBom.effectiveFrom = eco.effectiveDate ?: now.toLocalDate()
                    newBom.activatedAt = now
                    newBom.activatedBy = appliedBy
                    bomRepository.save(newBom)
                }
                EcoItemType.ROUTING -> {
                    item.oldVersionId?.let { oldId ->
                        val oldRouting = routingRepository.findById(oldId).orElse(null)
                        if (oldRouting != null) {
                            oldRouting.status = RoutingStatus.OBSOLETE
                            oldRouting.effectiveTo = eco.effectiveDate ?: now.toLocalDate()
                            oldRouting.obsoletedAt = now
                            oldRouting.obsoletedBy = appliedBy.toString()
                            routingRepository.save(oldRouting)
                        }
                    }
                    val newRouting =
                        routingRepository
                            .findById(
                                item.newVersionId,
                            ).orElseThrow { ResourceNotFoundException("New Routing version not found") }
                    newRouting.status = RoutingStatus.ACTIVE
                    newRouting.effectiveFrom = eco.effectiveDate ?: now.toLocalDate()
                    newRouting.activatedAt = now
                    newRouting.activatedBy = appliedBy.toString()
                    routingRepository.save(newRouting)
                }
            }
        }

        eco.status = EcoStatus.IMPLEMENTED
        eco.implementedAt = now
        return mapToDto(ecoRepository.save(eco))
    }

    private fun getEcoOrThrow(
        organizationId: UUID,
        ecoId: UUID,
    ): EngineeringChangeOrder {
        val eco =
            ecoRepository
                .findById(ecoId)
                .orElseThrow { ResourceNotFoundException("Engineering Change Order not found") }
        if (eco.organizationId != organizationId) {
            throw BusinessRuleException("ECO does not belong to the organization.")
        }
        return eco
    }

    private fun mapToDto(eco: EngineeringChangeOrder): EngineeringChangeOrderDto =
        EngineeringChangeOrderDto(
            id = eco.id,
            organizationId = eco.organizationId,
            ecoNumber = eco.ecoNumber,
            title = eco.title,
            description = eco.description,
            status = eco.status,
            effectiveDate = eco.effectiveDate,
            requestedBy = eco.requestedBy,
            approvedBy = eco.approvedBy,
            approvedAt = eco.approvedAt,
            implementedAt = eco.implementedAt,
            affectedItems =
                eco.affectedItems.map {
                    EcoAffectedItemDto(
                        id = it.id,
                        itemType = it.itemType,
                        oldVersionId = it.oldVersionId,
                        newVersionId = it.newVersionId,
                        notes = it.notes,
                    )
                },
        )
}
