package com.aquinofroilan.tessera.domain.procurement.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.domain.procurement.dto.CreateVendorRequest
import com.aquinofroilan.tessera.domain.procurement.dto.UpdateVendorRequest
import com.aquinofroilan.tessera.domain.procurement.dto.VendorResponse
import com.aquinofroilan.tessera.domain.procurement.model.Vendor
import com.aquinofroilan.tessera.domain.procurement.service.VendorService
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.security.CurrentOrganizationId
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
import java.util.UUID

@RestController
@RequestMapping("/api/v1/procurement/vendors")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class VendorController(
    private val vendorService: VendorService,
    private val authContext: AuthenticationContext,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('ap:create')")
    fun createVendor(
        @CurrentOrganizationId orgId: UUID,
        @Valid @RequestBody request: CreateVendorRequest,
    ): ResponseEntity<Any> {
        val vendor = vendorService.createVendor(request, orgId)
        return ResponseEntity.status(HttpStatus.CREATED).body(vendor.toResponse())
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ap:read')")
    fun listVendors(
        @CurrentOrganizationId orgId: UUID,
    ): ResponseEntity<Any> {
        val vendors = vendorService.listVendors(orgId)
        return ResponseEntity.ok(vendors.map { it.toResponse() })
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ap:read')")
    fun getVendor(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> {
        val vendor = vendorService.getVendor(id, orgId)
        return ResponseEntity.ok(vendor.toResponse())
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ap:create')")
    fun updateVendor(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: java.util.UUID,
        @Valid @RequestBody request: UpdateVendorRequest,
    ): ResponseEntity<Any> {
        val vendor = vendorService.updateVendor(id, request, orgId)
        return ResponseEntity.ok(vendor.toResponse())
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ap:create')")
    fun deleteVendor(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> {
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
