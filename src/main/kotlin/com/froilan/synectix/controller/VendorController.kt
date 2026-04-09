package com.froilan.synectix.controller

import com.froilan.synectix.annotation.LogLevel
import com.froilan.synectix.annotation.Loggable
import com.froilan.synectix.dto.CreateVendorRequest
import com.froilan.synectix.dto.UpdateVendorRequest
import com.froilan.synectix.dto.VendorResponse
import com.froilan.synectix.model.Vendor
import com.froilan.synectix.security.ApiKeyContext
import com.froilan.synectix.security.SessionContext
import com.froilan.synectix.service.VendorService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/finance/ap/vendors")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class VendorController(
    private val vendorService: VendorService,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('ap:create')")
    fun createVendor(
        @Valid @RequestBody request: CreateVendorRequest,
    ): ResponseEntity<Any> {
        val orgId = extractOrganizationId() ?: return unauthorized()

        return try {
            val vendor = vendorService.createVendor(request, orgId)
            ResponseEntity.status(HttpStatus.CREATED).body(vendor.toResponse())
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "Failed to create vendor")))
        }
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ap:read')")
    fun listVendors(): ResponseEntity<Any> {
        val orgId = extractOrganizationId() ?: return unauthorized()
        val vendors = vendorService.listVendors(orgId)
        return ResponseEntity.ok(vendors.map { it.toResponse() })
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ap:read')")
    fun getVendor(
        @PathVariable id: String,
    ): ResponseEntity<Any> {
        val orgId = extractOrganizationId() ?: return unauthorized()

        return try {
            val vendor = vendorService.getVendor(id, orgId)
            ResponseEntity.ok(vendor.toResponse())
        } catch (e: IllegalArgumentException) {
            ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to (e.message ?: "Vendor not found")))
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ap:create')")
    fun updateVendor(
        @PathVariable id: String,
        @Valid @RequestBody request: UpdateVendorRequest,
    ): ResponseEntity<Any> {
        val orgId = extractOrganizationId() ?: return unauthorized()

        return try {
            val vendor = vendorService.updateVendor(id, request, orgId)
            ResponseEntity.ok(vendor.toResponse())
        } catch (e: IllegalArgumentException) {
            val status =
                if (e.message == "Vendor not found") HttpStatus.NOT_FOUND else HttpStatus.BAD_REQUEST
            ResponseEntity
                .status(status)
                .body(mapOf("error" to (e.message ?: "Failed to update vendor")))
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ap:create')")
    fun deleteVendor(
        @PathVariable id: String,
    ): ResponseEntity<Any> {
        val orgId = extractOrganizationId() ?: return unauthorized()

        return try {
            val vendor = vendorService.deleteVendor(id, orgId)
            ResponseEntity.ok(vendor.toResponse())
        } catch (e: IllegalArgumentException) {
            val status =
                if (e.message == "Vendor not found") HttpStatus.NOT_FOUND else HttpStatus.BAD_REQUEST
            ResponseEntity
                .status(status)
                .body(mapOf("error" to (e.message ?: "Failed to delete vendor")))
        }
    }

    private fun Vendor.toResponse() =
        VendorResponse(
            id = id,
            name = name,
            contactName = contactName,
            contactEmail = contactEmail,
            contactPhone = contactPhone,
            paymentTermDays = paymentTermDays,
            defaultExpenseAccountId = defaultExpenseAccountId,
            organizationId = organizationId,
            isActive = isActive,
            createdAt = createdAt?.toString(),
            updatedAt = updatedAt?.toString(),
        )

    private fun extractOrganizationId(): String? {
        val authentication = SecurityContextHolder.getContext().authentication ?: return null
        return when (val details = authentication.details) {
            is SessionContext -> details.organizationId
            is ApiKeyContext -> details.organizationId
            else -> null
        }
    }

    private fun unauthorized(): ResponseEntity<Any> =
        ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(mapOf("error" to "Authentication required"))
}
