package com.aquinofroilan.tessera.model

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
class EmployeeCompensation(
    @Id
    @Column(columnDefinition = "uuid")
    var id: String = UUID.randomUUID().toString(),
    @Column(name = "employee_id", columnDefinition = "uuid")
    var employeeId: String,
    @Column(name = "position_id", columnDefinition = "uuid")
    var positionId: String? = null,
    @Column(name = "pay_rate")
    var payRate: BigDecimal,
    var currency: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "pay_period")
    var payPeriod: PayPeriod,
    @Column(name = "effective_date")
    var effectiveDate: LocalDate,
    @Column(name = "organization_id", columnDefinition = "uuid")
    var organizationId: String,
    @Column(name = "created_by", columnDefinition = "uuid")
    var createdBy: String,
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null,
)
