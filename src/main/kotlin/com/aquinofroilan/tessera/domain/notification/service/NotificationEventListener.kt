package com.aquinofroilan.tessera.domain.notification.service

import com.aquinofroilan.tessera.domain.notification.dto.CreateNotificationRequest
import com.aquinofroilan.tessera.event.DomainEvent
import com.aquinofroilan.tessera.event.LeaveRequestApprovedEvent
import com.aquinofroilan.tessera.event.LeaveRequestRejectedEvent
import com.aquinofroilan.tessera.event.PurchaseRequestApprovedEvent
import com.aquinofroilan.tessera.event.PurchaseRequestRejectedEvent
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * Notifies the **requester** about the lifecycle decision on their own
 * leave / purchase request. Runs on AFTER_COMMIT so a rolled-back source
 * transaction never produces a notification or the downstream email.
 *
 * Copy and link come from [NotificationTemplate.describe] so the
 * requester-side message and the workflow-rule fan-out can't drift.
 */
@Component
class NotificationEventListener(
    private val notificationService: NotificationService,
) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: LeaveRequestApprovedEvent) {
        publishToRequester(event, event.requesterUserId)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: LeaveRequestRejectedEvent) {
        publishToRequester(event, event.requesterUserId)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: PurchaseRequestApprovedEvent) {
        publishToRequester(event, event.requesterUserId)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: PurchaseRequestRejectedEvent) {
        publishToRequester(event, event.requesterUserId)
    }

    private fun publishToRequester(
        event: DomainEvent,
        requesterUserId: java.util.UUID,
    ) {
        val content = NotificationTemplate.describe(event)
        notificationService.publish(
            CreateNotificationRequest(
                recipientUserId = requesterUserId,
                category = content.category,
                kind = NotificationKinds.of(event),
                title = personaliseForRequester(content.title),
                body = content.body,
                link = content.link,
            ),
            event.organizationId,
        )
    }

    /**
     * The requester sees "Your leave request was approved", while the
     * workflow fan-out (manager / finance / etc.) sees "Leave request
     * approved". Same template, different lede.
     */
    private fun personaliseForRequester(generic: String): String =
        when {
            generic == "Leave request approved" -> "Your leave request was approved"
            generic == "Leave request rejected" -> "Your leave request was rejected"
            else -> generic
        }
}
