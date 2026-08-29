package com.aquinofroilan.tessera.domain.crm.service

import com.aquinofroilan.tessera.domain.auth.repository.UserRepository
import com.aquinofroilan.tessera.domain.crm.dto.AddTicketMessageRequest
import com.aquinofroilan.tessera.domain.crm.dto.CreateSupportTicketRequest
import com.aquinofroilan.tessera.domain.crm.model.SupportTicket
import com.aquinofroilan.tessera.domain.crm.model.SupportTicketMessage
import com.aquinofroilan.tessera.domain.crm.model.TicketCategory
import com.aquinofroilan.tessera.domain.crm.model.TicketPriority
import com.aquinofroilan.tessera.domain.crm.model.TicketSenderType
import com.aquinofroilan.tessera.domain.crm.model.TicketStatus
import com.aquinofroilan.tessera.domain.crm.repository.ContactRepository
import com.aquinofroilan.tessera.domain.crm.repository.SupportTicketMessageRepository
import com.aquinofroilan.tessera.domain.crm.repository.SupportTicketRepository
import com.aquinofroilan.tessera.domain.sales.model.Customer
import com.aquinofroilan.tessera.domain.sales.repository.CustomerRepository
import com.aquinofroilan.tessera.exception.BusinessRuleException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import java.util.Optional
import java.util.UUID

class SupportTicketServiceTest {
    private lateinit var supportTicketRepository: SupportTicketRepository
    private lateinit var supportTicketMessageRepository: SupportTicketMessageRepository
    private lateinit var customerRepository: CustomerRepository
    private lateinit var contactRepository: ContactRepository
    private lateinit var userRepository: UserRepository
    private lateinit var service: SupportTicketService

    private val orgId = UUID.fromString("4abe9f6d-6df3-6e5c-953e-3695db9a5216")
    private val userId = UUID.fromString("bc17c97c-3d89-7d43-b7e0-7ca0266eafa8")
    private val customerId = UUID.fromString("11111111-2222-3333-4444-555555555555")
    private val ticketId = UUID.fromString("22222222-3333-4444-5555-666666666666")

    @BeforeEach
    fun setUp() {
        supportTicketRepository = mock(SupportTicketRepository::class.java)
        supportTicketMessageRepository = mock(SupportTicketMessageRepository::class.java)
        customerRepository = mock(CustomerRepository::class.java)
        contactRepository = mock(ContactRepository::class.java)
        userRepository = mock(UserRepository::class.java)
        service =
            SupportTicketService(
                supportTicketRepository,
                supportTicketMessageRepository,
                customerRepository,
                contactRepository,
                userRepository,
            )
    }

    private fun createCustomer() =
        Customer(
            id = customerId,
            name = "Acme Corp",
            organizationId = orgId,
        )

    private fun createTicket(status: TicketStatus = TicketStatus.OPEN): SupportTicket =
        SupportTicket(
            id = ticketId,
            organizationId = orgId,
            ticketNumber = 1,
            customerId = customerId,
            subject = "Cannot access invoice",
            description = "Error when viewing PDF",
            status = status,
            priority = TicketPriority.HIGH,
            category = TicketCategory.BILLING,
            createdByUserId = userId,
            messages =
                mutableListOf(
                    SupportTicketMessage(
                        organizationId = orgId,
                        ticketId = ticketId,
                        senderId = userId,
                        senderType = TicketSenderType.CUSTOMER,
                        message = "Error when viewing PDF",
                        isInternalNote = false,
                    ),
                ),
        )

    @Test
    fun `createTicket creates ticket in OPEN status with initial message`() {
        val request =
            CreateSupportTicketRequest(
                customerId = customerId,
                subject = "Cannot access invoice",
                description = "Error when viewing PDF",
                priority = TicketPriority.HIGH,
                category = TicketCategory.BILLING,
            )

        `when`(customerRepository.findByIdAndOrganizationId(customerId, orgId)).thenReturn(Optional.of(createCustomer()))
        `when`(supportTicketRepository.findMaxTicketNumberByOrganizationId(orgId)).thenReturn(0)
        `when`(supportTicketRepository.save(any<SupportTicket>())).thenAnswer { it.arguments[0] }

        val response = service.createTicket(orgId, userId, request, senderType = TicketSenderType.CUSTOMER)

        assertEquals(1, response.ticketNumber)
        assertEquals(TicketStatus.OPEN, response.status)
        assertEquals("Cannot access invoice", response.subject)
        assertEquals(1, response.messages.size)
        assertEquals("Error when viewing PDF", response.messages[0].message)
    }

    @Test
    fun `addMessage updates ticket status to IN_PROGRESS when agent responds`() {
        val ticket = createTicket(TicketStatus.OPEN)
        `when`(supportTicketRepository.findByIdAndOrganizationId(ticketId, orgId)).thenReturn(Optional.of(ticket))
        `when`(supportTicketRepository.save(any<SupportTicket>())).thenAnswer { it.arguments[0] }
        `when`(customerRepository.findById(customerId)).thenReturn(Optional.of(createCustomer()))

        val request =
            AddTicketMessageRequest(
                message = "We are looking into this.",
                isInternalNote = false,
            )

        val response = service.addMessage(ticketId, orgId, userId, TicketSenderType.AGENT, request)

        assertEquals(TicketStatus.IN_PROGRESS, response.status)
        assertEquals(2, response.messages.size)
        verify(supportTicketMessageRepository).save(any())
    }

    @Test
    fun `resolveTicket sets status to RESOLVED and populates resolvedAt`() {
        val ticket = createTicket(TicketStatus.IN_PROGRESS)
        `when`(supportTicketRepository.findByIdAndOrganizationId(ticketId, orgId)).thenReturn(Optional.of(ticket))
        `when`(supportTicketRepository.save(any<SupportTicket>())).thenAnswer { it.arguments[0] }
        `when`(customerRepository.findById(customerId)).thenReturn(Optional.of(createCustomer()))

        val response = service.resolveTicket(ticketId, orgId)

        assertEquals(TicketStatus.RESOLVED, response.status)
        assertNotNull(response.resolvedAt)
    }

    @Test
    fun `closeTicket sets status to CLOSED and populates closedAt`() {
        val ticket = createTicket(TicketStatus.RESOLVED)
        `when`(supportTicketRepository.findByIdAndOrganizationId(ticketId, orgId)).thenReturn(Optional.of(ticket))
        `when`(supportTicketRepository.save(any<SupportTicket>())).thenAnswer { it.arguments[0] }
        `when`(customerRepository.findById(customerId)).thenReturn(Optional.of(createCustomer()))

        val response = service.closeTicket(ticketId, orgId)

        assertEquals(TicketStatus.CLOSED, response.status)
        assertNotNull(response.closedAt)
    }

    @Test
    fun `addMessage throws exception when ticket is closed`() {
        val ticket = createTicket(TicketStatus.CLOSED)
        `when`(supportTicketRepository.findByIdAndOrganizationId(ticketId, orgId)).thenReturn(Optional.of(ticket))

        val request = AddTicketMessageRequest(message = "Trying to reply")

        assertThrows<BusinessRuleException> {
            service.addMessage(ticketId, orgId, userId, TicketSenderType.CUSTOMER, request)
        }
    }
}
