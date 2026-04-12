package com.froilan.synectix.service

import com.froilan.synectix.dto.CreateCustomerRequest
import com.froilan.synectix.dto.UpdateCustomerRequest
import com.froilan.synectix.model.Customer
import com.froilan.synectix.repository.CustomerRepository
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CustomerService(
    private val customerRepository: CustomerRepository,
) {
    @Transactional
    fun createCustomer(
        request: CreateCustomerRequest,
        organizationId: String,
    ): Customer {
        val customer =
            Customer(
                name = request.name,
                contactName = request.contactName,
                contactEmail = request.contactEmail,
                contactPhone = request.contactPhone,
                paymentTermDays = request.paymentTermDays,
                defaultRevenueAccountId = request.defaultRevenueAccountId,
                organizationId = organizationId,
            )

        return try {
            customerRepository.save(customer)
        } catch (e: DuplicateKeyException) {
            throw IllegalArgumentException(
                "Customer '${request.name}' already exists in this organization",
                e,
            )
        }
    }

    fun getCustomer(
        customerId: String,
        organizationId: String,
    ): Customer {
        val customer =
            customerRepository.findById(customerId).orElseThrow {
                IllegalArgumentException("Customer not found")
            }
        if (customer.organizationId != organizationId) {
            throw IllegalArgumentException("Customer not found")
        }
        return customer
    }

    fun listCustomers(organizationId: String): List<Customer> =
        customerRepository.findByOrganizationIdAndIsActive(organizationId, true)

    @Transactional
    fun updateCustomer(
        customerId: String,
        request: UpdateCustomerRequest,
        organizationId: String,
    ): Customer {
        val customer = getCustomer(customerId, organizationId)

        if (!customer.isActive) {
            throw IllegalArgumentException("Cannot update inactive customer")
        }

        if (request.name != null && request.name.isBlank()) {
            throw IllegalArgumentException("Customer name cannot be blank")
        }

        val updated =
            customer.copy(
                name = request.name ?: customer.name,
                contactName = request.contactName ?: customer.contactName,
                contactEmail = request.contactEmail ?: customer.contactEmail,
                contactPhone = request.contactPhone ?: customer.contactPhone,
                paymentTermDays = request.paymentTermDays ?: customer.paymentTermDays,
                defaultRevenueAccountId = request.defaultRevenueAccountId ?: customer.defaultRevenueAccountId,
            )

        return try {
            customerRepository.save(updated)
        } catch (e: DuplicateKeyException) {
            throw IllegalArgumentException(
                "Customer '${updated.name}' already exists in this organization",
                e,
            )
        }
    }

    @Transactional
    fun deleteCustomer(
        customerId: String,
        organizationId: String,
    ): Customer {
        val customer = getCustomer(customerId, organizationId)

        if (!customer.isActive) {
            throw IllegalArgumentException("Customer is already inactive")
        }

        return customerRepository.save(customer.copy(isActive = false))
    }
}
