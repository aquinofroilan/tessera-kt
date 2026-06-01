package com.aquinofroilan.tessera.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.dto.CreateProductVariantRequest
import com.aquinofroilan.tessera.dto.ProductVariantResponse
import com.aquinofroilan.tessera.dto.UpdateProductVariantRequest
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.service.ProductVariantService
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
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/inventory/products/{productId}/variants")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class ProductVariantController(
    private val variantService: ProductVariantService,
    private val authContext: AuthenticationContext,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('inventory:write')")
    fun create(
        @PathVariable productId: String,
        @Valid @RequestBody request: CreateProductVariantRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        val v = variantService.createVariant(productId, request, orgId)
        return ResponseEntity.status(HttpStatus.CREATED).body(ProductVariantResponse.from(v))
    }

    @GetMapping
    @PreAuthorize("hasAuthority('inventory:read')")
    fun list(
        @PathVariable productId: String,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(variantService.listVariants(productId, orgId).map { ProductVariantResponse.from(it) })
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('inventory:read')")
    fun get(
        @PathVariable productId: String,
        @PathVariable id: String,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(ProductVariantResponse.from(variantService.getVariant(id, orgId)))
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('inventory:write')")
    fun update(
        @PathVariable productId: String,
        @PathVariable id: String,
        @Valid @RequestBody request: UpdateProductVariantRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(ProductVariantResponse.from(variantService.updateVariant(id, request, orgId)))
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('inventory:write')")
    fun deactivate(
        @PathVariable productId: String,
        @PathVariable id: String,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()
        return ResponseEntity.ok(ProductVariantResponse.from(variantService.deactivateVariant(id, orgId)))
    }
}
