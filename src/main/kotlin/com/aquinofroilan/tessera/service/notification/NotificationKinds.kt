package com.aquinofroilan.tessera.service.notification

import com.aquinofroilan.tessera.event.DomainEvent
import com.aquinofroilan.tessera.event.LeaveRequestApprovedEvent
import com.aquinofroilan.tessera.event.LeaveRequestRejectedEvent
import com.aquinofroilan.tessera.event.PurchaseRequestApprovedEvent
import com.aquinofroilan.tessera.event.PurchaseRequestRejectedEvent

/**
 * Well-known notification kind strings. The DB column is freeform so any
 * service can mint a new value, but keeping the ones we publish ourselves
 * in one place avoids drift between the listener that writes them, the
 * preference UI that lets users opt out, and the workflow-rule evaluator
 * that fans out additional notifications.
 */
object NotificationKinds {
    const val LEAVE_REQUEST_APPROVED = "leave_request.approved"
    const val LEAVE_REQUEST_REJECTED = "leave_request.rejected"
    const val PURCHASE_REQUEST_APPROVED = "purchase_request.approved"
    const val PURCHASE_REQUEST_REJECTED = "purchase_request.rejected"

    /**
     * Canonical kind string for a domain event. Exhaustive over the sealed
     * [DomainEvent] hierarchy — adding a new event subclass produces a
     * compile error here until you give it a kind.
     */
    fun of(event: DomainEvent): String =
        when (event) {
            is LeaveRequestApprovedEvent -> LEAVE_REQUEST_APPROVED
            is LeaveRequestRejectedEvent -> LEAVE_REQUEST_REJECTED
            is PurchaseRequestApprovedEvent -> PURCHASE_REQUEST_APPROVED
            is PurchaseRequestRejectedEvent -> PURCHASE_REQUEST_REJECTED
        }
}
