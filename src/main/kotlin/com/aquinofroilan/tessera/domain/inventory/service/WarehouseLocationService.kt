package com.aquinofroilan.tessera.domain.inventory.service

import com.aquinofroilan.tessera.domain.inventory.dto.CreateWarehouseLocationRequest
import com.aquinofroilan.tessera.domain.inventory.model.WarehouseLocation
import com.aquinofroilan.tessera.domain.inventory.repository.WarehouseLocationRepository
import com.aquinofroilan.tessera.domain.inventory.repository.WarehouseRepository
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class WarehouseLocationService(
    private val warehouseLocationRepository: WarehouseLocationRepository,
    private val warehouseRepository: WarehouseRepository,
) {
    @Transactional
    fun createLocation(
        request: CreateWarehouseLocationRequest,
        organizationId: UUID,
    ): WarehouseLocation {
        val warehouse = warehouseRepository.findById(request.warehouseId).orElseThrow {
            ResourceNotFoundException("Warehouse not found")
        }
        if (warehouse.organizationId != organizationId) {
            throw ResourceNotFoundException("Warehouse not found")
        }

        if (warehouseLocationRepository.existsByOrganizationIdAndWarehouseIdAndCode(organizationId, request.warehouseId, request.code)) {
            throw BusinessRuleException("Location with code '${request.code}' already exists in this warehouse")
        }

        if (request.parentLocationId != null) {
            val parent = warehouseLocationRepository.findById(request.parentLocationId).orElseThrow {
                ResourceNotFoundException("Parent location not found")
            }
            if (parent.organizationId != organizationId || parent.warehouseId != request.warehouseId) {
                throw ResourceNotFoundException("Parent location not found in this warehouse")
            }
        }

        val type = request.type ?: throw BusinessRuleException("Type is required")

        val location = WarehouseLocation(
            organizationId = organizationId,
            warehouseId = request.warehouseId,
            parentLocationId = request.parentLocationId,
            code = request.code,
            type = type,
            barcode = request.barcode,
        )
        return warehouseLocationRepository.save(location)
    }

    fun listLocations(
        organizationId: UUID,
        warehouseId: UUID,
    ): List<WarehouseLocation> {
        val warehouse = warehouseRepository.findById(warehouseId).orElseThrow {
            ResourceNotFoundException("Warehouse not found")
        }
        if (warehouse.organizationId != organizationId) {
            throw ResourceNotFoundException("Warehouse not found")
        }
        return warehouseLocationRepository.findByOrganizationIdAndWarehouseId(organizationId, warehouseId)
    }
}
