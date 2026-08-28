package com.aquinofroilan.tessera.domain.notification.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.domain.notification.dto.NotificationPreferenceResponse
import com.aquinofroilan.tessera.domain.notification.dto.UpdateNotificationPreferencesRequest
import com.aquinofroilan.tessera.domain.notification.service.NotificationPreferenceService
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.security.CurrentOrganizationId
import com.aquinofroilan.tessera.security.CurrentUserId
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/notifications/preferences")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class NotificationPreferenceController(
    private val preferenceService: NotificationPreferenceService,
    private val authContext: AuthenticationContext,
) {
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    fun listMine(
        @CurrentUserId userId: UUID,
        @CurrentOrganizationId orgId: UUID,
    ): ResponseEntity<Any> =
        ResponseEntity.ok(
            preferenceService.listFor(userId, orgId).map { NotificationPreferenceResponse.from(it) },
        )

    @PutMapping
    @PreAuthorize("isAuthenticated()")
    fun updateMine(
        @CurrentUserId userId: UUID,
        @CurrentOrganizationId orgId: UUID,
        @Valid @RequestBody request: UpdateNotificationPreferencesRequest,
    ): ResponseEntity<Any> =
        ResponseEntity.ok(
            preferenceService.upsertAll(userId, orgId, request.preferences).map {
                NotificationPreferenceResponse.from(it)
            },
        )
}
