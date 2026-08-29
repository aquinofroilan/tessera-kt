package com.aquinofroilan.tessera.domain.crm.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.domain.crm.dto.CustomerPortalUserDto
import com.aquinofroilan.tessera.domain.crm.dto.LinkPortalUserRequest
import com.aquinofroilan.tessera.domain.crm.service.CustomerPortalService
import com.aquinofroilan.tessera.security.CurrentOrganizationId
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/crm/customers/{customerId}/portal-users")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class CustomerPortalAdminController(
    private val customerPortalService: CustomerPortalService,
) {
    @GetMapping
    @PreAuthorize("hasAuthority('crm:read') or hasAuthority('sales:read') or hasAuthority('organization:read') or hasRole('SUPER_ADMIN')")
    fun listPortalUsers(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable customerId: UUID,
    ): ResponseEntity<List<CustomerPortalUserDto>> = ResponseEntity.ok(customerPortalService.listPortalUsers(orgId, customerId))

    @PostMapping
    @PreAuthorize(
        "hasAuthority('crm:create') or hasAuthority('sales:create') or hasAuthority('organization:write') or hasRole('SUPER_ADMIN')",
    )
    fun linkPortalUser(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable customerId: UUID,
        @Valid @RequestBody request: LinkPortalUserRequest,
    ): ResponseEntity<CustomerPortalUserDto> {
        val linked = customerPortalService.linkPortalUser(orgId, customerId, request)
        return ResponseEntity.status(HttpStatus.CREATED).body(linked)
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize(
        "hasAuthority('crm:create') or hasAuthority('sales:create') or hasAuthority('organization:write') or hasRole('SUPER_ADMIN')",
    )
    fun unlinkPortalUser(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable customerId: UUID,
        @PathVariable userId: UUID,
    ): ResponseEntity<Void> {
        customerPortalService.unlinkPortalUser(orgId, customerId, userId)
        return ResponseEntity.noContent().build()
    }
}
