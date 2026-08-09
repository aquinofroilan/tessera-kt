package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.NotificationChannel
import com.aquinofroilan.tessera.model.NotificationPreference
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface NotificationPreferenceRepository : JpaRepository<NotificationPreference, String> {
    fun findByUserIdAndOrganizationId(
<<<<<<< HEAD
        userId: java.util.UUID,
        organizationId: java.util.UUID,
    ): List<NotificationPreference>

    fun findByUserIdAndOrganizationIdAndKindAndChannel(
        userId: java.util.UUID,
        organizationId: java.util.UUID,
=======
        userId: java.util.UUID,
        organizationId: java.util.UUID,
    ): List<NotificationPreference>

    fun findByUserIdAndOrganizationIdAndKindAndChannel(
        userId: java.util.UUID,
        organizationId: java.util.UUID,
>>>>>>> 61cc253 (feat(notifications): per-user delivery preferences (channel x kind) (#251))
        kind: String,
        channel: NotificationChannel,
    ): Optional<NotificationPreference>
}
