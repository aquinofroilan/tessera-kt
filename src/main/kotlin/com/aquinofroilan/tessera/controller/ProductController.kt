package com.aquinofroilan.tessera.controller

import java.util.UUID

import com.aquinofroilan.tessera.annotation.LogLevel
import com.aquinofroilan.tessera.annotation.Loggable
import com.aquinofroilan.tessera.dto.CreateProductRequest
import com.aquinofroilan.tessera.dto.ProductResponse
import com.aquinofroilan.tessera.dto.UpdateProductRequest
import com.aquinofroilan.tessera.model.Product
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.service.ProductService
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

@RestController
@RequestMapping("/inventory/products")
@Loggable(logParameters = false, logReturnValue = false, level = LogLevel.INFO)
class ProductController(
    private val productService: ProductService,
    private val authContext: AuthenticationContext,
) {
    @PostMapping
    @PreAuthorize("hasAuthority('inventory:write')")
    fun createProduct(
        @Valid @RequestBody request: CreateProductRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()

        val product = productService.createProduct(request, orgId)
        return ResponseEntity.status(HttpStatus.CREATED).body(product.toResponse())
    }

    @GetMapping
    @PreAuthorize("hasAuthority('inventory:read')")
    fun listProducts(
        @RequestParam(required = false) category: String?,
        @RequestParam(required = false, defaultValue = "true") isActive: Boolean,
        @RequestParam(required = false) search: String?,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()

        val products = productService.listProducts(orgId, category, isActive, search)
        return ResponseEntity.ok(products.map { it.toResponse() })
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('inventory:read')")
    fun getProduct(
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()

        val product = productService.getProduct(id, orgId)
        return ResponseEntity.ok(product.toResponse())
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('inventory:write')")
    fun updateProduct(
        @PathVariable id: java.util.UUID,
        @Valid @RequestBody request: UpdateProductRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()

        val product = productService.updateProduct(id, request, orgId)
        return ResponseEntity.ok(product.toResponse())
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('inventory:write')")
    fun deleteProduct(
        @PathVariable id: java.util.UUID,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()

        val product = productService.deleteProduct(id, orgId)
        return ResponseEntity.ok(product.toResponse())
    }

    private fun Product.toResponse() =
        ProductResponse(
            id = id,
            sku = sku,
            name = name,
            description = description,
            category = category,
            imageUrl = imageUrl,
            listPrice = listPrice,
            priceCurrency = priceCurrency,
            taxGroupId = taxGroupId,
            organizationId = organizationId,
            isActive = isActive,
            createdAt = createdAt?.toString(),
            updatedAt = updatedAt?.toString(),
        )
}
