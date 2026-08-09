package com.aquinofroilan.tessera.service.notification

import com.aquinofroilan.tessera.dto.CreateNotificationRequest
import com.aquinofroilan.tessera.event.LeaveRequestApprovedEvent
import com.aquinofroilan.tessera.event.LeaveRequestRejectedEvent
import com.aquinofroilan.tessera.event.PurchaseRequestApprovedEvent
import com.aquinofroilan.tessera.event.PurchaseRequestRejectedEvent
import com.aquinofroilan.tessera.model.NotificationCategory
import com.aquinofroilan.tessera.service.NotificationService
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * Translates domain events into notification rows. Runs on AFTER_COMMIT
 * so a rolled-back source transaction never produces a notification (or
 * the downstream email side-effect).
 *
 * Each handler is intentionally a tiny mapper: source-domain payload →
 * \`CreateNotificationRequest\`. Per-user opt-out is handled inside
 * [NotificationService.publish] via the email enqueuer + preferences
 * service — listeners don't gate.
 */
@Component
class NotificationEventListener(
    private val notificationService: NotificationService,
) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: LeaveRequestApprovedEvent) {
        notificationService.publish(
            CreateNotificationRequest(
                recipientUserId = event.requesterUserId,
                category = NotificationCategory.APPROVAL,
                kind = NotificationKinds.LEAVE_REQUEST_APPROVED,
                title = "Your leave request was approved",
                body =
                    "Your leave from ${event.startDate} to ${event.endDate} " +
                        "(${event.days} day${if (event.days == 1) "" else "s"}) was approved.",
                link = "/hr/leave-requests/${event.leaveRequestId}",
            ),
            event.organizationId,
        )
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: LeaveRequestRejectedEvent) {
        notificationService.publish(
            CreateNotificationRequest(
                recipientUserId = event.requesterUserId,
                category = NotificationCategory.APPROVAL,
                kind = NotificationKinds.LEAVE_REQUEST_REJECTED,
                title = "Your leave request was rejected",
                body = event.reason?.takeIf { it.isNotBlank() }?.let { "Reason: $it" },
                link = "/hr/leave-requests/${event.leaveRequestId}",
            ),
            event.organizationId,
        )
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: PurchaseRequestApprovedEvent) {
        notificationService.publish(
            CreateNotificationRequest(
                recipientUserId = event.requesterUserId,
                category = NotificationCategory.APPROVAL,
                kind = NotificationKinds.PURCHASE_REQUEST_APPROVED,
                title = "Purchase request ${event.prNumber} approved",
                body = "Your purchase request was approved and is ready to be converted into a PO.",
                link = "/procurement/purchase-requests/${event.purchaseRequestId}",
            ),
            event.organizationId,
        )
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: PurchaseRequestRejectedEvent) {
        notificationService.publish(
            CreateNotificationRequest(
                recipientUserId = event.requesterUserId,
                category = NotificationCategory.APPROVAL,
                kind = NotificationKinds.PURCHASE_REQUEST_REJECTED,
                title = "Purchase request ${event.prNumber} rejected",
                body = event.reason?.takeIf { it.isNotBlank() }?.let { "Reason: $it" },
                link = "/procurement/purchase-requests/${event.purchaseRequestId}",
            ),
            event.organizationId,
        )
    }
}
