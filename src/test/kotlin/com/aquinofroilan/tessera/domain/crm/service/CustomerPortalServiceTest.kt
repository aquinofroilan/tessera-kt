package com.aquinofroilan.tessera.domain.crm.service

import com.aquinofroilan.tessera.domain.auth.model.User
import com.aquinofroilan.tessera.domain.auth.repository.UserRepository
import com.aquinofroilan.tessera.domain.crm.dto.CreatePortalTicketRequest
import com.aquinofroilan.tessera.domain.crm.dto.LinkPortalUserRequest
import com.aquinofroilan.tessera.domain.crm.dto.SupportTicketResponse
import com.aquinofroilan.tessera.domain.crm.model.CustomerPortalUser
import com.aquinofroilan.tessera.domain.crm.model.TicketCategory
import com.aquinofroilan.tessera.domain.crm.model.TicketPriority
import com.aquinofroilan.tessera.domain.crm.model.TicketStatus
import com.aquinofroilan.tessera.domain.crm.repository.CustomerPortalUserRepository
import com.aquinofroilan.tessera.domain.crm.repository.SupportTicketRepository
import com.aquinofroilan.tessera.domain.finance.model.Invoice
import com.aquinofroilan.tessera.domain.finance.model.InvoiceStatus
import com.aquinofroilan.tessera.domain.finance.repository.InvoiceRepository
import com.aquinofroilan.tessera.domain.sales.model.Customer
import com.aquinofroilan.tessera.domain.sales.model.CustomerSegment
import com.aquinofroilan.tessera.domain.sales.model.SalesOrder
import com.aquinofroilan.tessera.domain.sales.model.SalesOrderStatus
import com.aquinofroilan.tessera.domain.sales.repository.CustomerRepository
import com.aquinofroilan.tessera.domain.sales.repository.SalesOrderRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Optional
import java.util.UUID

class CustomerPortalServiceTest {
    private lateinit var customerPortalUserRepository: CustomerPortalUserRepository
    private lateinit var customerRepository: CustomerRepository
    private lateinit var invoiceRepository: InvoiceRepository
    private lateinit var salesOrderRepository: SalesOrderRepository
    private lateinit var supportTicketRepository: SupportTicketRepository
    private lateinit var supportTicketService: SupportTicketService
    private lateinit var userRepository: UserRepository
    private lateinit var service: CustomerPortalService

    private val orgId = UUID.fromString("4abe9f6d-6df3-6e5c-953e-3695db9a5216")
    private val userId = UUID.fromString("bc17c97c-3d89-7d43-b7e0-7ca0266eafa8")
    private val customerId = UUID.fromString("11111111-2222-3333-4444-555555555555")

    @BeforeEach
    fun setUp() {
        customerPortalUserRepository = mock(CustomerPortalUserRepository::class.java)
        customerRepository = mock(CustomerRepository::class.java)
        invoiceRepository = mock(InvoiceRepository::class.java)
        salesOrderRepository = mock(SalesOrderRepository::class.java)
        supportTicketRepository = mock(SupportTicketRepository::class.java)
        supportTicketService = mock(SupportTicketService::class.java)
        userRepository = mock(UserRepository::class.java)
        service =
            CustomerPortalService(
                customerPortalUserRepository,
                customerRepository,
                invoiceRepository,
                salesOrderRepository,
                supportTicketRepository,
                supportTicketService,
                userRepository,
            )
    }

    private fun createCustomer() =
        Customer(
            id = customerId,
            name = "Acme Corp",
            contactName = "Acme Contact",
            contactEmail = "contact@acme.com",
            contactPhone = "123-456",
            customerSegment = CustomerSegment.WHOLESALE,
            organizationId = orgId,
        )

    private fun createPortalMapping() =
        CustomerPortalUser(
            id = UUID.randomUUID(),
            organizationId = orgId,
            customerId = customerId,
            userId = userId,
            isPrimary = true,
            isActive = true,
        )

    private fun createUser() =
        User(
            uuid = userId,
            username = "acmecust",
            email = "contact@acme.com",
            firstName = "Acme",
            lastName = "User",
            passwordHash = "hash",
            organizationId = orgId,
        )

