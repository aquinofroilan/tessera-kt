package com.aquinofroilan.tessera.domain.assets.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.domain.assets.dto.AssetCategoryResponse
import com.aquinofroilan.tessera.domain.assets.dto.CreateAssetCategoryRequest
import com.aquinofroilan.tessera.domain.assets.dto.UpdateAssetCategoryRequest
import com.aquinofroilan.tessera.domain.assets.service.AssetCategoryService
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.security.CurrentOrganizationId
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
import java.util.UUID

@RestController
@RequestMapping("/api/v1/assets/categories")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class AssetCategoryController(
    private val assetCategoryService: AssetCategoryService,
    private val authContext: AuthenticationContext,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('assets:write')")
    fun createCategory(
        @CurrentOrganizationId orgId: UUID,
        @Valid @RequestBody request: CreateAssetCategoryRequest,
    ): ResponseEntity<Any> {
        val created = assetCategoryService.createCategory(request, orgId)
        return ResponseEntity.status(HttpStatus.CREATED).body(AssetCategoryResponse.from(created))
    }

    @GetMapping
    @PreAuthorize("hasAuthority('assets:read')")
    fun listCategories(
        @CurrentOrganizationId orgId: UUID,
        @RequestParam(required = false, defaultValue = "false") activeOnly: Boolean,
    ): ResponseEntity<Any> =
        ResponseEntity.ok(
            assetCategoryService.listCategories(orgId, activeOnly).map { AssetCategoryResponse.from(it) },
        )

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('assets:read')")
    fun getCategory(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: String,
    ): ResponseEntity<Any> {
        val categoryId =
            try {
                UUID.fromString(id)
            } catch (e: IllegalArgumentException) {
                return ResponseEntity.badRequest().body(mapOf("error" to "Invalid category ID"))
            }
        return ResponseEntity.ok(AssetCategoryResponse.from(assetCategoryService.getCategory(categoryId, orgId)))
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('assets:write')")
    fun updateCategory(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: String,
        @Valid @RequestBody request: UpdateAssetCategoryRequest,
    ): ResponseEntity<Any> {
        val categoryId =
            try {
                UUID.fromString(id)
            } catch (e: IllegalArgumentException) {
                return ResponseEntity.badRequest().body(mapOf("error" to "Invalid category ID"))
            }
        return ResponseEntity.ok(AssetCategoryResponse.from(assetCategoryService.updateCategory(categoryId, request, orgId)))
    }
}
