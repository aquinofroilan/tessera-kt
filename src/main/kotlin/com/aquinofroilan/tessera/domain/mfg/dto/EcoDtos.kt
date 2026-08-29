package com.aquinofroilan.tessera.domain.mfg.dto

import com.aquinofroilan.tessera.domain.mfg.model.EcoItemType
import com.aquinofroilan.tessera.domain.mfg.model.EcoStatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class EngineeringChangeOrderDto(
    val id: UUID,
    val organizationId: UUID,
    val ecoNumber: String,
    val title: String,
    val description: String?,
    val status: EcoStatus,
    val effectiveDate: LocalDate?,
    val requestedBy: UUID,
    val approvedBy: UUID?,
    val approvedAt: LocalDateTime?,
    val implementedAt: LocalDateTime?,
    val affectedItems: List<EcoAffectedItemDto>
)

data class EcoAffectedItemDto(
    val id: UUID,
    val itemType: EcoItemType,
    val oldVersionId: UUID?,
    val newVersionId: UUID,
    val notes: String?
)

data class CreateEcoRequest(
    val ecoNumber: String,
    val title: String,
    val description: String?,
    val effectiveDate: LocalDate?
)

data class AddEcoItemRequest(
    val itemType: EcoItemType,
    val oldVersionId: UUID?,
    val newVersionId: UUID,
    val notes: String?
)
