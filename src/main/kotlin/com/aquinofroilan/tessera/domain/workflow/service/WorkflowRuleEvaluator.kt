package com.aquinofroilan.tessera.domain.workflow.service

import com.aquinofroilan.tessera.domain.auth.model.User
import com.aquinofroilan.tessera.domain.auth.repository.UserRepository
import com.aquinofroilan.tessera.domain.notification.dto.CreateNotificationRequest
import com.aquinofroilan.tessera.domain.notification.service.NotificationKinds
import com.aquinofroilan.tessera.domain.notification.service.NotificationService
import com.aquinofroilan.tessera.domain.notification.service.NotificationTemplate
import com.aquinofroilan.tessera.domain.workflow.model.WorkflowRule
import com.aquinofroilan.tessera.domain.workflow.model.WorkflowRuleActionType
import com.aquinofroilan.tessera.event.DomainEvent
import com.aquinofroilan.tessera.event.LeaveRequestApprovedEvent
import com.aquinofroilan.tessera.event.LeaveRequestRejectedEvent
import com.aquinofroilan.tessera.event.PurchaseRequestApprovedEvent
import com.aquinofroilan.tessera.event.PurchaseRequestRejectedEvent
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * Workflow-rules side of the notification pipeline. Subscribes to the same
 * [DomainEvent] stream as [com.aquinofroilan.tessera.service.notification.NotificationEventListener],
 * but instead of notifying the requester it consults configured rules and
 * fans the event out to additional users (NOTIFY_USER) or every user in the
 * org who carries a named role (NOTIFY_ROLE).
 *
 * The two listeners are intentionally separate so the requester-side
 * notification keeps firing even when no workflow rules exist; conversely
 * rules can exist without depending on the requester-side path.
 */
@Component
class WorkflowRuleEvaluator(
    private val workflowRuleService: WorkflowRuleService,
    private val notificationService: NotificationService,
    private val userRepository: UserRepository,
) {
    private val log = LoggerFactory.getLogger(WorkflowRuleEvaluator::class.java)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: LeaveRequestApprovedEvent) = evaluate(event)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: LeaveRequestRejectedEvent) = evaluate(event)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: PurchaseRequestApprovedEvent) = evaluate(event)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun on(event: PurchaseRequestRejectedEvent) = evaluate(event)

    private fun evaluate(event: DomainEvent) {
        val kind = NotificationKinds.of(event)
        val rules = workflowRuleService.findEnabledFor(event.organizationId, kind)
        if (rules.isEmpty()) return

        val content = NotificationTemplate.describe(event)
        rules.forEach { rule ->
            try {
                resolveRecipients(rule, event.organizationId).forEach { recipientUserId ->
                    notificationService.publish(
                        CreateNotificationRequest(
                            recipientUserId = recipientUserId,
                            category = content.category,
                            kind = kind,
                            title = content.title,
                            body = content.body,
                            link = content.link,
                        ),
                        event.organizationId,
                    )
                }
            } catch (e: Exception) {
                log.warn(
                    "Workflow rule {} ({}={}) failed to evaluate event {}: {}",
                    rule.id,
                    rule.actionType,
                    rule.actionTarget,
                    kind,
                    e.message,
                )
            }
        }
    }

    private fun resolveRecipients(
        rule: WorkflowRule,
        organizationId: java.util.UUID,
    ): List<java.util.UUID> =
        when (rule.actionType) {
            WorkflowRuleActionType.NOTIFY_USER -> listOf(java.util.UUID.fromString(rule.actionTarget))
            WorkflowRuleActionType.NOTIFY_ROLE ->
                userRepository
                    .findAll()
                    .filter(matchesOrgAndRole(organizationId, rule.actionTarget))
                    .map(User::uuid)
        }

    private fun matchesOrgAndRole(
        organizationId: java.util.UUID,
        roleName: String,
    ): (User) -> Boolean =
        { user ->
            user.organizationId == organizationId &&
                user.roleAssignments.any { assignment ->
                    assignment.role == roleName &&
                        (assignment.organizationId == null || assignment.organizationId == organizationId)
                }
        }
}
