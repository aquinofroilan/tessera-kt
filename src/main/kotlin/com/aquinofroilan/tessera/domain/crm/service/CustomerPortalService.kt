package com.aquinofroilan.tessera.domain.crm.service

import com.aquinofroilan.tessera.domain.auth.repository.UserRepository
import com.aquinofroilan.tessera.domain.crm.dto.AddTicketMessageRequest
import com.aquinofroilan.tessera.domain.crm.dto.CreatePortalTicketRequest
import com.aquinofroilan.tessera.domain.crm.dto.CreateSupportTicketRequest
import com.aquinofroilan.tessera.domain.crm.dto.CustomerPortalSummaryResponse
import com.aquinofroilan.tessera.domain.crm.dto.CustomerPortalUserDto
import com.aquinofroilan.tessera.domain.crm.dto.LinkPortalUserRequest
import com.aquinofroilan.tessera.domain.crm.dto.PortalInvoiceResponse
import com.aquinofroilan.tessera.domain.crm.dto.PortalOrderResponse
import com.aquinofroilan.tessera.domain.crm.dto.SupportTicketResponse
import com.aquinofroilan.tessera.domain.crm.model.CustomerPortalUser
import com.aquinofroilan.tessera.domain.crm.model.TicketSenderType
import com.aquinofroilan.tessera.domain.crm.model.TicketStatus
import com.aquinofroilan.tessera.domain.crm.repository.CustomerPortalUserRepository
import com.aquinofroilan.tessera.domain.crm.repository.SupportTicketRepository
import com.aquinofroilan.tessera.domain.finance.model.InvoiceStatus
import com.aquinofroilan.tessera.domain.finance.repository.InvoiceRepository
import com.aquinofroilan.tessera.domain.sales.model.Customer
import com.aquinofroilan.tessera.domain.sales.model.SalesOrderStatus
import com.aquinofroilan.tessera.domain.sales.repository.CustomerRepository
import com.aquinofroilan.tessera.domain.sales.repository.SalesOrderRepository
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.UUID

