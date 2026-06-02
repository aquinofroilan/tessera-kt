package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.NotificationPreferenceEntry
import com.aquinofroilan.tessera.model.NotificationChannel
import com.aquinofroilan.tessera.model.NotificationPreference
import com.aquinofroilan.tessera.repository.NotificationPreferenceRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Optional

class NotificationPreferenceServiceTest {
    private lateinit var repository: NotificationPreferenceRepository
    private lateinit var service: NotificationPreferenceService

<<<<<<< HEAD
    private val orgId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000100")
    private val userId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000101")
=======
    private val orgId = "org-1"
    private val userId = "user-1"
>>>>>>> 61cc253 (feat(notifications): per-user delivery preferences (channel x kind) (#251))

    @BeforeEach
    fun setup() {
        repository = mock(NotificationPreferenceRepository::class.java)
        whenever(repository.save(any<NotificationPreference>())).thenAnswer { it.arguments[0] }
        service = NotificationPreferenceService(repository)
    }

    @Test
    fun `isEnabled defaults to true when no row exists`() {
        whenever(
            repository.findByUserIdAndOrganizationIdAndKindAndChannel(
                userId,
                orgId,
                "leave_request.submitted",
                NotificationChannel.EMAIL,
            ),
        ).thenReturn(Optional.empty())

        assertThat(
            service.isEnabled(userId, orgId, "leave_request.submitted", NotificationChannel.EMAIL),
        ).isTrue()
    }

    @Test
    fun `isEnabled honours a stored opt-out`() {
        whenever(
            repository.findByUserIdAndOrganizationIdAndKindAndChannel(
                userId,
                orgId,
                "leave_request.submitted",
                NotificationChannel.EMAIL,
            ),
        ).thenReturn(Optional.of(pref(kind = "leave_request.submitted", channel = NotificationChannel.EMAIL, enabled = false)))

        assertThat(
            service.isEnabled(userId, orgId, "leave_request.submitted", NotificationChannel.EMAIL),
        ).isFalse()
    }

    @Test
    fun `upsertAll writes an opt-out row when enabled=false and no row exists`() {
        whenever(
            repository.findByUserIdAndOrganizationIdAndKindAndChannel(
                eq(userId),
                eq(orgId),
                any(),
                any<NotificationChannel>(),
            ),
        ).thenReturn(Optional.empty())
        whenever(repository.findByUserIdAndOrganizationId(userId, orgId)).thenReturn(emptyList())

        service.upsertAll(
            userId,
            orgId,
            listOf(
                NotificationPreferenceEntry(
                    kind = "leave_request.submitted",
                    channel = NotificationChannel.EMAIL,
                    enabled = false,
                ),
            ),
        )

        val captor = argumentCaptor<NotificationPreference>()
        verify(repository).save(captor.capture())
        val saved = captor.firstValue
        assertThat(saved.kind).isEqualTo("leave_request.submitted")
        assertThat(saved.channel).isEqualTo(NotificationChannel.EMAIL)
        assertThat(saved.enabled).isFalse()
    }

    @Test
    fun `upsertAll deletes an existing opt-out when the new value is enabled`() {
        val existing = pref(kind = "leave_request.submitted", channel = NotificationChannel.EMAIL, enabled = false)
        whenever(
            repository.findByUserIdAndOrganizationIdAndKindAndChannel(
                userId,
                orgId,
                "leave_request.submitted",
                NotificationChannel.EMAIL,
            ),
        ).thenReturn(Optional.of(existing))
        whenever(repository.findByUserIdAndOrganizationId(userId, orgId)).thenReturn(emptyList())

        service.upsertAll(
            userId,
            orgId,
            listOf(
                NotificationPreferenceEntry(
                    kind = "leave_request.submitted",
                    channel = NotificationChannel.EMAIL,
                    enabled = true,
                ),
            ),
        )

        verify(repository).delete(existing)
        verify(repository, never()).save(any<NotificationPreference>())
    }

    @Test
    fun `upsertAll writes nothing when enabled=true matches the default`() {
        whenever(
            repository.findByUserIdAndOrganizationIdAndKindAndChannel(
                eq(userId),
                eq(orgId),
                any(),
                any<NotificationChannel>(),
            ),
        ).thenReturn(Optional.empty())
        whenever(repository.findByUserIdAndOrganizationId(userId, orgId)).thenReturn(emptyList())

        service.upsertAll(
            userId,
            orgId,
            listOf(
                NotificationPreferenceEntry(
                    kind = "leave_request.submitted",
                    channel = NotificationChannel.EMAIL,
                    enabled = true,
                ),
            ),
        )

        verify(repository, never()).save(any<NotificationPreference>())
        verify(repository, never()).delete(any<NotificationPreference>())
    }

    private fun pref(
        kind: String,
        channel: NotificationChannel,
        enabled: Boolean,
    ): NotificationPreference =
        NotificationPreference(
            organizationId = orgId,
            userId = userId,
            kind = kind,
            channel = channel,
            enabled = enabled,
        )
}
