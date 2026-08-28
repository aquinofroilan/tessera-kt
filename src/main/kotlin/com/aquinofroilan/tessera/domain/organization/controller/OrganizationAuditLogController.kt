package com.aquinofroilan.tessera.domain.organization.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.domain.organization.dto.AuditLogResponse
import com.aquinofroilan.tessera.domain.organization.model.AuditCategory
import com.aquinofroilan.tessera.domain.organization.service.OrganizationAuditService
import com.aquinofroilan.tessera.security.CurrentOrganizationId
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime
import java.util.UUID

@RestController
@RequestMapping("/api/v1/organization/audit-logs")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class OrganizationAuditLogController(
    private val auditService: OrganizationAuditService,
) {
    @GetMapping
    @PreAuthorize("hasAuthority('organization:read') or hasRole('SUPER_ADMIN')")
    fun getAuditLogs(
        @CurrentOrganizationId orgId: UUID,
        @RequestParam(required = false) category: AuditCategory?,
        @RequestParam(required = false) action: String?,
        @RequestParam(required = false) entityType: String?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) from: LocalDateTime?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) to: LocalDateTime?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ResponseEntity<Page<AuditLogResponse>> {
        val pageable = PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, 100), Sort.by(Sort.Direction.DESC, "createdAt"))
        val result = auditService.getAuditLogs(orgId, category, action, entityType, from, to, pageable)
        return ResponseEntity.ok(result)
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('organization:read') or hasRole('SUPER_ADMIN')")
    fun getAuditLogById(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: UUID,
    ): ResponseEntity<AuditLogResponse> = ResponseEntity.ok(auditService.getAuditLogById(orgId, id))
}
