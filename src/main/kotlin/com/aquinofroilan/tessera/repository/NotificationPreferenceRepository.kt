package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.NotificationChannel
import com.aquinofroilan.tessera.model.NotificationPreference
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface NotificationPreferenceRepository : JpaRepository<NotificationPreference, String> {
    fun findByUserIdAndOrganizationId(
        userId: String,
        organizationId: String,
    ): List<NotificationPreference>

    fun findByUserIdAndOrganizationIdAndKindAndChannel(
        userId: String,
        organizationId: String,
        kind: String,
        channel: NotificationChannel,
    ): Optional<NotificationPreference>
}
