package com.froilan.synectix.controller

import com.froilan.synectix.annotation.LogLevel
import com.froilan.synectix.annotation.Loggable
import com.froilan.synectix.dto.CreateCustomerRequest
import com.froilan.synectix.dto.CustomerResponse
import com.froilan.synectix.dto.UpdateCustomerRequest
import com.froilan.synectix.model.Customer
import com.froilan.synectix.security.AuthenticationContext
import com.froilan.synectix.service.CustomerService
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

        return try {
            val customer = customerService.createCustomer(request, orgId)
            ResponseEntity.status(HttpStatus.CREATED).body(customer.toResponse())
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "Failed to create customer")))
        }
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

        return try {
            val customer = customerService.getCustomer(id, orgId)
            ResponseEntity.ok(customer.toResponse())
        } catch (e: IllegalArgumentException) {
            ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(mapOf("error" to (e.message ?: "Customer not found")))
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ar:create')")
    fun updateCustomer(
        @PathVariable id: String,
        @Valid @RequestBody request: UpdateCustomerRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()

        return try {
            val customer = customerService.updateCustomer(id, request, orgId)
            ResponseEntity.ok(customer.toResponse())
        } catch (e: IllegalArgumentException) {
            val status =
                if (e.message == "Customer not found") HttpStatus.NOT_FOUND else HttpStatus.BAD_REQUEST
            ResponseEntity
                .status(status)
                .body(mapOf("error" to (e.message ?: "Failed to update customer")))
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ar:create')")
    fun deleteCustomer(
        @PathVariable id: String,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()

        return try {
            val customer = customerService.deleteCustomer(id, orgId)
            ResponseEntity.ok(customer.toResponse())
        } catch (e: IllegalArgumentException) {
            val status =
                if (e.message == "Customer not found") HttpStatus.NOT_FOUND else HttpStatus.BAD_REQUEST
            ResponseEntity
                .status(status)
                .body(mapOf("error" to (e.message ?: "Failed to delete customer")))
        }
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
