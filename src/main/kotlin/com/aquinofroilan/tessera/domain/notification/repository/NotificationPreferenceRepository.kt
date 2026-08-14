package com.aquinofroilan.tessera.domain.notification.repository

import com.aquinofroilan.tessera.domain.notification.model.NotificationChannel
import com.aquinofroilan.tessera.domain.notification.model.NotificationPreference
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface NotificationPreferenceRepository : JpaRepository<NotificationPreference, String> {
    fun findByUserIdAndOrganizationId(
        userId: java.util.UUID,
        organizationId: java.util.UUID,
    ): List<NotificationPreference>

    fun findByUserIdAndOrganizationIdAndKindAndChannel(
        userId: java.util.UUID,
        organizationId: java.util.UUID,
        kind: String,
        channel: NotificationChannel,
    ): Optional<NotificationPreference>
}
