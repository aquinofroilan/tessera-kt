package com.aquinofroilan.tessera.domain.procurement.service

import com.aquinofroilan.tessera.domain.procurement.dto.CreateVendorRequest
import com.aquinofroilan.tessera.domain.procurement.dto.UpdateVendorRequest
import com.aquinofroilan.tessera.domain.procurement.model.Vendor
import com.aquinofroilan.tessera.domain.procurement.repository.VendorRepository
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class VendorService(
    private val vendorRepository: VendorRepository,
) {
    @Transactional
    fun createVendor(
        request: CreateVendorRequest,
        organizationId: java.util.UUID,
    ): Vendor {
        val vendor =
            Vendor(
                name = request.name,
                contactName = request.contactName,
                contactEmail = request.contactEmail,
                contactPhone = request.contactPhone,
                paymentTermDays = request.paymentTermDays,
                defaultExpenseAccountId = request.defaultExpenseAccountId,
                organizationId = organizationId,
            )

        return try {
            vendorRepository.save(vendor)
        } catch (e: DuplicateKeyException) {
            throw BusinessRuleException(
                "Vendor '${request.name}' already exists in this organization",
                e,
            )
        }
    }

    fun getVendor(
        vendorId: java.util.UUID,
        organizationId: java.util.UUID,
    ): Vendor {
        val vendor =
            vendorRepository.findById(vendorId).orElseThrow {
                ResourceNotFoundException("Vendor not found")
            }
        if (vendor.organizationId != organizationId) {
            throw ResourceNotFoundException("Vendor not found")
        }
        return vendor
    }

    fun listVendors(organizationId: java.util.UUID): List<Vendor> = vendorRepository.findByOrganizationIdAndIsActive(organizationId, true)

    @Transactional
    fun updateVendor(
        vendorId: java.util.UUID,
        request: UpdateVendorRequest,
        organizationId: java.util.UUID,
    ): Vendor {
        val vendor = getVendor(vendorId, organizationId)

        if (!vendor.isActive) {
            throw BusinessRuleException("Cannot update inactive vendor")
        }

        if (request.name != null && request.name.isBlank()) {
            throw BusinessRuleException("Vendor name cannot be blank")
        }

        vendor.apply {
            name = request.name ?: vendor.name
            contactName = request.contactName ?: vendor.contactName
            contactEmail = request.contactEmail ?: vendor.contactEmail
            contactPhone = request.contactPhone ?: vendor.contactPhone
            paymentTermDays = request.paymentTermDays ?: vendor.paymentTermDays
            defaultExpenseAccountId = request.defaultExpenseAccountId ?: vendor.defaultExpenseAccountId
        }
        return try {
            vendorRepository.save(vendor)
        } catch (e: DuplicateKeyException) {
            throw BusinessRuleException(
                "Vendor '${vendor.name}' already exists in this organization",
                e,
            )
        }
    }

    @Transactional
    fun deleteVendor(
        vendorId: java.util.UUID,
        organizationId: java.util.UUID,
    ): Vendor {
        val vendor = getVendor(vendorId, organizationId)

        if (!vendor.isActive) {
            throw BusinessRuleException("Vendor is already inactive")
        }

        vendor.isActive = false
        return vendorRepository.save(vendor)
    }
}
