package com.aquinofroilan.tessera.domain.crm.model

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

enum class TicketStatus {
    OPEN,
    IN_PROGRESS,
    WAITING_FOR_CUSTOMER,
    RESOLVED,
    CLOSED,
}

enum class TicketPriority {
    LOW,
    MEDIUM,
    HIGH,
    URGENT,
}

enum class TicketCategory {
    BILLING,
    ORDER_INQUIRY,
    TECHNICAL_SUPPORT,
    PRODUCT_DEFECT,
    GENERAL_INQUIRY,
    FEATURE_REQUEST,
    OTHER,
}

enum class TicketSenderType {
    CUSTOMER,
    AGENT,
    SYSTEM,
}

@Entity
@Table(name = "support_ticket_messages")
class SupportTicketMessage(
    @Id
    @Column(columnDefinition = "uuid")
    var id: UUID = UUID.ofEpochMillis(System.currentTimeMillis()),
    @Column(name = "organization_id", nullable = false, columnDefinition = "uuid")
    var organizationId: UUID,
    @Column(name = "ticket_id", nullable = false)
    var ticketId: Long = 0L,
    @Column(name = "sender_id", nullable = false, columnDefinition = "uuid")
    var senderId: UUID,
    @Enumerated(EnumType.STRING)
    @Column(name = "sender_type", nullable = false)
    var senderType: TicketSenderType,
    @Column(nullable = false)
    var message: String,
    @Column(name = "is_internal_note", nullable = false)
    var isInternalNote: Boolean = false,
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC),
)

@Entity
@Table(name = "support_tickets")
@EntityListeners(AuditingEntityListener::class)
class SupportTicket(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L,
    @Column(name = "organization_id", nullable = false, columnDefinition = "uuid")
    var organizationId: UUID,
    @Column(name = "customer_id", nullable = false, columnDefinition = "uuid")
    var customerId: UUID,
    @Column(name = "contact_id", columnDefinition = "uuid")
    var contactId: UUID? = null,
    @Column(nullable = false)
    var subject: String,
    @Column(nullable = false)
    var description: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: TicketStatus = TicketStatus.OPEN,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var priority: TicketPriority = TicketPriority.MEDIUM,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var category: TicketCategory = TicketCategory.GENERAL_INQUIRY,
    @Column(name = "assigned_to_user_id", columnDefinition = "uuid")
    var assignedToUserId: UUID? = null,
    @Column(name = "created_by_user_id", nullable = false, columnDefinition = "uuid")
    var createdByUserId: UUID,
    @Column(name = "resolved_at")
    var resolvedAt: LocalDateTime? = null,
    @Column(name = "closed_at")
    var closedAt: LocalDateTime? = null,
    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "ticket_id")
    @OrderBy("createdAt ASC")
    var messages: MutableList<SupportTicketMessage> = mutableListOf(),
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC),
    @LastModifiedDate
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC),
)