    @Test
    fun `linkPortalUser links user to customer`() {
        val request = LinkPortalUserRequest(userId = userId, isPrimary = true)
        `when`(customerRepository.findByIdAndOrganizationId(customerId, orgId)).thenReturn(Optional.of(createCustomer()))
        `when`(userRepository.findById(userId)).thenReturn(Optional.of(createUser()))
        `when`(customerPortalUserRepository.findByOrganizationIdAndUserId(orgId, userId)).thenReturn(Optional.empty())
        `when`(customerPortalUserRepository.save(any<CustomerPortalUser>())).thenAnswer { it.arguments[0] }

        val response = service.linkPortalUser(orgId, customerId, request)

        assertEquals(customerId, response.customerId)
        assertEquals(userId, response.userId)
        assertTrue(response.isPrimary)
    }

    @Test
    fun `getMyPortalSummary aggregates invoices orders and tickets`() {
        `when`(customerPortalUserRepository.findByOrganizationIdAndUserId(orgId, userId)).thenReturn(Optional.of(createPortalMapping()))
        `when`(customerRepository.findByIdAndOrganizationId(customerId, orgId)).thenReturn(Optional.of(createCustomer()))

        val invoice =
            Invoice(
                id = UUID.randomUUID(),
                invoiceNumber = "INV-001",
                customerId = customerId,
                customerName = "Acme Corp",
                date = LocalDate.now(),
                dueDate = LocalDate.now().plusDays(30),
                organizationId = orgId,
                status = InvoiceStatus.APPROVED,
                lines = emptyList(),
                totalAmount = BigDecimal("500.00"),
                amountReceived = BigDecimal("100.00"),
                createdBy = userId,
            )
        `when`(invoiceRepository.findByOrganizationIdAndCustomerId(orgId, customerId)).thenReturn(listOf(invoice))

        val order =
            SalesOrder(
                id = UUID.randomUUID(),
                soNumber = "SO-001",
                customerId = customerId,
                customerName = "Acme Corp",
                warehouseId = UUID.randomUUID(),
                orderDate = LocalDate.now(),
                organizationId = orgId,
                status = SalesOrderStatus.APPROVED,
                lines = emptyList(),
                totalAmount = BigDecimal("1200.00"),
                createdBy = userId,
            )
        `when`(salesOrderRepository.findByOrganizationIdAndCustomerId(orgId, customerId)).thenReturn(listOf(order))
        `when`(supportTicketRepository.countByOrganizationIdAndCustomerIdAndStatusIn(eq(orgId), eq(customerId), any())).thenReturn(2L)

        val summary = service.getMyPortalSummary(orgId, userId)

        assertEquals(customerId, summary.customerId)
        assertEquals("Acme Corp", summary.customerName)
        assertEquals(1, summary.openInvoicesCount)
        assertEquals(0, summary.totalOutstandingBalance.compareTo(BigDecimal("400.00")))
        assertEquals(1, summary.activeOrdersCount)
        assertEquals(2, summary.openTicketsCount)
    }

    @Test
    fun `createMyTicket creates ticket and strips internal notes`() {
        `when`(customerPortalUserRepository.findByOrganizationIdAndUserId(orgId, userId)).thenReturn(Optional.of(createPortalMapping()))
        `when`(customerRepository.findByIdAndOrganizationId(customerId, orgId)).thenReturn(Optional.of(createCustomer()))

        val mockTicketResponse =
            SupportTicketResponse(
                id = UUID.randomUUID(),
                organizationId = orgId,
                ticketNumber = 1,
                customerId = customerId,
                customerName = "Acme Corp",
                contactId = null,
                subject = "Order question",
                description = "When will SO-001 ship?",
                status = TicketStatus.OPEN,
                priority = TicketPriority.MEDIUM,
                category = TicketCategory.ORDER_INQUIRY,
                assignedToUserId = null,
                createdByUserId = userId,
                resolvedAt = null,
                closedAt = null,
                messages = emptyList(),
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
            )
        `when`(supportTicketService.createTicket(eq(orgId), eq(userId), any(), any())).thenReturn(mockTicketResponse)

        val request =
            CreatePortalTicketRequest(
                subject = "Order question",
                description = "When will SO-001 ship?",
                category = TicketCategory.ORDER_INQUIRY,
            )

        val response = service.createMyTicket(orgId, userId, request)

        assertEquals(1, response.ticketNumber)
        assertEquals("Order question", response.subject)
    }
}
