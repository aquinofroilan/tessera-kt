package com.aquinofroilan.tessera.model

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
data class FiscalPeriod(
    @Id
    @Column(columnDefinition = "uuid")
    val id: String = UUID.randomUUID().toString(),
    @Column(name = "period_number")
    val periodNumber: Int,
    val name: String,
    @Column(name = "start_date")
    val startDate: LocalDate,
    @Column(name = "end_date")
    val endDate: LocalDate,
    @Enumerated(EnumType.STRING)
    val status: FiscalPeriodStatus = FiscalPeriodStatus.OPEN,
    @Column(name = "closed_at")
    val closedAt: LocalDateTime? = null,
    @Column(name = "closed_by", columnDefinition = "uuid")
    val closedBy: String? = null,
    @Column(name = "reopened_at")
    val reopenedAt: LocalDateTime? = null,
    @Column(name = "reopened_by", columnDefinition = "uuid")
    val reopenedBy: String? = null,
)

@Entity
@Table(name = "fiscal_years")
@EntityListeners(AuditingEntityListener::class)
data class FiscalYear(
    @Id
    @Column(columnDefinition = "uuid")
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    @Column(name = "start_date")
    val startDate: LocalDate,
    @Column(name = "end_date")
    val endDate: LocalDate,
    @Enumerated(EnumType.STRING)
    val status: FiscalYearStatus = FiscalYearStatus.ACTIVE,
    @Column(name = "organization_id", columnDefinition = "uuid")
    val organizationId: String,
    @OneToMany(
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.EAGER,
    )
    @JoinColumn(name = "fiscal_year_id")
    @OrderBy("periodNumber ASC")
    val periods: List<FiscalPeriod> = emptyList(),
    @Column(name = "closed_at")
    val closedAt: LocalDateTime? = null,
    @Column(name = "closed_by", columnDefinition = "uuid")
    val closedBy: String? = null,
    @Column(name = "closing_entry_id", columnDefinition = "uuid")
    val closingEntryId: String? = null,
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null,
)
