package com.aquinofroilan.tessera.domain.crm.dto

import com.aquinofroilan.tessera.domain.crm.model.CustomerPortalUser
import com.aquinofroilan.tessera.domain.crm.model.TicketCategory
import com.aquinofroilan.tessera.domain.crm.model.TicketPriority
import com.aquinofroilan.tessera.domain.finance.model.Invoice
import com.aquinofroilan.tessera.domain.finance.model.InvoiceStatus
import com.aquinofroilan.tessera.domain.sales.model.CustomerSegment
import com.aquinofroilan.tessera.domain.sales.model.SalesOrder
import com.aquinofroilan.tessera.domain.sales.model.SalesOrderStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class CustomerPortalUserDto(
    val id: UUID,
    val organizationId: UUID,
    val customerId: UUID,
    val userId: UUID,
    val isPrimary: Boolean,
    val isActive: Boolean,
    val createdAt: LocalDateTime?,
) {
    companion object {
        fun from(user: CustomerPortalUser): CustomerPortalUserDto =
            CustomerPortalUserDto(
                id = user.id,
                organizationId = user.organizationId,
                customerId = user.customerId,
                userId = user.userId,
                isPrimary = user.isPrimary,
                isActive = user.isActive,
                createdAt = user.createdAt,
            )
    }
}

data class LinkPortalUserRequest(
    @field:NotNull(message = "User ID is required")
    val userId: UUID,
    val isPrimary: Boolean? = null,
)

data class CustomerPortalSummaryResponse(
    val customerId: UUID,
    val customerName: String,
    val contactName: String?,
    val email: String?,
    val phone: String?,
    val customerSegment: CustomerSegment,
    val openInvoicesCount: Long,
    val totalOutstandingBalance: BigDecimal,
    val activeOrdersCount: Long,
    val openTicketsCount: Long,
)

data class PortalInvoiceLineDto(
    val id: UUID,
    val lineNumber: Int,
    val description: String?,
    val amount: BigDecimal,
)

data class PortalInvoiceResponse(
    val id: UUID,
    val invoiceNumber: String,
    val date: LocalDate,
    val dueDate: LocalDate,
    val referenceNumber: String?,
    val status: InvoiceStatus,
    val currencyCode: String,
    val totalAmount: BigDecimal,
    val amountReceived: BigDecimal,
    val balanceDue: BigDecimal,
    val lines: List<PortalInvoiceLineDto>,
    val createdAt: LocalDateTime?,
) {
    companion object {
        fun from(invoice: Invoice): PortalInvoiceResponse {
            val balanceDue = (invoice.totalAmount.subtract(invoice.amountReceived)).max(BigDecimal.ZERO)
            return PortalInvoiceResponse(
                id = invoice.id,
                invoiceNumber = invoice.invoiceNumber,
                date = invoice.date,
                dueDate = invoice.dueDate,
                referenceNumber = invoice.referenceNumber,
                status = invoice.status,
                currencyCode = invoice.currencyCode,
                totalAmount = invoice.totalAmount,
                amountReceived = invoice.amountReceived,
                balanceDue = balanceDue,
                lines =
                    invoice.lines.map {
                        PortalInvoiceLineDto(
                            id = it.id,
                            lineNumber = it.lineNumber,
                            description = it.description ?: it.accountName,
                            amount = it.amount,
                        )
                    },
                createdAt = invoice.createdAt,
            )
        }
    }
}

data class PortalOrderLineDto(
    val id: UUID,
    val lineNumber: Int,
    val productId: UUID,
    val productSku: String,
    val productName: String,
    val quantity: BigDecimal,
    val unitPrice: BigDecimal,
    val lineTotal: BigDecimal,
    val fulfilledQuantity: BigDecimal,
    val description: String?,
)

data class PortalOrderResponse(
    val id: UUID,
    val soNumber: String,
    val orderDate: LocalDate,
    val expectedDate: LocalDate?,
    val status: SalesOrderStatus,
    val totalAmount: BigDecimal,
    val lines: List<PortalOrderLineDto>,
    val createdAt: LocalDateTime?,
) {
    companion object {
        fun from(order: SalesOrder): PortalOrderResponse =
            PortalOrderResponse(
                id = order.id,
                soNumber = order.soNumber,
                orderDate = order.orderDate,
                expectedDate = order.expectedDate,
                status = order.status,
                totalAmount = order.totalAmount,
                lines =
                    order.lines.map {
                        PortalOrderLineDto(
                            id = it.id,
                            lineNumber = it.lineNumber,
                            productId = it.productId,
                            productSku = it.productSku,
                            productName = it.productName,
                            quantity = it.quantity,
                            unitPrice = it.unitPrice,
                            lineTotal = it.lineTotal,
                            fulfilledQuantity = it.fulfilledQuantity,
                            description = it.description,
                        )
                    },
                createdAt = order.createdAt,
            )
    }
}

data class CreatePortalTicketRequest(
    @field:NotBlank(message = "Subject is required")
    val subject: String,
    @field:NotBlank(message = "Description is required")
    val description: String,
    val category: TicketCategory? = null,
    val priority: TicketPriority? = null,
)
