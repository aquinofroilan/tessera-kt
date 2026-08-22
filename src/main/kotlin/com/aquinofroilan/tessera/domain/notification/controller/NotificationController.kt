package com.aquinofroilan.tessera.domain.notification.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.domain.notification.dto.CreateNotificationRequest
import com.aquinofroilan.tessera.domain.notification.dto.NotificationMarkAllReadResponse
import com.aquinofroilan.tessera.domain.notification.dto.NotificationResponse
import com.aquinofroilan.tessera.domain.notification.dto.NotificationUnreadCountResponse
import com.aquinofroilan.tessera.domain.notification.service.NotificationService
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.security.CurrentOrganizationId
import com.aquinofroilan.tessera.security.CurrentUserId
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/**
 * In-app notification feed for the authenticated caller. Every endpoint
 * resolves the recipient from the auth context, so a user can only see
 * their own notifications — no need for a per-row permission check.
 *
 * The single publish endpoint requires [Permissions.NOTIFICATION_WRITE]
 * and is intended for admin / system creation paths until the event-bus
 * sub-PR lands and services publish via [NotificationService.publish]
 * directly.
 */
@RestController
@RequestMapping("/api/v1/notifications")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class NotificationController(
    private val notificationService: NotificationService,
    private val authContext: AuthenticationContext,
) {
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    fun listMine(
        @CurrentUserId userId: UUID,
        @CurrentOrganizationId orgId: UUID,
    ): ResponseEntity<Any> = ResponseEntity.ok(notificationService.listFor(userId, orgId).map { NotificationResponse.from(it) })

    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    fun unreadCount(
        @CurrentUserId userId: UUID,
        @CurrentOrganizationId orgId: UUID,
    ): ResponseEntity<Any> = ResponseEntity.ok(NotificationUnreadCountResponse(notificationService.unreadCountFor(userId, orgId)))

    @PostMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    fun markRead(
        @CurrentUserId userId: UUID,
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> = ResponseEntity.ok(NotificationResponse.from(notificationService.markRead(id, userId, orgId)))

    @PostMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    fun markAllRead(
        @CurrentUserId userId: UUID,
        @CurrentOrganizationId orgId: UUID,
    ): ResponseEntity<Any> = ResponseEntity.ok(NotificationMarkAllReadResponse(notificationService.markAllRead(userId, orgId)))

    @PostMapping
    @PreAuthorize("hasAuthority('notification:write')")
    fun publish(
        @CurrentOrganizationId orgId: UUID,
        @Valid @RequestBody request: CreateNotificationRequest,
    ): ResponseEntity<Any> {
        val created = notificationService.publish(request, orgId)
        return ResponseEntity.status(HttpStatus.CREATED).body(NotificationResponse.from(created))
    }
}
