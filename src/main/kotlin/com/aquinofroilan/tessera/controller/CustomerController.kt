package com.aquinofroilan.tessera.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.dto.CreateCustomerRequest
import com.aquinofroilan.tessera.dto.CustomerResponse
import com.aquinofroilan.tessera.dto.UpdateCustomerRequest
import com.aquinofroilan.tessera.model.Customer
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.service.CustomerService
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
@RequestMapping("/finance/ar/customers")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class CustomerController(
    private val customerService: CustomerService,
    private val authContext: AuthenticationContext,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('ar:create')")
    fun createCustomer(
        @Valid @RequestBody request: CreateCustomerRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()

        val customer = customerService.createCustomer(request, orgId)
        return ResponseEntity.status(HttpStatus.CREATED).body(customer.toResponse())
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ar:read')")
    fun listCustomers(): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val customers = customerService.listCustomers(orgId)
        return ResponseEntity.ok(customers.map { it.toResponse() })
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ar:read')")
    fun getCustomer(
        @PathVariable id: String,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()

        val customer = customerService.getCustomer(id, orgId)
        return ResponseEntity.ok(customer.toResponse())
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ar:create')")
    fun updateCustomer(
        @PathVariable id: String,
        @Valid @RequestBody request: UpdateCustomerRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()

        val customer = customerService.updateCustomer(id, request, orgId)
        return ResponseEntity.ok(customer.toResponse())
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ar:create')")
    fun deleteCustomer(
        @PathVariable id: String,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()

        val customer = customerService.deleteCustomer(id, orgId)
        return ResponseEntity.ok(customer.toResponse())
    }

    private fun Customer.toResponse() =
        CustomerResponse(
            id = id,
            name = name,
            contactName = contactName,
            contactEmail = contactEmail,
            contactPhone = contactPhone,
            paymentTermDays = paymentTermDays,
            defaultRevenueAccountId = defaultRevenueAccountId,
            organizationId = organizationId,
            isActive = isActive,
            createdAt = createdAt?.toString(),
            updatedAt = updatedAt?.toString(),
        )
}
