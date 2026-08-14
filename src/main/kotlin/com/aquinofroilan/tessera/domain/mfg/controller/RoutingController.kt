package com.aquinofroilan.tessera.domain.mfg.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.domain.mfg.dto.CreateRoutingRequest
import com.aquinofroilan.tessera.domain.mfg.dto.RoutingResponse
import com.aquinofroilan.tessera.domain.mfg.dto.UpdateRoutingRequest
import com.aquinofroilan.tessera.domain.mfg.model.RoutingStatus
import com.aquinofroilan.tessera.domain.mfg.service.RoutingService
import com.aquinofroilan.tessera.security.AuthenticationContext
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.Locale

@RestController
@RequestMapping("/api/v1/mfg/routings")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class RoutingController(
    private val routingService: RoutingService,
    private val authContext: AuthenticationContext,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('mfg:write')")
    fun create(
        @Valid @RequestBody request: CreateRoutingRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val userId = authContext.userId()?.toString() ?: "api-key"
        val routing = routingService.createRouting(request, orgId, userId)
        return ResponseEntity.status(HttpStatus.CREATED).body(RoutingResponse.from(routing))
    }

    @GetMapping
    @PreAuthorize("hasAuthority('mfg:read')")
    fun list(
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) productId: java.util.UUID?,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val parsed =
            if (status != null) {
                try {
                    RoutingStatus.valueOf(status.uppercase(Locale.ROOT))
                } catch (e: IllegalArgumentException) {
                    return ResponseEntity.badRequest().body(mapOf("error" to "Invalid status '$status'"))
                }
            } else {
                null
            }
        return ResponseEntity.ok(routingService.listRoutings(orgId, parsed, productId).map { RoutingResponse.from(it) })
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('mfg:read')")
    fun get(
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(RoutingResponse.from(routingService.getRouting(id, orgId)))
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('mfg:write')")
    fun update(
        @PathVariable id: java.util.UUID,
        @Valid @RequestBody request: UpdateRoutingRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(RoutingResponse.from(routingService.updateRouting(id, request, orgId)))
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('mfg:approve')")
    fun activate(
        @PathVariable id: java.util.UUID,
        @RequestParam(required = false, defaultValue = "false") makeDefault: Boolean,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val userId = authContext.userId()?.toString() ?: "api-key"
        return ResponseEntity.ok(RoutingResponse.from(routingService.activateRouting(id, orgId, userId, makeDefault)))
    }

    @PostMapping("/{id}/obsolete")
    @PreAuthorize("hasAuthority('mfg:approve')")
    fun obsolete(
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val userId = authContext.userId()?.toString() ?: "api-key"
        return ResponseEntity.ok(RoutingResponse.from(routingService.obsoleteRouting(id, orgId, userId)))
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('mfg:write')")
    fun delete(
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        routingService.deleteRouting(id, orgId)
        return ResponseEntity.noContent().build()
    }
}
