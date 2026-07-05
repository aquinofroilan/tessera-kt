package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.CreateCustomerRequest
import com.aquinofroilan.tessera.dto.UpdateCustomerRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import com.aquinofroilan.tessera.model.Customer
import com.aquinofroilan.tessera.repository.CustomerRepository
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
            throw BusinessRuleException(
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
                ResourceNotFoundException("Customer not found")
            }
        if (customer.organizationId != organizationId) {
            throw ResourceNotFoundException("Customer not found")
        }
        return customer
    }

    fun listCustomers(organizationId: String): List<Customer> = customerRepository.findByOrganizationIdAndIsActive(organizationId, true)

    @Transactional
    fun updateCustomer(
        customerId: String,
        request: UpdateCustomerRequest,
        organizationId: String,
    ): Customer {
        val customer = getCustomer(customerId, organizationId)

        if (!customer.isActive) {
            throw BusinessRuleException("Cannot update inactive customer")
        }

        if (request.name != null && request.name.isBlank()) {
            throw BusinessRuleException("Customer name cannot be blank")
        }

        customer.apply {
            name = request.name ?: customer.name
            contactName = request.contactName ?: customer.contactName
            contactEmail = request.contactEmail ?: customer.contactEmail
            contactPhone = request.contactPhone ?: customer.contactPhone
            paymentTermDays = request.paymentTermDays ?: customer.paymentTermDays
            defaultRevenueAccountId = request.defaultRevenueAccountId ?: customer.defaultRevenueAccountId
        }
        return try {
            customerRepository.save(customer)
        } catch (e: DuplicateKeyException) {
            throw BusinessRuleException(
                "Customer '${customer.name}' already exists in this organization",
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
            throw BusinessRuleException("Customer is already inactive")
        }

        customer.isActive = false
        return customerRepository.save(customer)
    }
}
