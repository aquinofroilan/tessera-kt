package com.aquinofroilan.tessera.service.notification

import com.aquinofroilan.tessera.dto.CreateNotificationRequest
import com.aquinofroilan.tessera.event.LeaveRequestApprovedEvent
import com.aquinofroilan.tessera.event.LeaveRequestRejectedEvent
import com.aquinofroilan.tessera.event.PurchaseRequestApprovedEvent
import com.aquinofroilan.tessera.event.PurchaseRequestRejectedEvent
import com.aquinofroilan.tessera.model.NotificationCategory
import com.aquinofroilan.tessera.service.NotificationService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import java.time.LocalDate

class NotificationEventListenerTest {
    private lateinit var notificationService: NotificationService
    private lateinit var listener: NotificationEventListener

    private val orgId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000100")
    private val requesterUserId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000101")

    @BeforeEach
    fun setup() {
        notificationService = mock(NotificationService::class.java)
        listener = NotificationEventListener(notificationService)
    }

    @Test
    fun `LeaveRequestApproved produces an APPROVAL notification to the requester`() {
        val event =
            LeaveRequestApprovedEvent(
                organizationId = orgId,
                leaveRequestId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000301"),
                requesterUserId = requesterUserId,
                startDate = LocalDate.of(2026, 8, 1),
                endDate = LocalDate.of(2026, 8, 3),
                days = 3,
            )

        listener.on(event)

        val captured = captureRequest()
        assertThat(captured.recipientUserId).isEqualTo(requesterUserId)
        assertThat(captured.category).isEqualTo(NotificationCategory.APPROVAL)
        assertThat(captured.kind).isEqualTo(NotificationKinds.LEAVE_REQUEST_APPROVED)
        assertThat(captured.title).isEqualTo("Your leave request was approved")
        assertThat(captured.body).contains("2026-08-01", "2026-08-03", "3 days")
        assertThat(captured.link).isEqualTo("/hr/leave-requests/lr-1")
    }

    @Test
    fun `LeaveRequestRejected uses singular 'day' when needed and includes reason`() {
        val event =
            LeaveRequestRejectedEvent(
                organizationId = orgId,
                leaveRequestId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000302"),
                requesterUserId = requesterUserId,
                reason = "Conflicts with sprint demo",
            )

        listener.on(event)

        val captured = captureRequest()
        assertThat(captured.kind).isEqualTo(NotificationKinds.LEAVE_REQUEST_REJECTED)
        assertThat(captured.title).isEqualTo("Your leave request was rejected")
        assertThat(captured.body).isEqualTo("Reason: Conflicts with sprint demo")
    }

    @Test
    fun `LeaveRequestRejected leaves body null when reason is blank`() {
        val event =
            LeaveRequestRejectedEvent(
                organizationId = orgId,
                leaveRequestId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000303"),
                requesterUserId = requesterUserId,
                reason = "   ",
            )

        listener.on(event)

        assertThat(captureRequest().body).isNull()
    }

    @Test
    fun `PurchaseRequestApproved includes prNumber in title and links to detail`() {
        val event =
            PurchaseRequestApprovedEvent(
                organizationId = orgId,
                purchaseRequestId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000104"),
                prNumber = "PR-000123",
                requesterUserId = requesterUserId,
            )

        listener.on(event)

        val captured = captureRequest()
        assertThat(captured.kind).isEqualTo(NotificationKinds.PURCHASE_REQUEST_APPROVED)
        assertThat(captured.title).isEqualTo("Purchase request PR-000123 approved")
        assertThat(captured.link).isEqualTo("/procurement/purchase-requests/pr-1")
    }

    @Test
    fun `PurchaseRequestRejected forwards the rejection reason`() {
        val event =
            PurchaseRequestRejectedEvent(
                organizationId = orgId,
                purchaseRequestId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000402"),
                prNumber = "PR-000124",
                requesterUserId = requesterUserId,
                reason = "Out of budget this quarter",
            )

        listener.on(event)

        val captured = captureRequest()
        assertThat(captured.kind).isEqualTo(NotificationKinds.PURCHASE_REQUEST_REJECTED)
        assertThat(captured.title).isEqualTo("Purchase request PR-000124 rejected")
        assertThat(captured.body).isEqualTo("Reason: Out of budget this quarter")
    }

    private fun captureRequest(): CreateNotificationRequest {
        val captor = argumentCaptor<CreateNotificationRequest>()
        verify(notificationService).publish(captor.capture(), eq(orgId))
        return captor.firstValue
    }
}
