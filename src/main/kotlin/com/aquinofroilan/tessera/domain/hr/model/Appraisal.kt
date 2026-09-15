package com.aquinofroilan.tessera.domain.hr.model

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
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

enum class AppraisalCycleStatus {
    DRAFT,
    ACTIVE,
    CLOSED,
}

enum class AppraisalStatus {
    DRAFT,
    SELF_REVIEW,
    MANAGER_REVIEW,
    COMPLETED,
}

@Entity
@Table(name = "appraisal_cycles")
@EntityListeners(AuditingEntityListener::class)
class AppraisalCycle(
    @Id
    @Column(columnDefinition = "uuid")
    var id: java.util.UUID = java.util.UUID.ofEpochMillis(System.currentTimeMillis()),
    @Column(name = "organization_id", columnDefinition = "uuid")
    var organizationId: java.util.UUID,
    var name: String,
    @Column(name = "start_date")
    var startDate: LocalDate,
    @Column(name = "end_date")
    var endDate: LocalDate,
    @Enumerated(EnumType.STRING)
    var status: AppraisalCycleStatus = AppraisalCycleStatus.DRAFT,
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null,
)

@Entity
@Table(name = "appraisal_goals")
class AppraisalGoal(
    @Id
    @Column(columnDefinition = "uuid")
    var id: java.util.UUID = java.util.UUID.ofEpochMillis(System.currentTimeMillis()),
    @Column(name = "appraisal_id", columnDefinition = "uuid")
    var appraisalId: java.util.UUID,
    var title: String,
    var description: String? = null,
    var weight: Int = 0,
    @Column(name = "self_rating")
    var selfRating: Int? = null,
    @Column(name = "manager_rating")
    var managerRating: Int? = null,
    @Column(name = "self_comments")
    var selfComments: String? = null,
    @Column(name = "manager_comments")
    var managerComments: String? = null,
)

@Entity
@Table(name = "appraisals")
@EntityListeners(AuditingEntityListener::class)
class Appraisal(
    @Id
    @Column(columnDefinition = "uuid")
    var id: java.util.UUID = java.util.UUID.ofEpochMillis(System.currentTimeMillis()),
    @Column(name = "organization_id", columnDefinition = "uuid")
    var organizationId: java.util.UUID,
    @Column(name = "cycle_id", columnDefinition = "uuid")
    var cycleId: java.util.UUID,
    @Column(name = "employee_id", columnDefinition = "uuid")
    var employeeId: java.util.UUID,
    @Column(name = "manager_id", columnDefinition = "uuid")
    var managerId: java.util.UUID? = null,
    @Enumerated(EnumType.STRING)
    var status: AppraisalStatus = AppraisalStatus.DRAFT,
    @Column(name = "overall_rating")
    var overallRating: Double? = null,
    @Column(name = "manager_summary")
    var managerSummary: String? = null,
    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "appraisal_id")
    var goals: MutableList<AppraisalGoal> = mutableListOf(),
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null,
)
