package com.froilan.synectix.service

import com.froilan.synectix.dto.CreateVendorRequest
import com.froilan.synectix.dto.UpdateVendorRequest
import com.froilan.synectix.model.Vendor
import com.froilan.synectix.repository.VendorRepository
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
        organizationId: String,
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
            throw IllegalArgumentException(
                "Vendor '${request.name}' already exists in this organization",
            )
        }
    }

    fun getVendor(
        vendorId: String,
        organizationId: String,
    ): Vendor {
        val vendor =
            vendorRepository.findById(vendorId).orElseThrow {
                IllegalArgumentException("Vendor not found")
            }
        if (vendor.organizationId != organizationId) {
            throw IllegalArgumentException("Vendor not found")
        }
        return vendor
    }

    fun listVendors(organizationId: String): List<Vendor> =
        vendorRepository.findByOrganizationIdAndIsActive(organizationId, true)

    @Transactional
    fun updateVendor(
        vendorId: String,
        request: UpdateVendorRequest,
        organizationId: String,
    ): Vendor {
        val vendor = getVendor(vendorId, organizationId)

        if (!vendor.isActive) {
            throw IllegalArgumentException("Cannot update inactive vendor")
        }

        val updated =
            vendor.copy(
                name = request.name ?: vendor.name,
                contactName = request.contactName ?: vendor.contactName,
                contactEmail = request.contactEmail ?: vendor.contactEmail,
                contactPhone = request.contactPhone ?: vendor.contactPhone,
                paymentTermDays = request.paymentTermDays ?: vendor.paymentTermDays,
                defaultExpenseAccountId = request.defaultExpenseAccountId ?: vendor.defaultExpenseAccountId,
            )

        return try {
            vendorRepository.save(updated)
        } catch (e: DuplicateKeyException) {
            throw IllegalArgumentException(
                "Vendor '${updated.name}' already exists in this organization",
            )
        }
    }

    @Transactional
    fun deleteVendor(
        vendorId: String,
        organizationId: String,
    ): Vendor {
        val vendor = getVendor(vendorId, organizationId)

        if (!vendor.isActive) {
            throw IllegalArgumentException("Vendor is already inactive")
        }

        return vendorRepository.save(vendor.copy(isActive = false))
    }
}
