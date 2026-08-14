package com.aquinofroilan.tessera.domain.finance.model

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
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

enum class FiscalYearStatus {
    ACTIVE,
    CLOSED,
}

enum class FiscalPeriodStatus {
    OPEN,
    CLOSED,
    REOPENED,
}

@Entity
@Table(name = "fiscal_periods")
class FiscalPeriod(
    @Id
    @Column(columnDefinition = "uuid")
    var id: java.util.UUID = java.util.UUID.ofEpochMillis(System.currentTimeMillis()),
    @Column(name = "period_number")
    var periodNumber: Int,
    var name: String,
    @Column(name = "start_date")
    var startDate: LocalDate,
    @Column(name = "end_date")
    var endDate: LocalDate,
    @Enumerated(EnumType.STRING)
    var status: FiscalPeriodStatus = FiscalPeriodStatus.OPEN,
    @Column(name = "closed_at")
    var closedAt: LocalDateTime? = null,
    @Column(name = "closed_by", columnDefinition = "uuid")
    var closedBy: java.util.UUID? = null,
    @Column(name = "reopened_at")
    var reopenedAt: LocalDateTime? = null,
    @Column(name = "reopened_by", columnDefinition = "uuid")
    var reopenedBy: java.util.UUID? = null,
)

@Entity
@Table(name = "fiscal_years")
@EntityListeners(AuditingEntityListener::class)
class FiscalYear(
    @Id
    @Column(columnDefinition = "uuid")
    var id: java.util.UUID = java.util.UUID.ofEpochMillis(System.currentTimeMillis()),
    var name: String,
    @Column(name = "start_date")
    var startDate: LocalDate,
    @Column(name = "end_date")
    var endDate: LocalDate,
    @Enumerated(EnumType.STRING)
    var status: FiscalYearStatus = FiscalYearStatus.ACTIVE,
    @Column(name = "organization_id", columnDefinition = "uuid")
    var organizationId: java.util.UUID,
    @OneToMany(
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.EAGER,
    )
    @JoinColumn(name = "fiscal_year_id")
    @OrderBy("periodNumber ASC")
    var periods: List<FiscalPeriod> = emptyList(),
    @Column(name = "closed_at")
    var closedAt: LocalDateTime? = null,
    @Column(name = "closed_by", columnDefinition = "uuid")
    var closedBy: java.util.UUID? = null,
    @Column(name = "closing_entry_id", columnDefinition = "uuid")
    var closingEntryId: java.util.UUID? = null,
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null,
)
