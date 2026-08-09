package com.aquinofroilan.tessera.service.notification

import com.aquinofroilan.tessera.config.NotificationEmailProperties
import com.aquinofroilan.tessera.model.Notification
import com.aquinofroilan.tessera.model.NotificationCategory
import com.aquinofroilan.tessera.model.NotificationEmailOutbox
import com.aquinofroilan.tessera.model.User
import com.aquinofroilan.tessera.repository.NotificationEmailOutboxRepository
import com.aquinofroilan.tessera.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Optional

class NotificationEmailEnqueuerTest {
    private lateinit var outboxRepository: NotificationEmailOutboxRepository
    private lateinit var userRepository: UserRepository
    private lateinit var enqueuer: NotificationEmailEnqueuer

    private val orgId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000100")
    private val userId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000101")

    @BeforeEach
    fun setup() {
        outboxRepository = mock(NotificationEmailOutboxRepository::class.java)
        userRepository = mock(UserRepository::class.java)
        whenever(outboxRepository.save(any<NotificationEmailOutbox>())).thenAnswer { it.arguments[0] }
        enqueuer = NotificationEmailEnqueuer(outboxRepository, userRepository, NotificationEmailProperties())
    }

    @Test
    fun `enqueue writes an outbox row snapshotting the recipient email`() {
        whenever(userRepository.findById(userId)).thenReturn(Optional.of(user(email = "alice@example.com")))

        enqueuer.enqueue(notification(id = java.util.UUID.fromString("00000000-0000-0000-0000-000000000001")))

        val captor = argumentCaptor<NotificationEmailOutbox>()
        verify(outboxRepository).save(captor.capture())
        assertThat(captor.firstValue.notificationId).isEqualTo(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"))
        assertThat(captor.firstValue.recipientEmail).isEqualTo("alice@example.com")
    }

    @Test
    fun `enqueue does nothing when the email channel is disabled`() {
        val disabled =
            NotificationEmailEnqueuer(
                outboxRepository,
                userRepository,
                NotificationEmailProperties(enabled = false),
            )

        disabled.enqueue(notification(id = java.util.UUID.fromString("00000000-0000-0000-0000-000000000001")))

        verify(outboxRepository, never()).save(any<NotificationEmailOutbox>())
    }

    @Test
    fun `enqueue does nothing when the recipient user is missing`() {
        whenever(userRepository.findById(userId)).thenReturn(Optional.empty())

        enqueuer.enqueue(notification(id = java.util.UUID.fromString("00000000-0000-0000-0000-000000000001")))

        verify(outboxRepository, never()).save(any<NotificationEmailOutbox>())
    }

    @Test
    fun `enqueue does nothing when the recipient has a blank email`() {
        whenever(userRepository.findById(userId)).thenReturn(Optional.of(user(email = "   ")))

        enqueuer.enqueue(notification(id = java.util.UUID.fromString("00000000-0000-0000-0000-000000000001")))

        verify(outboxRepository, never()).save(any<NotificationEmailOutbox>())
    }

    private fun notification(id: java.util.UUID): Notification =
        Notification(
            id = id,
            organizationId = orgId,
            recipientUserId = userId,
            category = NotificationCategory.INFO,
            kind = "test.kind",
            title = "Test",
        )

    private fun user(email: String): User =
        User(
            uuid = userId,
            username = "alice",
            email = email,
            firstName = "Alice",
            lastName = "Anderson",
            passwordHash = "x",
            organizationId = orgId,
        )
}
