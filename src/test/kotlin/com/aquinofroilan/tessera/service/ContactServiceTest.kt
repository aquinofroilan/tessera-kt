package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.CreateContactRequest
import com.aquinofroilan.tessera.dto.UpdateContactRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.model.Contact
import com.aquinofroilan.tessera.model.Customer
import com.aquinofroilan.tessera.repository.ContactRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.util.Optional
import java.util.UUID

class ContactServiceTest {
    private lateinit var repository: ContactRepository
    private lateinit var customerService: CustomerService
    private lateinit var service: ContactService

    private val orgId = UUID.randomUUID()
    private val userId = UUID.randomUUID()

    @BeforeEach
    fun setup() {
        repository = mock(ContactRepository::class.java)
        customerService = mock(CustomerService::class.java)
        whenever(repository.save(any<Contact>())).thenAnswer { it.arguments[0] }
        whenever(customerService.getCustomer(any(), any())).thenReturn(
            Customer(id = UUID.fromString("00000000-0000-0000-0000-000000000001"), name = "Acme", organizationId = orgId),
        )
        service = ContactService(repository, customerService)
    }

    @Test
    fun `create persists a contact and trims whitespace`() {
        val c =
            service.createContact(
                CreateContactRequest(
                    customerId = UUID.fromString("00000000-0000-0000-0000-000000000001"),
                    firstName = " Ada ",
                    lastName = " Lovelace ",
                    email = " ada@example.com ",
                ),
                orgId,
                userId,
            )
        assertThat(c.firstName).isEqualTo("Ada")
        assertThat(c.lastName).isEqualTo("Lovelace")
        assertThat(c.email).isEqualTo("ada@example.com")
        assertThat(c.isActive).isTrue()
    }

    @Test
    fun `create allows just lastName`() {
        val c =
            service.createContact(
                CreateContactRequest(firstName = null, lastName = "Cher"),
                orgId,
                userId,
            )
        assertThat(c.firstName).isEmpty()
        assertThat(c.lastName).isEqualTo("Cher")
    }

    @Test
    fun `create rejects when both names are blank`() {
        assertThatThrownBy {
            service.createContact(CreateContactRequest(firstName = "  ", lastName = ""), orgId, userId)
        }.isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `deactivate rejects double-deactivation`() {
        val c1Id = UUID.randomUUID()
        whenever(repository.findById(c1Id)).thenReturn(
            Optional.of(
                Contact(
                    id = c1Id,
                    organizationId = orgId,
                    firstName = "A",
                    lastName = "B",
                    isActive = false,
                    createdBy = userId,
                ),
            ),
        )
        assertThatThrownBy { service.deactivateContact(c1Id, orgId) }
            .isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `update validates new customer when reassigning`() {
        val c1Id = UUID.randomUUID()
        val custOld = UUID.randomUUID()
        val custNew = UUID.randomUUID()
        whenever(repository.findById(c1Id)).thenReturn(
            Optional.of(
                Contact(
                    id = c1Id,
                    organizationId = orgId,
                    customerId = custOld,
                    firstName = "A",
                    lastName = "B",
                    createdBy = userId,
                ),
            ),
        )
        val updated =
            service.updateContact(
                c1Id,
                UpdateContactRequest(customerId = custNew),
                orgId,
            )
        assertThat(updated.customerId).isEqualTo(custNew)
    }
}
