package com.aquinofroilan.tessera.domain.mfg.service

import com.aquinofroilan.tessera.domain.inventory.service.WarehouseService
import com.aquinofroilan.tessera.domain.mfg.dto.CreateWorkCenterRequest
import com.aquinofroilan.tessera.domain.mfg.dto.UpdateWorkCenterRequest
import com.aquinofroilan.tessera.domain.mfg.model.WorkCenter
import com.aquinofroilan.tessera.domain.mfg.repository.WorkCenterRepository
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal

@Service
class WorkCenterService(
    private val workCenterRepository: WorkCenterRepository,
    private val warehouseService: WarehouseService,
) {
    @Transactional
    fun createWorkCenter(
        request: CreateWorkCenterRequest,
        organizationId: java.util.UUID,
    ): WorkCenter {
        val code = request.code.trim().uppercase()
        if (code.isBlank()) throw BusinessRuleException("Code cannot be blank")
        workCenterRepository.findByOrganizationIdAndCode(organizationId, code).ifPresent {
            throw BusinessRuleException("Work center with code '$code' already exists")
        }
        if (request.warehouseId != null) {
            warehouseService.getWarehouse(request.warehouseId, organizationId)
        }
        val wc =
            WorkCenter(
                organizationId = organizationId,
                code = code,
                name = request.name.trim(),
                description = request.description,
                type = request.type,
                warehouseId = request.warehouseId,
                capacityPerHour = request.capacityPerHour ?: BigDecimal.ONE,
                costPerHour = request.costPerHour ?: BigDecimal.ZERO,
                efficiencyPct = request.efficiencyPct ?: BigDecimal(100),
            )
        return try {
            workCenterRepository.save(wc)
        } catch (e: DataIntegrityViolationException) {
            throw BusinessRuleException("Work center with code '$code' already exists")
        }
    }

    fun getWorkCenter(
        id: java.util.UUID,
        organizationId: java.util.UUID,
    ): WorkCenter {
        val wc =
            workCenterRepository.findById(id).orElseThrow {
                ResourceNotFoundException("Work center not found: $id")
            }
        if (wc.organizationId != organizationId) {
            throw ResourceNotFoundException("Work center not found: $id")
        }
        return wc
    }

    fun listWorkCenters(
        organizationId: java.util.UUID,
        activeOnly: Boolean,
    ): List<WorkCenter> =
        if (activeOnly) {
            workCenterRepository.findByOrganizationIdAndIsActive(organizationId, true)
        } else {
            workCenterRepository.findByOrganizationId(organizationId)
        }

    @Transactional
    fun updateWorkCenter(
        id: java.util.UUID,
        request: UpdateWorkCenterRequest,
        organizationId: java.util.UUID,
    ): WorkCenter {
        val wc = getWorkCenter(id, organizationId)
        if (request.warehouseId != null) {
            warehouseService.getWarehouse(request.warehouseId, organizationId)
        }
        return workCenterRepository.save(
            wc.copy(
                name = request.name?.trim() ?: wc.name,
                description = request.description ?: wc.description,
                type = request.type ?: wc.type,
                warehouseId = request.warehouseId ?: wc.warehouseId,
                capacityPerHour = request.capacityPerHour ?: wc.capacityPerHour,
                costPerHour = request.costPerHour ?: wc.costPerHour,
                efficiencyPct = request.efficiencyPct ?: wc.efficiencyPct,
                isActive = request.isActive ?: wc.isActive,
            ),
        )
    }

    @Transactional
    fun deactivateWorkCenter(
        id: java.util.UUID,
        organizationId: java.util.UUID,
    ): WorkCenter {
        val wc = getWorkCenter(id, organizationId)
        if (!wc.isActive) {
            throw BusinessRuleException("Work center '${wc.code}' is already inactive")
        }
        return workCenterRepository.save(wc.copy(isActive = false))
    }
}
