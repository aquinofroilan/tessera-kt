package com.aquinofroilan.tessera.domain.sales.model

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

@Entity
@Table(name = "price_list_lines")
class PriceListLine(
    @Id
    @Column(columnDefinition = "uuid")
    var id: UUID = UUID.ofEpochMillis(System.currentTimeMillis()),
    @Column(name = "price_list_id", nullable = false, columnDefinition = "uuid")
    var priceListId: UUID,
    @Column(name = "product_id", nullable = false, columnDefinition = "uuid")
    var productId: UUID,
    @Column(name = "product_sku", nullable = false)
    var productSku: String,
    @Column(name = "unit_price", nullable = false, precision = 19, scale = 4)
    var unitPrice: BigDecimal,
    @Column(name = "min_quantity", nullable = false, precision = 19, scale = 4)
    var minQuantity: BigDecimal = BigDecimal.ONE,
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC),
    @LastModifiedDate
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC),
)

@Entity
@Table(name = "price_lists")
@EntityListeners(AuditingEntityListener::class)
class PriceList(
    @Id
    @Column(columnDefinition = "uuid")
    var id: UUID = UUID.ofEpochMillis(System.currentTimeMillis()),
    @Column(name = "organization_id", nullable = false, columnDefinition = "uuid")
    var organizationId: UUID,
    @Column(nullable = false)
    var name: String,
    @Column(nullable = false)
    var code: String,
    @Column(nullable = false)
    var currency: String,
    @Column(name = "customer_segment")
    @Enumerated(EnumType.STRING)
    var customerSegment: CustomerSegment? = null,
    @Column(name = "is_default", nullable = false)
    var isDefault: Boolean = false,
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,
    @Column(name = "valid_from")
    var validFrom: LocalDate? = null,
    @Column(name = "valid_to")
    var validTo: LocalDate? = null,
    var description: String? = null,
    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "price_list_id")
    var lines: MutableList<PriceListLine> = mutableListOf(),
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC),
    @LastModifiedDate
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC),
)
