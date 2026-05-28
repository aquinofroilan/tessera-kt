package com.loom.synectix.controller

import com.loom.synectix.annotation.LogLevel
import com.loom.synectix.annotation.Loggable
import com.loom.synectix.dto.CreateVendorRequest
import com.loom.synectix.dto.UpdateVendorRequest
import com.loom.synectix.dto.VendorResponse
import com.loom.synectix.model.Vendor
import com.loom.synectix.security.AuthenticationContext
import com.loom.synectix.service.VendorService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
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
    private val authContext: AuthenticationContext,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('ap:create')")
    fun createVendor(
        @Valid @RequestBody request: CreateVendorRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val vendor = vendorService.createVendor(request, orgId)
        return ResponseEntity.status(HttpStatus.CREATED).body(vendor.toResponse())
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ap:read')")
    fun listVendors(): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val vendors = vendorService.listVendors(orgId)
        return ResponseEntity.ok(vendors.map { it.toResponse() })
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ap:read')")
    fun getVendor(
        @PathVariable id: String,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val vendor = vendorService.getVendor(id, orgId)
        return ResponseEntity.ok(vendor.toResponse())
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ap:create')")
    fun updateVendor(
        @PathVariable id: String,
        @Valid @RequestBody request: UpdateVendorRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val vendor = vendorService.updateVendor(id, request, orgId)
        return ResponseEntity.ok(vendor.toResponse())
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ap:create')")
    fun deleteVendor(
        @PathVariable id: String,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val vendor = vendorService.deleteVendor(id, orgId)
        return ResponseEntity.ok(vendor.toResponse())
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
}
