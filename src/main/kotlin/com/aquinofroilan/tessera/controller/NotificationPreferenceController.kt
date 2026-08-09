package com.aquinofroilan.tessera.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.dto.NotificationPreferenceResponse
import com.aquinofroilan.tessera.dto.UpdateNotificationPreferencesRequest
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.service.NotificationPreferenceService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/notifications/preferences")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class NotificationPreferenceController(
    private val preferenceService: NotificationPreferenceService,
    private val authContext: AuthenticationContext,
) {
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    fun listMine(): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val userId = authContext.userId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(
            preferenceService.listFor(userId, orgId).map { NotificationPreferenceResponse.from(it) },
        )
    }

    @PutMapping
    @PreAuthorize("isAuthenticated()")
    fun updateMine(
        @Valid @RequestBody request: UpdateNotificationPreferencesRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val userId = authContext.userId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(
            preferenceService.upsertAll(userId, orgId, request.preferences).map {
                NotificationPreferenceResponse.from(it)
            },
        )
    }
}
