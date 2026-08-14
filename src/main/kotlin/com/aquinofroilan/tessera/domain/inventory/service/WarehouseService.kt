package com.aquinofroilan.tessera.domain.inventory.service

import com.aquinofroilan.tessera.domain.inventory.dto.CreateWarehouseRequest
import com.aquinofroilan.tessera.domain.inventory.dto.UpdateWarehouseRequest
import com.aquinofroilan.tessera.domain.inventory.model.Warehouse
import com.aquinofroilan.tessera.domain.inventory.repository.WarehouseRepository
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class WarehouseService(
    private val warehouseRepository: WarehouseRepository,
) {
    @Transactional
    fun createWarehouse(
        request: CreateWarehouseRequest,
        organizationId: java.util.UUID,
    ): Warehouse {
        val warehouse =
            Warehouse(
                code = request.code,
                name = request.name,
                description = request.description,
                addressLine = request.addressLine,
                city = request.city,
                country = request.country,
                allowNegativeStock = request.allowNegativeStock ?: false,
                organizationId = organizationId,
            )
        return try {
            warehouseRepository.save(warehouse)
        } catch (e: DuplicateKeyException) {
            throw BusinessRuleException(
                "Warehouse with code '${request.code}' already exists in this organization",
                e,
            )
        }
    }

    fun getWarehouse(
        warehouseId: java.util.UUID,
        organizationId: java.util.UUID,
    ): Warehouse {
        val warehouse =
            warehouseRepository.findById(warehouseId).orElseThrow {
                ResourceNotFoundException("Warehouse not found")
            }
        if (warehouse.organizationId != organizationId) {
            throw ResourceNotFoundException("Warehouse not found")
        }
        return warehouse
    }

    fun listWarehouses(
        organizationId: java.util.UUID,
        isActive: Boolean = true,
        search: String? = null,
    ): List<Warehouse> = warehouseRepository.search(organizationId, isActive, search)

    @Transactional
    fun updateWarehouse(
        warehouseId: java.util.UUID,
        request: UpdateWarehouseRequest,
        organizationId: java.util.UUID,
    ): Warehouse {
        val existing = getWarehouse(warehouseId, organizationId)
        if (!existing.isActive) {
            throw BusinessRuleException("Cannot update inactive warehouse")
        }
        existing.apply {
            name = request.name ?: existing.name
            description = request.description ?: existing.description
            addressLine = request.addressLine ?: existing.addressLine
            city = request.city ?: existing.city
            country = request.country ?: existing.country
            allowNegativeStock = request.allowNegativeStock ?: existing.allowNegativeStock
        }
        return warehouseRepository.save(existing)
    }

    @Transactional
    fun deleteWarehouse(
        warehouseId: java.util.UUID,
        organizationId: java.util.UUID,
    ): Warehouse {
        val warehouse = getWarehouse(warehouseId, organizationId)
        if (!warehouse.isActive) {
            throw BusinessRuleException("Warehouse is already inactive")
        }
        // TODO: when #41 lands, block soft delete if warehouse has open stock or movements.
        warehouse.isActive = false
        return warehouseRepository.save(warehouse)
    }
}
