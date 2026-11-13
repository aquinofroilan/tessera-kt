package com.aquinofroilan.tessera.domain.inventory.controller

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.domain.inventory.dto.ProductVariantResponse
import com.aquinofroilan.tessera.domain.inventory.service.ProductVariantService
import com.aquinofroilan.tessera.security.CurrentOrganizationId
import com.aquinofroilan.tessera.util.PaginationResponse
import com.aquinofroilan.tessera.util.optionalPageable
import org.springframework.data.domain.Sort
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/inventory/variants")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class OrganizationProductVariantController(
    private val variantService: ProductVariantService,
) {
    @GetMapping
    @PreAuthorize("hasAuthority('inventory:read')")
    fun list(
        @CurrentOrganizationId organizationId: UUID,
        @RequestParam(required = false) productId: UUID?,
        @RequestParam(required = false) isActive: Boolean?,
        @RequestParam(required = false) page: Int? = null,
        @RequestParam(required = false) size: Int? = null,
    ): ResponseEntity<Any> {
        val pageable =
            optionalPageable(
                page,
                size,
                Sort.by("productId").ascending().and(Sort.by("code").ascending()).and(Sort.by("id").ascending()),
            )
        return if (pageable.isUnpaged) {
            ResponseEntity.ok(
                variantService
                    .listOrganizationVariants(organizationId, productId, isActive)
                    .map(ProductVariantResponse::from),
            )
        } else {
            val variants = variantService.listOrganizationVariants(organizationId, productId, isActive, pageable)
            ResponseEntity.ok(PaginationResponse.from(variants.map(ProductVariantResponse::from)))
        }
    }
}
