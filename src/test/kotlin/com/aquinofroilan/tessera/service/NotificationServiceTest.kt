package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.CreateNotificationRequest
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import com.aquinofroilan.tessera.model.Notification
import com.aquinofroilan.tessera.model.NotificationCategory
import com.aquinofroilan.tessera.repository.NotificationRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDateTime
import java.util.Optional

class NotificationServiceTest {
    private lateinit var repository: NotificationRepository
    private lateinit var service: NotificationService

    private val orgId = "org-1"
    private val userId = "user-1"

    @BeforeEach
    fun setup() {
        repository = mock(NotificationRepository::class.java)
        whenever(repository.save(any<Notification>())).thenAnswer { it.arguments[0] }
        service = NotificationService(repository)
    }

    @Test
    fun `publish persists a notification scoped to the org`() {
        val request =
            CreateNotificationRequest(
                recipientUserId = userId,
                category = NotificationCategory.APPROVAL,
                kind = "leave_request.submitted",
                title = "New leave request",
                body = "Alice requested 3 days",
                link = "/hr/leave-requests/abc",
            )

        val saved = service.publish(request, orgId)

        assertThat(saved.organizationId).isEqualTo(orgId)
        assertThat(saved.recipientUserId).isEqualTo(userId)
        assertThat(saved.category).isEqualTo(NotificationCategory.APPROVAL)
        assertThat(saved.kind).isEqualTo("leave_request.submitted")
        assertThat(saved.title).isEqualTo("New leave request")
        assertThat(saved.readAt).isNull()
    }

    @Test
    fun `listFor delegates to the org+recipient query ordered by createdAt`() {
        val rows = listOf(notification("a"), notification("b"))
        whenever(
            repository.findByRecipientUserIdAndOrganizationIdOrderByCreatedAtDesc(userId, orgId),
        ).thenReturn(rows)

        assertThat(service.listFor(userId, orgId)).isEqualTo(rows)
    }

    @Test
    fun `unreadCountFor returns the repo count`() {
        whenever(
            repository.countByRecipientUserIdAndOrganizationIdAndReadAtIsNull(userId, orgId),
        ).thenReturn(7L)

        assertThat(service.unreadCountFor(userId, orgId)).isEqualTo(7L)
    }

    @Test
    fun `markRead stamps readAt when the row is unread`() {
        val row = notification("a")
        whenever(repository.findById("a")).thenReturn(Optional.of(row))

        val updated = service.markRead("a", userId, orgId)

        assertThat(updated.readAt).isNotNull()
    }

    @Test
    fun `markRead is a no-op when the row is already read`() {
        val already = notification("a").copy(readAt = LocalDateTime.now())
        whenever(repository.findById("a")).thenReturn(Optional.of(already))

        val result = service.markRead("a", userId, orgId)

        assertThat(result.readAt).isEqualTo(already.readAt)
        verify(repository, never()).save(any<Notification>())
    }

    @Test
    fun `markRead 404s when the row belongs to a different org`() {
        val foreign = notification("a").copy(organizationId = "other-org")
        whenever(repository.findById("a")).thenReturn(Optional.of(foreign))

        assertThatThrownBy { service.markRead("a", userId, orgId) }
            .isInstanceOf(ResourceNotFoundException::class.java)
    }

    @Test
    fun `markRead 404s when the row belongs to a different user in the same org`() {
        val foreign = notification("a").copy(recipientUserId = "user-2")
        whenever(repository.findById("a")).thenReturn(Optional.of(foreign))

        assertThatThrownBy { service.markRead("a", userId, orgId) }
            .isInstanceOf(ResourceNotFoundException::class.java)
    }

    @Test
    fun `markAllRead reports the row count touched by the repo`() {
        whenever(
            repository.markAllReadFor(eq(userId), eq(orgId), any<LocalDateTime>()),
        ).thenReturn(4)

        assertThat(service.markAllRead(userId, orgId)).isEqualTo(4)
    }

    private fun notification(id: String): Notification =
        Notification(
            id = id,
            organizationId = orgId,
            recipientUserId = userId,
            category = NotificationCategory.INFO,
            kind = "test.kind",
            title = "Title $id",
        )
}
