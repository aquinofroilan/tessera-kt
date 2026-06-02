package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.NotificationPreferenceEntry
import com.aquinofroilan.tessera.model.NotificationChannel
import com.aquinofroilan.tessera.model.NotificationPreference
import com.aquinofroilan.tessera.repository.NotificationPreferenceRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Per-user notification delivery preferences. The contract is:
 *
 * - Default state for every (user, kind, channel) tuple is **enabled**.
 * - Only explicit deviations are persisted; flipping a tuple back to
 *   enabled=true deletes the row instead of storing the default again.
 * - [isEnabled] is the read path consumed by enqueuers / listeners. It
 *   returns true unless a row exists with enabled=false.
 */
@Service
class NotificationPreferenceService(
    private val preferenceRepository: NotificationPreferenceRepository,
) {
    fun isEnabled(
<<<<<<< HEAD
        userId: java.util.UUID,
        organizationId: java.util.UUID,
=======
        userId: String,
        organizationId: String,
>>>>>>> 61cc253 (feat(notifications): per-user delivery preferences (channel x kind) (#251))
        kind: String,
        channel: NotificationChannel,
    ): Boolean =
        preferenceRepository
            .findByUserIdAndOrganizationIdAndKindAndChannel(userId, organizationId, kind, channel)
            .map { it.enabled }
            .orElse(true)

    fun listFor(
<<<<<<< HEAD
        userId: java.util.UUID,
        organizationId: java.util.UUID,
=======
        userId: String,
        organizationId: String,
>>>>>>> 61cc253 (feat(notifications): per-user delivery preferences (channel x kind) (#251))
    ): List<NotificationPreference> = preferenceRepository.findByUserIdAndOrganizationId(userId, organizationId)

    @Transactional
    fun upsertAll(
<<<<<<< HEAD
        userId: java.util.UUID,
        organizationId: java.util.UUID,
=======
        userId: String,
        organizationId: String,
>>>>>>> 61cc253 (feat(notifications): per-user delivery preferences (channel x kind) (#251))
        entries: List<NotificationPreferenceEntry>,
    ): List<NotificationPreference> {
        entries.forEach { entry ->
            val kind = entry.kind.trim()
            val channel = entry.channel ?: error("Channel validated upstream")
            val enabled = entry.enabled ?: error("Enabled flag validated upstream")
            val existing =
                preferenceRepository.findByUserIdAndOrganizationIdAndKindAndChannel(
                    userId,
                    organizationId,
                    kind,
                    channel,
                )
            if (enabled) {
                existing.ifPresent { preferenceRepository.delete(it) }
            } else {
                val next =
                    existing
                        .map { it.copy(enabled = false) }
                        .orElseGet {
                            NotificationPreference(
                                organizationId = organizationId,
                                userId = userId,
                                kind = kind,
                                channel = channel,
                                enabled = false,
                            )
                        }
                preferenceRepository.save(next)
            }
        }
        return listFor(userId, organizationId)
    }
}
