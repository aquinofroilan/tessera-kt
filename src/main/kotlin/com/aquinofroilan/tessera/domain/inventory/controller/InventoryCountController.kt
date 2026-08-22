package com.aquinofroilan.tessera.domain.inventory.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.domain.inventory.dto.CountSessionResponse
import com.aquinofroilan.tessera.domain.inventory.dto.CreateCountSessionRequest
import com.aquinofroilan.tessera.domain.inventory.dto.RecordCountRequest
import com.aquinofroilan.tessera.domain.inventory.model.InventoryCountStatus
import com.aquinofroilan.tessera.domain.inventory.service.InventoryCountSessionService
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
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.Locale
import java.util.UUID

@RestController
@RequestMapping("/api/v1/inventory/counts")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class InventoryCountController(
    private val countService: InventoryCountSessionService,
    private val authContext: AuthenticationContext,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('inventory:write')")
    fun create(
        @CurrentUserId userId: UUID,
        @CurrentOrganizationId orgId: UUID,
        @Valid @RequestBody request: CreateCountSessionRequest,
    ): ResponseEntity<Any> {
        val s = countService.createSession(request, orgId, userId)
        return ResponseEntity.status(HttpStatus.CREATED).body(CountSessionResponse.from(s))
    }

    @GetMapping
    @PreAuthorize("hasAuthority('inventory:read')")
    fun list(
        @CurrentOrganizationId orgId: UUID,
        @RequestParam(required = false) status: String?,
    ): ResponseEntity<Any> {
        val parsed =
            if (status != null) {
                try {
                    InventoryCountStatus.valueOf(status.uppercase(Locale.ROOT))
                } catch (e: IllegalArgumentException) {
                    return ResponseEntity.badRequest().body(mapOf("error" to "Invalid status '$status'"))
                }
            } else {
                null
            }
        return ResponseEntity.ok(countService.listSessions(orgId, parsed).map { CountSessionResponse.from(it) })
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('inventory:read')")
    fun get(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: UUID,
    ): ResponseEntity<Any> = ResponseEntity.ok(CountSessionResponse.from(countService.getSession(id, orgId)))

    @PostMapping("/{id}/lines/{lineId}/record-count")
    @PreAuthorize("hasAuthority('inventory:write')")
    fun recordCount(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: UUID,
        @PathVariable lineId: UUID,
        @Valid @RequestBody request: RecordCountRequest,
    ): ResponseEntity<Any> = ResponseEntity.ok(CountSessionResponse.from(countService.recordCount(id, lineId, request, orgId)))

    @PostMapping("/{id}/post")
    @PreAuthorize("hasAuthority('inventory:write')")
    fun post(
        @CurrentUserId userId: UUID,
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: UUID,
    ): ResponseEntity<Any> = ResponseEntity.ok(CountSessionResponse.from(countService.postSession(id, orgId, userId)))

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('inventory:write')")
    fun cancel(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: UUID,
    ): ResponseEntity<Any> = ResponseEntity.ok(CountSessionResponse.from(countService.cancelSession(id, orgId)))
}
