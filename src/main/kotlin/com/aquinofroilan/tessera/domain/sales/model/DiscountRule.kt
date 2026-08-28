package com.aquinofroilan.tessera.domain.sales.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

enum class DiscountType {
    PERCENTAGE,
    FIXED_AMOUNT,
    VOLUME_TIER,
}

@Entity
@Table(name = "discount_rules")
@EntityListeners(AuditingEntityListener::class)
class DiscountRule(
    @Id
    @Column(columnDefinition = "uuid")
    var id: UUID = UUID.ofEpochMillis(System.currentTimeMillis()),
    @Column(name = "organization_id", nullable = false, columnDefinition = "uuid")
    var organizationId: UUID,
    @Column(nullable = false)
    var name: String,
    @Column(nullable = false)
    var code: String,
    @Column(name = "discount_type", nullable = false)
    @Enumerated(EnumType.STRING)
    var discountType: DiscountType,
    @Column(name = "discount_value", nullable = false, precision = 19, scale = 4)
    var discountValue: BigDecimal,
    @Column(name = "customer_segment")
    @Enumerated(EnumType.STRING)
    var customerSegment: CustomerSegment? = null,
    @Column(name = "customer_id", columnDefinition = "uuid")
    var customerId: UUID? = null,
    @Column(name = "product_id", columnDefinition = "uuid")
    var productId: UUID? = null,
    @Column(name = "price_list_id", columnDefinition = "uuid")
    var priceListId: UUID? = null,
    @Column(name = "min_quantity", precision = 19, scale = 4)
    var minQuantity: BigDecimal? = null,
    @Column(name = "min_order_amount", precision = 19, scale = 4)
    var minOrderAmount: BigDecimal? = null,
    @Column(name = "valid_from")
    var validFrom: LocalDate? = null,
    @Column(name = "valid_to")
    var validTo: LocalDate? = null,
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,
    @Column(nullable = false)
    var priority: Int = 0,
    var description: String? = null,
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC),
    @LastModifiedDate
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC),
)
