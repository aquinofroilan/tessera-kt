package com.aquinofroilan.tessera.domain.notification.service

import com.aquinofroilan.tessera.domain.notification.model.NotificationCategory
import com.aquinofroilan.tessera.event.DomainEvent
import com.aquinofroilan.tessera.event.LeaveRequestApprovedEvent
import com.aquinofroilan.tessera.event.LeaveRequestRejectedEvent
import com.aquinofroilan.tessera.event.PurchaseRequestApprovedEvent
import com.aquinofroilan.tessera.event.PurchaseRequestRejectedEvent

/**
 * Maps a [DomainEvent] to the display content carried by a notification —
 * exhaustive over the sealed hierarchy, so a new event subclass surfaces
 * as a compile error here until someone gives it copy.
 *
 * The two consumers ([NotificationEventListener] for the requester-side
 * notification, [com.aquinofroilan.tessera.service.WorkflowRuleEvaluator]
 * for the workflow fan-out) call this so they can never drift on copy.
 */
data class NotificationContent(
    val category: NotificationCategory,
    val title: String,
    val body: String?,
    val link: String?,
)

object NotificationTemplate {
    fun describe(event: DomainEvent): NotificationContent =
        when (event) {
            is LeaveRequestApprovedEvent ->
                NotificationContent(
                    category = NotificationCategory.APPROVAL,
                    title = "Leave request approved",
                    body =
                        "Leave from ${event.startDate} to ${event.endDate} " +
                            "(${event.days} day${if (event.days == 1) "" else "s"}) was approved.",
                    link = "/hr/leave-requests/${event.leaveRequestId}",
                )
            is LeaveRequestRejectedEvent ->
                NotificationContent(
                    category = NotificationCategory.APPROVAL,
                    title = "Leave request rejected",
                    body = event.reason?.takeIf { it.isNotBlank() }?.let { "Reason: $it" },
                    link = "/hr/leave-requests/${event.leaveRequestId}",
                )
            is PurchaseRequestApprovedEvent ->
                NotificationContent(
                    category = NotificationCategory.APPROVAL,
                    title = "Purchase request ${event.prNumber} approved",
                    body = "Approved and ready to be converted into a PO.",
                    link = "/procurement/purchase-requests/${event.purchaseRequestId}",
                )
            is PurchaseRequestRejectedEvent ->
                NotificationContent(
                    category = NotificationCategory.APPROVAL,
                    title = "Purchase request ${event.prNumber} rejected",
                    body = event.reason?.takeIf { it.isNotBlank() }?.let { "Reason: $it" },
                    link = "/procurement/purchase-requests/${event.purchaseRequestId}",
                )
        }
}
