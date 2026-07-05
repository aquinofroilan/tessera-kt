package com.aquinofroilan.tessera.model

import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "tax_groups")
@EntityListeners(AuditingEntityListener::class)
class TaxGroup(
    @Id
    @Column(columnDefinition = "uuid")
    var id: String = UUID.randomUUID().toString(),
    var name: String,
    var code: String,
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "tax_group_rates",
        joinColumns = [JoinColumn(name = "tax_group_id")],
    )
    @Column(name = "tax_rate_id", columnDefinition = "uuid")
    var taxRateIds: List<String>,
    @Column(name = "combined_rate")
    var combinedRate: BigDecimal,
    @Column(name = "organization_id", columnDefinition = "uuid")
    var organizationId: String,
    @Column(name = "is_active")
    var isActive: Boolean = true,
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null,
)
