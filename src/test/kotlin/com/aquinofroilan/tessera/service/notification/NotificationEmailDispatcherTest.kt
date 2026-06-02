package com.aquinofroilan.tessera.service.notification

import com.aquinofroilan.tessera.config.NotificationEmailProperties
import com.aquinofroilan.tessera.model.EmailDeliveryStatus
import com.aquinofroilan.tessera.model.Notification
import com.aquinofroilan.tessera.model.NotificationCategory
import com.aquinofroilan.tessera.model.NotificationEmailOutbox
import com.aquinofroilan.tessera.repository.NotificationEmailOutboxRepository
import com.aquinofroilan.tessera.repository.NotificationRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.data.domain.Limit
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Optional

class NotificationEmailDispatcherTest {
    private lateinit var outboxRepository: NotificationEmailOutboxRepository
    private lateinit var notificationRepository: NotificationRepository
    private lateinit var emailSender: EmailSender
    private lateinit var dispatcher: NotificationEmailDispatcher

    private val notification =
        Notification(
            id = java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"),
            organizationId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000100"),
            recipientUserId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000101"),
            category = NotificationCategory.APPROVAL,
            kind = "leave_request.submitted",
            title = "New leave request",
            body = "Alice requested 3 days",
            link = "/hr/leave-requests/abc",
        )

    @BeforeEach
    fun setup() {
        outboxRepository = mock(NotificationEmailOutboxRepository::class.java)
        notificationRepository = mock(NotificationRepository::class.java)
        emailSender = mock(EmailSender::class.java)
        whenever(outboxRepository.save(any<NotificationEmailOutbox>())).thenAnswer { it.arguments[0] }
        whenever(notificationRepository.findById(notification.id)).thenReturn(Optional.of(notification))
        dispatcher =
            NotificationEmailDispatcher(
                outboxRepository,
                notificationRepository,
                emailSender,
                NotificationEmailProperties(),
            )
    }

    @Test
    fun `successful send marks the row SENT with sent_at`() {
        givenPending(row(notificationId = notification.id))
        whenever(emailSender.send(any(), any(), any())).thenReturn(true)

        dispatcher.drain()

        val saved = capture()
        assertThat(saved.status).isEqualTo(EmailDeliveryStatus.SENT)
        assertThat(saved.attempts).isEqualTo(1)
        assertThat(saved.sentAt).isNotNull()
        assertThat(saved.lastError).isNull()
    }

    @Test
    fun `intentional skip from the sender marks the row SKIPPED`() {
        givenPending(row(notificationId = notification.id))
        whenever(emailSender.send(any(), any(), any())).thenReturn(false)

        dispatcher.drain()

        val saved = capture()
        assertThat(saved.status).isEqualTo(EmailDeliveryStatus.SKIPPED)
        assertThat(saved.attempts).isEqualTo(1)
    }

    @Test
    fun `transport failure with attempts left reschedules with linear backoff`() {
        givenPending(row(notificationId = notification.id))
        whenever(emailSender.send(any(), any(), any()))
            .thenThrow(RuntimeException("connection refused"))

        val beforeUtc = LocalDateTime.now(ZoneOffset.UTC)
        dispatcher.drain()

        val saved = capture()
        assertThat(saved.status).isEqualTo(EmailDeliveryStatus.PENDING)
        assertThat(saved.attempts).isEqualTo(1)
        assertThat(saved.lastError).isEqualTo("connection refused")
        // 1 attempt × 60s default backoff → at least ~50s ahead of pre-drain UTC.
        assertThat(saved.scheduledAt).isAfterOrEqualTo(beforeUtc.plusSeconds(50))
    }

    @Test
    fun `transport failure on the last allowed attempt marks the row FAILED`() {
        givenPending(row(notificationId = notification.id, attempts = 4))
        whenever(emailSender.send(any(), any(), any()))
            .thenThrow(RuntimeException("550 mailbox unavailable"))

        dispatcher.drain()

        val saved = capture()
        assertThat(saved.status).isEqualTo(EmailDeliveryStatus.FAILED)
        assertThat(saved.attempts).isEqualTo(5)
        assertThat(saved.lastError).isEqualTo("550 mailbox unavailable")
    }

    @Test
    fun `missing notification row marks the outbox row FAILED instead of dispatching`() {
        givenPending(row(notificationId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000999")))
        whenever(notificationRepository.findById(java.util.UUID.fromString("00000000-0000-0000-0000-000000000999"))).thenReturn(Optional.empty())

        dispatcher.drain()

        val saved = capture()
        assertThat(saved.status).isEqualTo(EmailDeliveryStatus.FAILED)
        verify(emailSender, org.mockito.Mockito.never()).send(any(), any(), any())
    }

    @Test
    fun `drain is a no-op when no rows are due`() {
        givenPending()

        dispatcher.drain()

        verify(emailSender, org.mockito.Mockito.never()).send(any(), any(), any())
        verify(outboxRepository, org.mockito.Mockito.never()).save(any<NotificationEmailOutbox>())
    }

    private fun givenPending(vararg rows: NotificationEmailOutbox) {
        whenever(
            outboxRepository.findByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc(
                eq(EmailDeliveryStatus.PENDING),
                any<LocalDateTime>(),
                any<Limit>(),
            ),
        ).thenReturn(rows.toList())
    }

    private fun capture(): NotificationEmailOutbox {
        val captor = argumentCaptor<NotificationEmailOutbox>()
        verify(outboxRepository).save(captor.capture())
        return captor.firstValue
    }

    private fun row(
        notificationId: java.util.UUID,
        attempts: Int = 0,
    ): NotificationEmailOutbox =
        NotificationEmailOutbox(
            id = "outbox-$notificationId",
            notificationId = notificationId,
            recipientEmail = "alice@example.com",
            attempts = attempts,
        )
}
