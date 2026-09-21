package com.aquinofroilan.tessera.domain.reporting.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "kpi_values")
@EntityListeners(AuditingEntityListener::class)
class KpiValue(
    @Id
    @Column(columnDefinition = "uuid")
    var id: UUID = UUID.randomUUID(),
    @Column(name = "kpi_id", columnDefinition = "uuid")
    var kpiId: UUID,
    @Column(name = "period_date")
    var periodDate: LocalDate,
    @Column(name = "actual_value")
    var actualValue: BigDecimal,
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null,
)
