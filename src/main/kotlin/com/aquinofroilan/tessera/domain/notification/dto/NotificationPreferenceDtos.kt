package com.aquinofroilan.tessera.domain.notification.dto

import com.aquinofroilan.tessera.domain.notification.model.NotificationChannel
import com.aquinofroilan.tessera.domain.notification.model.NotificationPreference
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class NotificationPreferenceEntry(
    @field:NotBlank(message = "Kind is required")
    @field:Size(max = 100)
    val kind: String,
    @field:NotNull(message = "Channel is required")
    val channel: NotificationChannel?,
    @field:NotNull(message = "Enabled flag is required")
    val enabled: Boolean?,
)

data class UpdateNotificationPreferencesRequest(
    @field:Valid
    val preferences: List<NotificationPreferenceEntry> = emptyList(),
)

data class NotificationPreferenceResponse(
    val kind: String,
    val channel: NotificationChannel,
    val enabled: Boolean,
    val updatedAt: String?,
) {
    companion object {
        fun from(pref: NotificationPreference) =
            NotificationPreferenceResponse(
                kind = pref.kind,
                channel = pref.channel,
                enabled = pref.enabled,
                updatedAt = (pref.updatedAt ?: pref.createdAt)?.toString(),
            )
    }
}
