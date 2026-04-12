package com.froilan.synectix.service

import com.froilan.synectix.dto.CreateCustomerRequest
import com.froilan.synectix.dto.UpdateCustomerRequest
import com.froilan.synectix.exception.BusinessRuleException
import com.froilan.synectix.exception.ResourceNotFoundException
import com.froilan.synectix.model.Customer
import com.froilan.synectix.repository.CustomerRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import java.util.Optional

class CustomerServiceTest {
    private lateinit var customerService: CustomerService
    private lateinit var customerRepository: CustomerRepository

    private val orgId = "org-123"

    @BeforeEach
    fun setup() {
        customerRepository = mock(CustomerRepository::class.java)
        customerService = CustomerService(customerRepository)
    }

    @Test
    fun `create should save customer with correct fields`() {
        `when`(customerRepository.save(any<Customer>())).thenAnswer { it.arguments[0] }

        val request =
            CreateCustomerRequest(
                name = "BigCorp",
                contactName = "Jane Doe",
                contactEmail = "jane@bigcorp.com",
                paymentTermDays = 45,
            )

        val result = customerService.createCustomer(request, orgId)

        assertThat(result.name).isEqualTo("BigCorp")
        assertThat(result.contactName).isEqualTo("Jane Doe")
        assertThat(result.contactEmail).isEqualTo("jane@bigcorp.com")
        assertThat(result.paymentTermDays).isEqualTo(45)
        assertThat(result.organizationId).isEqualTo(orgId)
        assertThat(result.isActive).isTrue()
    }

    @Test
    fun `get should return customer for correct org`() {
        val customer = createCustomer()
        `when`(customerRepository.findById("c-1")).thenReturn(Optional.of(customer))

        val result = customerService.getCustomer("c-1", orgId)
        assertThat(result.id).isEqualTo("c-1")
    }

    @Test
    fun `get should throw when customer belongs to different org`() {
        val customer = createCustomer(orgId = "other-org")
        `when`(customerRepository.findById("c-1")).thenReturn(Optional.of(customer))

        val exception =
            assertThrows<ResourceNotFoundException> {
                customerService.getCustomer("c-1", orgId)
            }
        assertThat(exception.message).contains("Customer not found")
    }

    @Test
    fun `list should return active customers`() {
        val customers = listOf(createCustomer(), createCustomer(id = "c-2", name = "SmallCo"))
        `when`(customerRepository.findByOrganizationIdAndIsActive(orgId, true)).thenReturn(customers)

        val result = customerService.listCustomers(orgId)
        assertThat(result).hasSize(2)
    }

    @Test
    fun `update should apply partial changes`() {
        val customer = createCustomer()
        `when`(customerRepository.findById("c-1")).thenReturn(Optional.of(customer))
        `when`(customerRepository.save(any<Customer>())).thenAnswer { it.arguments[0] }

        val request = UpdateCustomerRequest(name = "Updated Corp")
        val result = customerService.updateCustomer("c-1", request, orgId)

        assertThat(result.name).isEqualTo("Updated Corp")
        assertThat(result.contactName).isEqualTo("Jane Doe")
    }

    @Test
    fun `update should reject inactive customer`() {
        val customer = createCustomer(isActive = false)
        `when`(customerRepository.findById("c-1")).thenReturn(Optional.of(customer))

        val exception =
            assertThrows<BusinessRuleException> {
                customerService.updateCustomer("c-1", UpdateCustomerRequest(name = "New"), orgId)
            }
        assertThat(exception.message).contains("inactive")
    }

    @Test
    fun `update should reject blank name`() {
        val customer = createCustomer()
        `when`(customerRepository.findById("c-1")).thenReturn(Optional.of(customer))

        val exception =
            assertThrows<BusinessRuleException> {
                customerService.updateCustomer("c-1", UpdateCustomerRequest(name = "  "), orgId)
            }
        assertThat(exception.message).contains("blank")
    }

    @Test
    fun `delete should soft delete customer`() {
        val customer = createCustomer()
        `when`(customerRepository.findById("c-1")).thenReturn(Optional.of(customer))
        `when`(customerRepository.save(any<Customer>())).thenAnswer { it.arguments[0] }

        val result = customerService.deleteCustomer("c-1", orgId)

        assertThat(result.isActive).isFalse()
        val captor = argumentCaptor<Customer>()
        verify(customerRepository).save(captor.capture())
        assertThat(captor.firstValue.isActive).isFalse()
    }

    @Test
    fun `delete should reject already inactive customer`() {
        val customer = createCustomer(isActive = false)
        `when`(customerRepository.findById("c-1")).thenReturn(Optional.of(customer))

        val exception =
            assertThrows<BusinessRuleException> {
                customerService.deleteCustomer("c-1", orgId)
            }
        assertThat(exception.message).contains("already inactive")
    }

    private fun createCustomer(
        id: String = "c-1",
        name: String = "BigCorp",
        orgId: String = this.orgId,
        isActive: Boolean = true,
    ) = Customer(
        id = id,
        name = name,
        contactName = "Jane Doe",
        contactEmail = "jane@bigcorp.com",
        paymentTermDays = 30,
        organizationId = orgId,
        isActive = isActive,
    )
}
