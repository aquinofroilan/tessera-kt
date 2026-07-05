package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.CreateProductRequest
import com.aquinofroilan.tessera.dto.UpdateProductRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import com.aquinofroilan.tessera.model.Product
import com.aquinofroilan.tessera.repository.OrganizationRepository
import com.aquinofroilan.tessera.repository.ProductRepository
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ProductService(
    private val productRepository: ProductRepository,
    private val organizationRepository: OrganizationRepository,
    private val currencyService: CurrencyService,
    private val taxGroupService: TaxGroupService,
) {
    @Transactional
    fun createProduct(
        request: CreateProductRequest,
        organizationId: String,
    ): Product {
        val sku = requireNonBlankTrimmed(request.sku, "SKU")
        val name = requireNonBlankTrimmed(request.name, "Product name")
        val priceCurrency = (request.priceCurrency ?: getBaseCurrency(organizationId)).uppercase()
        currencyService.getCurrency(priceCurrency)

        if (request.taxGroupId != null) {
            validateTaxGroup(request.taxGroupId, organizationId)
        }

        val product =
            Product(
                sku = sku,
                name = name,
                description = request.description,
                category = request.category,
                imageUrl = request.imageUrl,
                listPrice = request.listPrice,
                priceCurrency = priceCurrency,
                taxGroupId = request.taxGroupId,
                organizationId = organizationId,
            )

        return try {
            productRepository.save(product)
        } catch (e: DuplicateKeyException) {
            throw BusinessRuleException(
                "Product with SKU '$sku' already exists in this organization",
                e,
            )
        }
    }

    fun getProduct(
        productId: String,
        organizationId: String,
    ): Product {
        val product =
            productRepository.findById(productId).orElseThrow {
                ResourceNotFoundException("Product not found")
            }
        if (product.organizationId != organizationId) {
            throw ResourceNotFoundException("Product not found")
        }
        return product
    }

    fun listProducts(
        organizationId: String,
        category: String? = null,
        isActive: Boolean = true,
        search: String? = null,
    ): List<Product> = productRepository.search(organizationId, isActive, category, search)

    @Transactional
    fun updateProduct(
        productId: String,
        request: UpdateProductRequest,
        organizationId: String,
    ): Product {
        val existing = getProduct(productId, organizationId)
        if (!existing.isActive) {
            throw BusinessRuleException("Cannot update inactive product")
        }

        val newCurrency =
            request.priceCurrency?.uppercase()?.also { currencyService.getCurrency(it) }
                ?: existing.priceCurrency

        if (request.taxGroupId != null && request.taxGroupId != existing.taxGroupId) {
            validateTaxGroup(request.taxGroupId, organizationId)
        }

        val newName =
            request.name?.let { requireNonBlankTrimmed(it, "Product name") } ?: existing.name

        existing.apply {
            name = newName
            description = request.description ?: existing.description
            category = request.category ?: existing.category
            imageUrl = request.imageUrl ?: existing.imageUrl
            listPrice = request.listPrice ?: existing.listPrice
            priceCurrency = newCurrency
            taxGroupId = request.taxGroupId ?: existing.taxGroupId
        }

        return productRepository.save(existing)
    }

    private fun requireNonBlankTrimmed(
        value: String,
        fieldLabel: String,
    ): String {
        val trimmed = value.trim()
        if (trimmed.isEmpty()) {
            throw BusinessRuleException("$fieldLabel must not be blank")
        }
        return trimmed
    }

    @Transactional
    fun deleteProduct(
        productId: String,
        organizationId: String,
    ): Product {
        val product = getProduct(productId, organizationId)
        if (!product.isActive) {
            throw BusinessRuleException("Product is already inactive")
        }
        // TODO: when #41/#10/#9 land, block hard delete (or even soft delete) if product
        // is referenced by stock movements, sales lines, or purchase lines.
        product.isActive = false
        return productRepository.save(product)
    }

    private fun validateTaxGroup(
        taxGroupId: String,
        organizationId: String,
    ) {
        val group = taxGroupService.getTaxGroup(taxGroupId, organizationId)
        if (!group.isActive) {
            throw BusinessRuleException("Tax group '${group.code}' is inactive")
        }
    }

    private fun getBaseCurrency(organizationId: String): String =
        organizationRepository
            .findById(organizationId)
            .orElseThrow {
                ResourceNotFoundException("Organization not found")
            }.baseCurrency
}