@Service
class CustomerPortalService(
    private val customerPortalUserRepository: CustomerPortalUserRepository,
    private val customerRepository: CustomerRepository,
    private val invoiceRepository: InvoiceRepository,
    private val salesOrderRepository: SalesOrderRepository,
    private val supportTicketRepository: SupportTicketRepository,
    private val supportTicketService: SupportTicketService,
    private val userRepository: UserRepository,
) {
    // -------------------------------------------------------------------------
    // Portal User Mapping (Staff / Admin operations)
    // -------------------------------------------------------------------------
    @Transactional(readOnly = true)
    fun listPortalUsers(
        organizationId: UUID,
        customerId: UUID,
    ): List<CustomerPortalUserDto> {
        val mappings = customerPortalUserRepository.findByOrganizationIdAndCustomerId(organizationId, customerId)
        return mappings.map { CustomerPortalUserDto.from(it) }
    }

    @Transactional
    fun linkPortalUser(
        organizationId: UUID,
        customerId: UUID,
        request: LinkPortalUserRequest,
    ): CustomerPortalUserDto {
        val customer =
            customerRepository.findByIdAndOrganizationId(customerId, organizationId).orElseThrow {
                ResourceNotFoundException("Customer $customerId not found")
            }

        val user =
            userRepository.findById(request.userId).orElseThrow {
                ResourceNotFoundException("User ${request.userId} not found")
            }
        if (user.organizationId != organizationId) {
            throw ResourceNotFoundException("User ${request.userId} not found")
        }

        val existingOpt =
            customerPortalUserRepository.findByOrganizationIdAndUserId(organizationId, request.userId)
        if (existingOpt.isPresent) {
            val existing = existingOpt.get()
            if (existing.customerId != customer.id) {
                throw BusinessRuleException("User is already linked to another customer portal in this organization")
            }
            existing.isActive = true
            request.isPrimary?.let { existing.isPrimary = it }
            val saved = customerPortalUserRepository.save(existing)
            return CustomerPortalUserDto.from(saved)
        }

        val mapping =
            CustomerPortalUser(
                organizationId = organizationId,
                customerId = customer.id,
                userId = user.uuid,
                isPrimary = request.isPrimary ?: false,
                isActive = true,
            )
        val saved = customerPortalUserRepository.save(mapping)
        return CustomerPortalUserDto.from(saved)
    }

    @Transactional
    fun unlinkPortalUser(
        organizationId: UUID,
        customerId: UUID,
        userId: UUID,
    ) {
        customerPortalUserRepository.deleteByOrganizationIdAndCustomerIdAndUserId(organizationId, customerId, userId)
    }

    // -------------------------------------------------------------------------
    // Customer Self-Service Portal Operations
    // -------------------------------------------------------------------------
    @Transactional(readOnly = true)
    fun resolveCustomerForUser(
        organizationId: UUID,
        userId: UUID,
    ): Customer {
        val mapping =
            customerPortalUserRepository.findByOrganizationIdAndUserId(organizationId, userId).orElseThrow {
                ResourceNotFoundException("No customer portal profile found for user $userId")
            }

        if (!mapping.isActive) {
            throw BusinessRuleException("Customer portal access for user $userId is inactive")
        }

        return customerRepository.findByIdAndOrganizationId(mapping.customerId, organizationId).orElseThrow {
            ResourceNotFoundException("Customer ${mapping.customerId} not found")
        }
    }

    @Transactional(readOnly = true)
    fun getMyPortalSummary(
        organizationId: UUID,
        userId: UUID,
    ): CustomerPortalSummaryResponse {
        val customer = resolveCustomerForUser(organizationId, userId)

        val openInvoices =
            invoiceRepository
                .findByOrganizationIdAndCustomerId(organizationId, customer.id)
                .filter { it.status == InvoiceStatus.APPROVED || it.status == InvoiceStatus.PARTIALLY_PAID }

        val outstandingBalance =
            openInvoices
                .map { (it.totalAmount.subtract(it.amountReceived)).max(BigDecimal.ZERO) }
                .fold(BigDecimal.ZERO) { acc, amt -> acc.add(amt) }

        val activeOrders =
            salesOrderRepository
                .findByOrganizationIdAndCustomerId(organizationId, customer.id)
                .filter {
                    it.status == SalesOrderStatus.APPROVED ||
                        it.status == SalesOrderStatus.PARTIALLY_FULFILLED ||
                        it.status == SalesOrderStatus.FULFILLED
                }

        val openTicketsCount =
            supportTicketRepository.countByOrganizationIdAndCustomerIdAndStatusIn(
                organizationId,
                customer.id,
                listOf(TicketStatus.OPEN, TicketStatus.IN_PROGRESS, TicketStatus.WAITING_FOR_CUSTOMER),
            )

        return CustomerPortalSummaryResponse(
            customerId = customer.id,
            customerName = customer.name,
            contactName = customer.contactName,
            email = customer.contactEmail,
            phone = customer.contactPhone,
            customerSegment = customer.customerSegment,
            openInvoicesCount = openInvoices.size.toLong(),
            totalOutstandingBalance = outstandingBalance,
            activeOrdersCount = activeOrders.size.toLong(),
            openTicketsCount = openTicketsCount,
        )
    }

    @Transactional(readOnly = true)
    fun getMyInvoices(
        organizationId: UUID,
        userId: UUID,
        status: InvoiceStatus? = null,
    ): List<PortalInvoiceResponse> {
        val customer = resolveCustomerForUser(organizationId, userId)
        val invoices =
            if (status != null) {
                invoiceRepository.findByOrganizationIdAndStatusAndCustomerId(organizationId, status, customer.id)
            } else {
                invoiceRepository.findByOrganizationIdAndCustomerId(organizationId, customer.id)
            }
        return invoices.map { PortalInvoiceResponse.from(it) }
    }

    @Transactional(readOnly = true)
    fun getMyInvoice(
        organizationId: UUID,
        userId: UUID,
        invoiceId: UUID,
    ): PortalInvoiceResponse {
        val customer = resolveCustomerForUser(organizationId, userId)
        val invoice =
            invoiceRepository.findById(invoiceId).orElseThrow {
                ResourceNotFoundException("Invoice $invoiceId not found")
            }
        if (invoice.organizationId != organizationId || invoice.customerId != customer.id) {
            throw ResourceNotFoundException("Invoice $invoiceId not found")
        }
        return PortalInvoiceResponse.from(invoice)
    }

    @Transactional(readOnly = true)
    fun getMyOrders(
        organizationId: UUID,
        userId: UUID,
        status: SalesOrderStatus? = null,
    ): List<PortalOrderResponse> {
        val customer = resolveCustomerForUser(organizationId, userId)
        val orders =
            if (status != null) {
                salesOrderRepository.findByOrganizationIdAndStatusAndCustomerId(organizationId, status, customer.id)
            } else {
                salesOrderRepository.findByOrganizationIdAndCustomerId(organizationId, customer.id)
            }
        return orders.map { PortalOrderResponse.from(it) }
    }

    @Transactional(readOnly = true)
    fun getMyOrder(
        organizationId: UUID,
        userId: UUID,
        orderId: UUID,
    ): PortalOrderResponse {
        val customer = resolveCustomerForUser(organizationId, userId)
        val order =
            salesOrderRepository.findById(orderId).orElseThrow {
                ResourceNotFoundException("Sales order $orderId not found")
            }
        if (order.organizationId != organizationId || order.customerId != customer.id) {
            throw ResourceNotFoundException("Sales order $orderId not found")
        }
        return PortalOrderResponse.from(order)
    }

    @Transactional(readOnly = true)
    fun getMyTickets(
        organizationId: UUID,
        userId: UUID,
        status: TicketStatus? = null,
    ): List<SupportTicketResponse> {
        val customer = resolveCustomerForUser(organizationId, userId)
        val tickets =
            if (status != null) {
                supportTicketRepository.findByOrganizationIdAndCustomerIdAndStatus(organizationId, customer.id, status)
            } else {
                supportTicketRepository.findByOrganizationIdAndCustomerId(organizationId, customer.id)
            }
        return tickets.map {
            SupportTicketResponse.from(it, customerName = customer.name, includeInternalNotes = false)
        }
    }

    @Transactional(readOnly = true)
    fun getMyTicket(
        organizationId: UUID,
        userId: UUID,
        ticketId: UUID,
    ): SupportTicketResponse {
        val customer = resolveCustomerForUser(organizationId, userId)
        val ticket =
            supportTicketRepository.findByIdAndOrganizationId(ticketId, organizationId).orElseThrow {
                ResourceNotFoundException("Support ticket $ticketId not found")
            }
        if (ticket.customerId != customer.id) {
            throw ResourceNotFoundException("Support ticket $ticketId not found")
        }
        return SupportTicketResponse.from(ticket, customerName = customer.name, includeInternalNotes = false)
    }

    @Transactional
    fun createMyTicket(
        organizationId: UUID,
        userId: UUID,
        request: CreatePortalTicketRequest,
    ): SupportTicketResponse {
        val customer = resolveCustomerForUser(organizationId, userId)
        val ticketReq =
            CreateSupportTicketRequest(
                customerId = customer.id,
                subject = request.subject,
                description = request.description,
                category = request.category,
                priority = request.priority,
            )
        val created = supportTicketService.createTicket(organizationId, userId, ticketReq, senderType = TicketSenderType.CUSTOMER)
        return created.copy(messages = created.messages.filter { !it.isInternalNote })
    }

    @Transactional
    fun addMyTicketMessage(
        organizationId: UUID,
        userId: UUID,
        ticketId: UUID,
        message: String,
    ): SupportTicketResponse {
        val customer = resolveCustomerForUser(organizationId, userId)
        val ticket =
            supportTicketRepository.findByIdAndOrganizationId(ticketId, organizationId).orElseThrow {
                ResourceNotFoundException("Support ticket $ticketId not found")
            }
        if (ticket.customerId != customer.id) {
            throw ResourceNotFoundException("Support ticket $ticketId not found")
        }

        val updated =
            supportTicketService.addMessage(
                id = ticketId,
                organizationId = organizationId,
                senderId = userId,
                senderType = TicketSenderType.CUSTOMER,
                request = AddTicketMessageRequest(message = message, isInternalNote = false),
            )
        return updated.copy(messages = updated.messages.filter { !it.isInternalNote })
    }

    @Transactional
    fun closeMyTicket(
        organizationId: UUID,
        userId: UUID,
        ticketId: UUID,
    ): SupportTicketResponse {
        val customer = resolveCustomerForUser(organizationId, userId)
        val ticket =
            supportTicketRepository.findByIdAndOrganizationId(ticketId, organizationId).orElseThrow {
                ResourceNotFoundException("Support ticket $ticketId not found")
            }
        if (ticket.customerId != customer.id) {
            throw ResourceNotFoundException("Support ticket $ticketId not found")
        }

        val updated = supportTicketService.closeTicket(ticketId, organizationId)
        return updated.copy(messages = updated.messages.filter { !it.isInternalNote })
    }
}
