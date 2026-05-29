package com.loom.synectix.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

enum class PayPeriod {
    ANNUAL,
    MONTHLY,
    HOURLY,
}

@Entity
@Table(name = "employee_compensation")
@EntityListeners(AuditingEntityListener::class)
data class EmployeeCompensation(
    @Id
    @Column(columnDefinition = "uuid")
    val id: String = UUID.randomUUID().toString(),
    @Column(name = "employee_id", columnDefinition = "uuid")
    val employeeId: String,
    @Column(name = "position_id", columnDefinition = "uuid")
    val positionId: String? = null,
    @Column(name = "pay_rate")
    val payRate: BigDecimal,
    val currency: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "pay_period")
    val payPeriod: PayPeriod,
    @Column(name = "effective_date")
    val effectiveDate: LocalDate,
    @Column(name = "organization_id", columnDefinition = "uuid")
    val organizationId: String,
    @Column(name = "created_by", columnDefinition = "uuid")
    val createdBy: String,
    @CreatedDate
    @Column(name = "created_at")
    val createdAt: LocalDateTime? = null,
)
