package com.aquinofroilan.tessera.domain.workflow.service

import com.aquinofroilan.tessera.domain.auth.model.RoleAssignment
import com.aquinofroilan.tessera.domain.auth.model.User
import com.aquinofroilan.tessera.domain.auth.repository.UserRepository
import com.aquinofroilan.tessera.domain.notification.dto.CreateNotificationRequest
import com.aquinofroilan.tessera.domain.notification.model.Notification
import com.aquinofroilan.tessera.domain.notification.model.NotificationCategory
import com.aquinofroilan.tessera.domain.notification.service.NotificationKinds
import com.aquinofroilan.tessera.domain.notification.service.NotificationService
import com.aquinofroilan.tessera.domain.workflow.model.WorkflowRule
import com.aquinofroilan.tessera.domain.workflow.model.WorkflowRuleActionType
import com.aquinofroilan.tessera.event.LeaveRequestApprovedEvent
import com.aquinofroilan.tessera.event.PurchaseRequestApprovedEvent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate

class WorkflowRuleEvaluatorTest {
    private lateinit var workflowRuleService: WorkflowRuleService
    private lateinit var notificationService: NotificationService
    private lateinit var userRepository: UserRepository
    private lateinit var evaluator: WorkflowRuleEvaluator

    private val orgId: java.util.UUID = java.util.UUID.fromString("00000000-0000-0000-0000-000000000100")

    @BeforeEach
    fun setup() {
        workflowRuleService = mock(WorkflowRuleService::class.java)
        notificationService = mock(NotificationService::class.java)
        userRepository = mock(UserRepository::class.java)
        evaluator = WorkflowRuleEvaluator(workflowRuleService, notificationService, userRepository)
    }

    @Test
    fun `no rules means no notification fan-out`() {
        whenever(
            workflowRuleService.findEnabledFor(orgId, NotificationKinds.LEAVE_REQUEST_APPROVED),
        ).thenReturn(emptyList())

        evaluator.on(leaveApproved())

        verify(notificationService, never()).publish(any(), any())
    }

    @Test
    fun `NOTIFY_USER rule produces one notification to the configured user`() {
        whenever(
            workflowRuleService.findEnabledFor(orgId, NotificationKinds.LEAVE_REQUEST_APPROVED),
        ).thenReturn(
            listOf(
                rule(
                    id = java.util.UUID.fromString("00000000-0000-0000-0000-000000000600"),
                    actionType = WorkflowRuleActionType.NOTIFY_USER,
                    actionTarget = "00000000-0000-0000-0000-000000000301",
                ),
            ),
        )

        evaluator.on(leaveApproved())

        val captor = argumentCaptor<CreateNotificationRequest>()
        verify(notificationService).publish(captor.capture(), eq(orgId))
        assertThat(captor.firstValue.recipientUserId).isEqualTo(java.util.UUID.fromString("00000000-0000-0000-0000-000000000301"))
        assertThat(captor.firstValue.kind).isEqualTo(NotificationKinds.LEAVE_REQUEST_APPROVED)
        assertThat(captor.firstValue.category).isEqualTo(NotificationCategory.APPROVAL)
    }

    @Test
    fun `NOTIFY_ROLE rule fans out to every user in org with that role`() {
        whenever(
            workflowRuleService.findEnabledFor(orgId, NotificationKinds.PURCHASE_REQUEST_APPROVED),
        ).thenReturn(
            listOf(
                rule(
                    id = java.util.UUID.fromString("00000000-0000-0000-0000-000000000601"),
                    actionType = WorkflowRuleActionType.NOTIFY_ROLE,
                    actionTarget = "FINANCE",
                ),
            ),
        )
        whenever(userRepository.findAll()).thenReturn(
            listOf(
                user(java.util.UUID.fromString("00000000-0000-0000-0000-000000000201"), roles = listOf("FINANCE")),
                user(java.util.UUID.fromString("00000000-0000-0000-0000-000000000202"), roles = listOf("FINANCE", "OWNER")),
                user(java.util.UUID.fromString("00000000-0000-0000-0000-000000000203"), roles = listOf("ENGINEERING")),
                user(
                    java.util.UUID.fromString("00000000-0000-0000-0000-000000000204"),
                    organizationId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000205"),
                    roles = listOf("FINANCE"),
                ),
            ),
        )

        evaluator.on(prApproved())

        val captor = argumentCaptor<CreateNotificationRequest>()
        verify(notificationService, times(2)).publish(captor.capture(), eq(orgId))
        val recipients = captor.allValues.map { it.recipientUserId }.toSet()
        assertThat(
            recipients,
        ).containsExactlyInAnyOrder(
            java.util.UUID.fromString("00000000-0000-0000-0000-000000000201"),
            java.util.UUID.fromString("00000000-0000-0000-0000-000000000202"),
        )
    }

    @Test
    fun `a failing rule does not block the next rule from firing`() {
        whenever(
            workflowRuleService.findEnabledFor(orgId, NotificationKinds.LEAVE_REQUEST_APPROVED),
        ).thenReturn(
            listOf(
                rule(
                    id = java.util.UUID.fromString("00000000-0000-0000-0000-000000000209"),
                    actionType = WorkflowRuleActionType.NOTIFY_USER,
                    actionTarget = "00000000-0000-0000-0000-000000000211",
                ),
                rule(
                    id = java.util.UUID.fromString("00000000-0000-0000-0000-000000000210"),
                    actionType = WorkflowRuleActionType.NOTIFY_USER,
                    actionTarget = "00000000-0000-0000-0000-000000000302",
                ),
            ),
        )
        whenever(notificationService.publish(any(), eq(orgId)))
            .thenThrow(RuntimeException("first one explodes"))
            .thenReturn(stubNotification())

        evaluator.on(leaveApproved())

        verify(notificationService, times(2)).publish(any(), eq(orgId))
    }

    private fun leaveApproved() =
        LeaveRequestApprovedEvent(
            organizationId = orgId,
            leaveRequestId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000214"),
            requesterUserId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000213"),
            startDate = LocalDate.of(2026, 8, 1),
            endDate = LocalDate.of(2026, 8, 3),
            days = 3,
        )

    private fun prApproved() =
        PurchaseRequestApprovedEvent(
            organizationId = orgId,
            purchaseRequestId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000215"),
            prNumber = "PR-000123",
            requesterUserId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000213"),
        )

    private fun rule(
        id: java.util.UUID,
        actionType: WorkflowRuleActionType,
        actionTarget: String,
    ): WorkflowRule =
        WorkflowRule(
            id = id,
            organizationId = orgId,
            name = id.toString(),
            eventKind = "leave_request.approved",
            actionType = actionType,
            actionTarget = actionTarget,
            enabled = true,
        )

    private fun user(
        userId: java.util.UUID,
        organizationId: java.util.UUID = orgId,
        roles: List<String>,
    ): User =
        User(
            uuid = userId,
            username = userId.toString(),
            email = "$userId@example.com",
            firstName = "First",
            lastName = "Last",
            passwordHash = "x",
            organizationId = organizationId,
            roleAssignments = roles.map { RoleAssignment(role = it, organizationId = organizationId) },
        )

    private fun stubNotification(): com.aquinofroilan.tessera.domain.notification.model.Notification =
        com.aquinofroilan.tessera.domain.notification.model.Notification(
            organizationId = orgId,
            recipientUserId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000212"),
            category = NotificationCategory.INFO,
            kind = NotificationKinds.LEAVE_REQUEST_APPROVED,
            title = "Test",
        )
}
