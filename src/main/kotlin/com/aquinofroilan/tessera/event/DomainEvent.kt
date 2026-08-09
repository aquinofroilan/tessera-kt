package com.aquinofroilan.tessera.event

import java.time.LocalDate

/**
 * Marker for domain events the rest of the platform can subscribe to.
 *
 * Events are emitted from service methods via [DomainEventPublisher] and
 * delivered to listeners on the AFTER_COMMIT phase of the source
 * transaction (see [NotificationEventListener]). A rolled-back service
 * call therefore produces no notification / no email / no workflow rule
 * fan-out — the source-of-truth row never landed.
 */
sealed interface DomainEvent {
    val organizationId: java.util.UUID
}

data class LeaveRequestApprovedEvent(
    override val organizationId: java.util.UUID,
    val leaveRequestId: java.util.UUID,
    val requesterUserId: java.util.UUID,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val days: Int,
) : DomainEvent

data class LeaveRequestRejectedEvent(
    override val organizationId: java.util.UUID,
    val leaveRequestId: java.util.UUID,
    val requesterUserId: java.util.UUID,
    val reason: String?,
) : DomainEvent

data class PurchaseRequestApprovedEvent(
    override val organizationId: java.util.UUID,
    val purchaseRequestId: java.util.UUID,
    val prNumber: String,
    val requesterUserId: java.util.UUID,
) : DomainEvent

data class PurchaseRequestRejectedEvent(
    override val organizationId: java.util.UUID,
    val purchaseRequestId: java.util.UUID,
    val prNumber: String,
    val requesterUserId: java.util.UUID,
    val reason: String?,
) : DomainEvent
