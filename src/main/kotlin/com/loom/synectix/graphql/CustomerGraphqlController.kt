package com.loom.synectix.graphql

import com.loom.synectix.dto.CreateCustomerRequest
import com.loom.synectix.dto.UpdateCustomerRequest
import com.loom.synectix.exception.AuthenticationException
import com.loom.synectix.model.Customer
import com.loom.synectix.security.AuthenticationContext
import com.loom.synectix.service.CustomerService
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@Controller
class CustomerGraphqlController(
    private val customerService: CustomerService,
    private val authContext: AuthenticationContext,
) {
    @QueryMapping
    @PreAuthorize("hasAuthority('ar:read')")
    fun customers(): List<CustomerGraphql> {
        val orgId = requireOrganizationId()
        return customerService.listCustomers(orgId).map { it.toGraphql() }
    }

    @QueryMapping
    @PreAuthorize("hasAuthority('ar:read')")
    fun customer(
        @Argument id: String,
    ): CustomerGraphql {
        val orgId = requireOrganizationId()
        return customerService.getCustomer(id, orgId).toGraphql()
    }

    @MutationMapping
    @PreAuthorize("hasAuthority('ar:create')")
    fun createCustomer(
        @Argument input: CreateCustomerInput,
    ): CustomerGraphql {
        val orgId = requireOrganizationId()
        val customer =
            customerService.createCustomer(
                CreateCustomerRequest(
                    name = input.name,
                    contactName = input.contactName,
                    contactEmail = input.contactEmail,
                    contactPhone = input.contactPhone,
                    paymentTermDays = input.paymentTermDays,
                    defaultRevenueAccountId = input.defaultRevenueAccountId,
                ),
                orgId,
            )
        return customer.toGraphql()
    }

    @MutationMapping
    @PreAuthorize("hasAuthority('ar:create')")
    fun updateCustomer(
        @Argument id: String,
        @Argument input: UpdateCustomerInput,
    ): CustomerGraphql {
        val orgId = requireOrganizationId()
        val customer =
            customerService.updateCustomer(
                id,
                UpdateCustomerRequest(
                    name = input.name,
                    contactName = input.contactName,
                    contactEmail = input.contactEmail,
                    contactPhone = input.contactPhone,
                    paymentTermDays = input.paymentTermDays,
                    defaultRevenueAccountId = input.defaultRevenueAccountId,
                ),
                orgId,
            )
        return customer.toGraphql()
    }

    @MutationMapping
    @PreAuthorize("hasAuthority('ar:create')")
    fun deleteCustomer(
        @Argument id: String,
    ): CustomerGraphql {
        val orgId = requireOrganizationId()
        return customerService.deleteCustomer(id, orgId).toGraphql()
    }

    private fun requireOrganizationId(): String = authContext.organizationId() ?: throw AuthenticationException("Authentication required")

    private fun Customer.toGraphql() =
        CustomerGraphql(
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

data class CreateCustomerInput(
    @field:NotBlank(message = "Customer name is required")
    val name: String,
    val contactName: String? = null,
    @field:Email(message = "Invalid email format")
    val contactEmail: String? = null,
    val contactPhone: String? = null,
    @field:Min(value = 0, message = "Payment term days must be zero or positive")
    val paymentTermDays: Int = 30,
    val defaultRevenueAccountId: String? = null,
)

data class UpdateCustomerInput(
    val name: String? = null,
    val contactName: String? = null,
    @field:Email(message = "Invalid email format")
    val contactEmail: String? = null,
    val contactPhone: String? = null,
    @field:Min(value = 0, message = "Payment term days must be zero or positive")
    val paymentTermDays: Int? = null,
    val defaultRevenueAccountId: String? = null,
)

data class CustomerGraphql(
    val id: String,
    val name: String,
    val contactName: String?,
    val contactEmail: String?,
    val contactPhone: String?,
    val paymentTermDays: Int,
    val defaultRevenueAccountId: String?,
    val organizationId: String,
    val isActive: Boolean,
    val createdAt: String?,
    val updatedAt: String?,
)
