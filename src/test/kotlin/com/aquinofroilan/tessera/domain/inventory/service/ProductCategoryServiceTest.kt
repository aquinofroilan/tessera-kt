package com.aquinofroilan.tessera.domain.inventory.service

import com.aquinofroilan.tessera.domain.inventory.dto.CreateProductCategoryRequest
import com.aquinofroilan.tessera.domain.inventory.dto.UpdateProductCategoryRequest
import com.aquinofroilan.tessera.domain.inventory.model.ProductCategory
import com.aquinofroilan.tessera.domain.inventory.repository.ProductCategoryRepository
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import java.util.Optional
import java.util.UUID

class ProductCategoryServiceTest {
    private lateinit var repository: ProductCategoryRepository
    private lateinit var service: ProductCategoryService

    private val organizationId = UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d")
    private val otherOrganizationId = UUID.fromString("8576b8f7-dd04-3e57-b849-081b3776f223")

    @BeforeEach
    fun setUp() {
        repository = mock(ProductCategoryRepository::class.java)
        service = ProductCategoryService(repository)
    }

    @Test
    fun `create trims name and assigns current organization`() {
        `when`(repository.findByOrganizationIdAndNameIgnoreCase(organizationId, "Hardware")).thenReturn(null)
        `when`(repository.save(any<ProductCategory>())).thenAnswer { it.arguments[0] }

        val category = service.create(CreateProductCategoryRequest(" Hardware "), organizationId)

        assertThat(category.name).isEqualTo("Hardware")
        assertThat(category.organizationId).isEqualTo(organizationId)
    }

    @Test
    fun `create rejects duplicate category name in organization`() {
        `when`(repository.findByOrganizationIdAndNameIgnoreCase(organizationId, "Hardware"))
            .thenReturn(ProductCategory(organizationId = organizationId, name = "Hardware"))

        assertThrows<BusinessRuleException> {
            service.create(CreateProductCategoryRequest("Hardware"), organizationId)
        }
    }

    @Test
    fun `category from another organization is not found`() {
        val category = ProductCategory(organizationId = otherOrganizationId, name = "Private")
        `when`(repository.findById(category.id)).thenReturn(Optional.of(category))

        assertThrows<ResourceNotFoundException> { service.get(category.id, organizationId) }
    }

    @Test
    fun `update prevents renaming to an existing category`() {
        val category = ProductCategory(organizationId = organizationId, name = "Hardware")
        `when`(repository.findById(category.id)).thenReturn(Optional.of(category))
        `when`(repository.findByOrganizationIdAndNameIgnoreCase(organizationId, "Office"))
            .thenReturn(ProductCategory(organizationId = organizationId, name = "Office"))

        assertThrows<BusinessRuleException> {
            service.update(category.id, UpdateProductCategoryRequest(name = "Office"), organizationId)
        }
    }
}
