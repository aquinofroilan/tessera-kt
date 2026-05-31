package com.aquinofroilan.tessera.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "products")
@EntityListeners(AuditingEntityListener::class)
data class Product(
    @Id
    @Column(columnDefinition = "uuid")
    val id: String = UUID.randomUUID().toString(),
    val sku: String,
    val name: String,
    val description: String? = null,
    val category: String? = null,
    @Column(name = "image_url")
    val imageUrl: String? = null,
    @Column(name = "list_price")
    val listPrice: BigDecimal,
    @Column(name = "price_currency", columnDefinition = "char(3)")
    val priceCurrency: String,
    @Column(name = "tax_group_id", columnDefinition = "uuid")
    val taxGroupId: String? = null,
    @Column(name = "organization_id", columnDefinition = "uuid")
    val organizationId: String,
    @Column(name = "is_active")
    val isActive: Boolean = true,
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null,
)
