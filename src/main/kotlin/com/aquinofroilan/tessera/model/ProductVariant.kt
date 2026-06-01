package com.aquinofroilan.tessera.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "inventory_product_variants")
@EntityListeners(AuditingEntityListener::class)
data class ProductVariant(
    @Id
    @Column(columnDefinition = "uuid")
    val id: String = UUID.randomUUID().toString(),
    @Column(name = "organization_id", columnDefinition = "uuid")
    val organizationId: String,
    @Column(name = "product_id", columnDefinition = "uuid")
    val productId: String,
    val code: String,
    val name: String,
    @Column(name = "sku_suffix")
    val skuSuffix: String? = null,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    val attributes: Map<String, Any> = emptyMap(),
    @Column(name = "is_active")
    val isActive: Boolean = true,
    @CreatedDate
    @Column(name = "created_at")
    val createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    val updatedAt: LocalDateTime? = null,
)
