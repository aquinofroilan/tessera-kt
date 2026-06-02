package com.aquinofroilan.tessera.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.model.AssetStatus
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.service.asset.AssetReportService
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.Locale

@RestController
@RequestMapping("/assets/reports")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class AssetReportController(
    private val assetReportService: AssetReportService,
    private val authContext: AuthenticationContext,
) {
    @GetMapping("/register")
    @PreAuthorize("hasAuthority('assets:read')")
    fun assetRegister(
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) categoryId: String?,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val assetStatus =
            status?.let {
                try {
                    AssetStatus.valueOf(it.uppercase(Locale.ROOT))
                } catch (e: IllegalArgumentException) {
                    return ResponseEntity.badRequest().body(mapOf("error" to "Invalid status '$it'"))
                }
            }
        return ResponseEntity.ok(assetReportService.assetRegister(orgId, assetStatus, categoryId))
    }

    @GetMapping("/depreciation-schedule")
    @PreAuthorize("hasAuthority('assets:read')")
    fun depreciationSchedule(
        @RequestParam(required = false) assetId: String?,
        @RequestParam(required = false, defaultValue = "12") months: Int,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(assetReportService.depreciationSchedule(orgId, assetId, months))
    }
}
