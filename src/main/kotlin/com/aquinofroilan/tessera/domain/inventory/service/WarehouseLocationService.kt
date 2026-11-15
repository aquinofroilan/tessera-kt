package com.aquinofroilan.tessera.domain.inventory.service

import com.aquinofroilan.tessera.domain.inventory.dto.CreateWarehouseLocationRequest
import com.aquinofroilan.tessera.domain.inventory.dto.UpdateWarehouseLocationRequest
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
        val warehouse =
            warehouseRepository.findById(request.warehouseId).orElseThrow {
                ResourceNotFoundException("Warehouse not found")
            }
        if (warehouse.organizationId != organizationId) {
            throw ResourceNotFoundException("Warehouse not found")
        }

        if (warehouseLocationRepository.existsByOrganizationIdAndWarehouseIdAndCode(organizationId, request.warehouseId, request.code)) {
            throw BusinessRuleException("Location with code '${request.code}' already exists in this warehouse")
        }

        if (request.parentLocationId != null) {
            val parent = loadLocation(request.parentLocationId, organizationId)
            validateSameWarehouse(parent, request.warehouseId)
            if (!parent.isActive) throw BusinessRuleException("Cannot create a location under an inactive parent")
        }

        val type = request.type ?: throw BusinessRuleException("Type is required")

        val location =
            WarehouseLocation(
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
        activeOnly: Boolean = true,
    ): List<WarehouseLocation> {
        validateWarehouse(warehouseId, organizationId)
        return if (activeOnly) {
            warehouseLocationRepository.findByOrganizationIdAndWarehouseIdAndIsActiveTrue(organizationId, warehouseId)
        } else {
            warehouseLocationRepository.findByOrganizationIdAndWarehouseId(organizationId, warehouseId)
        }
    }

    fun getLocation(
        id: UUID,
        organizationId: UUID,
    ): WarehouseLocation = loadLocation(id, organizationId)

    @Transactional
    fun updateLocation(
        id: UUID,
        request: UpdateWarehouseLocationRequest,
        organizationId: UUID,
    ): WarehouseLocation {
        val location = loadLocation(id, organizationId)

        request.code?.let { code ->
            if (code.isBlank()) throw BusinessRuleException("Location code cannot be blank")
            if (code != location.code &&
                warehouseLocationRepository.existsByOrganizationIdAndWarehouseIdAndCodeAndIdNot(
                    organizationId,
                    location.warehouseId,
                    code,
                    location.id,
                )
            ) {
                throw BusinessRuleException("Location with code '$code' already exists in this warehouse")
            }
            location.code = code
        }

        if (request.parentLocationIdProvided) {
            location.parentLocationId = request.parentLocationId?.let { parentId ->
                validateParent(location, parentId, organizationId)
                parentId
            }
        }

        request.type?.let { location.type = it }
        if (request.barcode != null) location.barcode = request.barcode

        if (request.isActive == false) ensureNoActiveChildren(location, organizationId)
        if (request.isActive == true && location.parentLocationId != null) {
            val parent = loadLocation(location.parentLocationId!!, organizationId)
            if (!parent.isActive) throw BusinessRuleException("Cannot activate a location under an inactive parent")
        }
        request.isActive?.let { location.isActive = it }
        return warehouseLocationRepository.save(location)
    }

    @Transactional
    fun deactivateLocation(
        id: UUID,
        organizationId: UUID,
    ): WarehouseLocation {
        val location = loadLocation(id, organizationId)
        if (!location.isActive) return location
        ensureNoActiveChildren(location, organizationId)
        location.isActive = false
        return warehouseLocationRepository.save(location)
    }

    private fun validateParent(
        location: WarehouseLocation,
        parentId: UUID,
        organizationId: UUID,
    ) {
        if (parentId == location.id) throw BusinessRuleException("A location cannot be its own parent")
        val visited = mutableSetOf<UUID>()
        var ancestor: WarehouseLocation? = loadLocation(parentId, organizationId)
        while (ancestor != null) {
            validateSameWarehouse(ancestor, location.warehouseId)
            if (!ancestor.isActive) throw BusinessRuleException("Cannot assign an inactive parent location")
            if (ancestor.id == location.id) throw BusinessRuleException("Reparenting would create a location hierarchy cycle")
            if (!visited.add(ancestor.id)) throw BusinessRuleException("The existing location hierarchy contains a cycle")
            ancestor = ancestor.parentLocationId?.let { loadLocation(it, organizationId) }
        }
    }

    private fun ensureNoActiveChildren(
        location: WarehouseLocation,
        organizationId: UUID,
    ) {
        if (warehouseLocationRepository.existsByOrganizationIdAndWarehouseIdAndParentLocationIdAndIsActiveTrue(
                organizationId,
                location.warehouseId,
                location.id,
            )
        ) {
            throw BusinessRuleException("Deactivate or reparent active child locations first")
        }
    }

    private fun loadLocation(
        id: UUID,
        organizationId: UUID,
    ): WarehouseLocation {
        val location =
            warehouseLocationRepository.findById(id).orElseThrow {
                ResourceNotFoundException("Location not found")
            }
        if (location.organizationId != organizationId) throw ResourceNotFoundException("Location not found")
        return location
    }

    private fun validateWarehouse(
        warehouseId: UUID,
        organizationId: UUID,
    ) {
        val warehouse =
            warehouseRepository.findById(warehouseId).orElseThrow {
                ResourceNotFoundException("Warehouse not found")
            }
        if (warehouse.organizationId != organizationId) throw ResourceNotFoundException("Warehouse not found")
    }

    private fun validateSameWarehouse(
        location: WarehouseLocation,
        warehouseId: UUID,
    ) {
        if (location.warehouseId != warehouseId) {
            throw ResourceNotFoundException("Location not found in this warehouse")
        }
    }
}
