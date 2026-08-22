package com.aquinofroilan.tessera.domain.assets.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.domain.assets.dto.CreateFixedAssetRequest
import com.aquinofroilan.tessera.domain.assets.dto.FixedAssetResponse
import com.aquinofroilan.tessera.domain.assets.dto.UpdateFixedAssetRequest
import com.aquinofroilan.tessera.domain.assets.model.AssetStatus
import com.aquinofroilan.tessera.domain.assets.service.FixedAssetService
import com.aquinofroilan.tessera.security.AuthenticationContext
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.Locale
import java.util.UUID

@RestController
@RequestMapping("/api/v1/assets")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class FixedAssetController(
    private val fixedAssetService: FixedAssetService,
    private val authContext: AuthenticationContext,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('assets:write')")
    fun createAsset(
        @Valid @RequestBody request: CreateFixedAssetRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val created = fixedAssetService.createAsset(request, orgId)
        return ResponseEntity.status(HttpStatus.CREATED).body(FixedAssetResponse.from(created))
    }

    @GetMapping
    @PreAuthorize("hasAuthority('assets:read')")
    fun listAssets(
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
        val categoryUuid =
            categoryId?.let {
                try {
                    UUID.fromString(it)
                } catch (e: IllegalArgumentException) {
                    return ResponseEntity.badRequest().body(mapOf("error" to "Invalid category ID"))
                }
            }
        return ResponseEntity.ok(
            fixedAssetService.listAssets(orgId, assetStatus, categoryUuid).map { FixedAssetResponse.from(it) },
        )
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('assets:read')")
    fun getAsset(
        @PathVariable id: String,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val assetId =
            try {
                UUID.fromString(id)
            } catch (e: IllegalArgumentException) {
                return ResponseEntity.badRequest().body(mapOf("error" to "Invalid asset ID"))
            }
        return ResponseEntity.ok(FixedAssetResponse.from(fixedAssetService.getAsset(assetId, orgId)))
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('assets:write')")
    fun updateAsset(
        @PathVariable id: String,
        @Valid @RequestBody request: UpdateFixedAssetRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val assetId =
            try {
                UUID.fromString(id)
            } catch (e: IllegalArgumentException) {
                return ResponseEntity.badRequest().body(mapOf("error" to "Invalid asset ID"))
            }
        return ResponseEntity.ok(FixedAssetResponse.from(fixedAssetService.updateAsset(assetId, request, orgId)))
    }
}
