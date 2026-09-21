package com.aquinofroilan.tessera.domain.reporting.model

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
import java.time.LocalDateTime
import java.util.UUID

enum class MetricType {
    NUMBER,
    CURRENCY,
    PERCENTAGE,
}

@Entity
@Table(name = "kpi_definitions")
@EntityListeners(AuditingEntityListener::class)
class KpiDefinition(
    @Id
    @Column(columnDefinition = "uuid")
    var id: UUID = UUID.randomUUID(),
    @Column(name = "organization_id", columnDefinition = "uuid")
    var organizationId: UUID,
    var name: String,
    var description: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "metric_type")
    var metricType: MetricType,
    @Column(name = "target_value")
    var targetValue: BigDecimal,
    @Column(name = "warning_threshold")
    var warningThreshold: BigDecimal? = null,
    @Column(name = "critical_threshold")
    var criticalThreshold: BigDecimal? = null,
    @Column(name = "is_higher_better")
    var higherIsBetter: Boolean = true,
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null,
)
