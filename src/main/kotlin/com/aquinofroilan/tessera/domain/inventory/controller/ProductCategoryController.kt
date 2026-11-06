package com.aquinofroilan.tessera.domain.inventory.controller

import com.aquinofroilan.tessera.domain.inventory.dto.CreateProductCategoryRequest
import com.aquinofroilan.tessera.domain.inventory.dto.ProductCategoryResponse
import com.aquinofroilan.tessera.domain.inventory.dto.UpdateProductCategoryRequest
import com.aquinofroilan.tessera.domain.inventory.service.ProductCategoryService
import com.aquinofroilan.tessera.security.CurrentOrganizationId
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
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
@RequestMapping("/api/v1/inventory/categories")
class ProductCategoryController(
    private val service: ProductCategoryService,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('inventory:write')")
    fun create(
        @CurrentOrganizationId orgId: UUID,
        @Valid @RequestBody request: CreateProductCategoryRequest,
    ) = ResponseEntity.status(HttpStatus.CREATED).body(ProductCategoryResponse.from(service.create(request, orgId)))

    @GetMapping
    @PreAuthorize("hasAuthority('inventory:read')")
    fun list(
        @CurrentOrganizationId orgId: UUID,
        @RequestParam(defaultValue = "true") activeOnly: Boolean,
    ) = ResponseEntity.ok(service.list(orgId, activeOnly).map(ProductCategoryResponse::from))

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('inventory:read')")
    fun get(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: UUID,
    ) = ResponseEntity.ok(ProductCategoryResponse.from(service.get(id, orgId)))

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('inventory:write')")
    fun update(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: UUID,
        @Valid @RequestBody request: UpdateProductCategoryRequest,
    ) = ResponseEntity.ok(ProductCategoryResponse.from(service.update(id, request, orgId)))

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('inventory:write')")
    fun deactivate(
        @CurrentOrganizationId orgId: UUID,
        @PathVariable id: UUID,
    ) = ResponseEntity.ok(ProductCategoryResponse.from(service.deactivate(id, orgId)))
}
