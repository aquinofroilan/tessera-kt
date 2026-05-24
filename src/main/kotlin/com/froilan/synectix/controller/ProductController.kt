package com.froilan.synectix.controller

import com.froilan.synectix.annotation.LogLevel
import com.froilan.synectix.annotation.Loggable
import com.froilan.synectix.dto.CreateProductRequest
import com.froilan.synectix.dto.ProductResponse
import com.froilan.synectix.dto.UpdateProductRequest
import com.froilan.synectix.model.Product
import com.froilan.synectix.security.AuthenticationContext
import com.froilan.synectix.service.ProductService
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
        @PathVariable id: String,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()

        val product = productService.getProduct(id, orgId)
        return ResponseEntity.ok(product.toResponse())
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('inventory:write')")
    fun updateProduct(
        @PathVariable id: String,
        @Valid @RequestBody request: UpdateProductRequest,
    ): ResponseEntity<Any> {
        val orgId = authContext.organizationId() ?: return authContext.unauthorized()

        val product = productService.updateProduct(id, request, orgId)
        return ResponseEntity.ok(product.toResponse())
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('inventory:write')")
    fun deleteProduct(
        @PathVariable id: String,
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
