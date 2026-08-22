package com.aquinofroilan.tessera.domain.assets.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.domain.assets.model.AssetStatus
import com.aquinofroilan.tessera.domain.assets.service.AssetReportService
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.security.CurrentOrganizationId
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.Locale
import java.util.UUID

@RestController
@RequestMapping("/api/v1/assets/reports")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class AssetReportController(
    private val assetReportService: AssetReportService,
    private val authContext: AuthenticationContext,
) {
    @GetMapping("/register")
    @PreAuthorize("hasAuthority('assets:read')")
    fun assetRegister(
        @CurrentOrganizationId orgId: UUID,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) categoryId: String?,
    ): ResponseEntity<Any> {
        val assetStatus =
            status?.let {
                try {
                    AssetStatus.valueOf(it.uppercase(Locale.ROOT))
                } catch (e: IllegalArgumentException) {
                    return ResponseEntity.badRequest().body(mapOf("error" to "Invalid status '$it'"))
                }
            }
        val categoryUuid =
            categoryId?.let {
                try {
                    UUID.fromString(it)
                } catch (e: IllegalArgumentException) {
                    return ResponseEntity.badRequest().body(mapOf("error" to "Invalid category ID"))
                }
            }
        return ResponseEntity.ok(assetReportService.assetRegister(orgId, assetStatus, categoryUuid))
    }

    @GetMapping("/depreciation-schedule")
    @PreAuthorize("hasAuthority('assets:read')")
    fun depreciationSchedule(
        @CurrentOrganizationId orgId: UUID,
        @RequestParam(required = false) assetId: String?,
        @RequestParam(required = false, defaultValue = "12") months: Int,
    ): ResponseEntity<Any> {
        val assetUuid =
            assetId?.let {
                try {
                    UUID.fromString(it)
                } catch (e: IllegalArgumentException) {
                    return ResponseEntity.badRequest().body(mapOf("error" to "Invalid asset ID"))
                }
            }
        return ResponseEntity.ok(assetReportService.depreciationSchedule(orgId, assetUuid, months))
    }
}
