package com.aquinofroilan.tessera.domain.hr.model

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
import java.time.LocalDateTime
import java.util.UUID

enum class JobPostingStatus {
    DRAFT,
    OPEN,
    CLOSED,
    CANCELLED,
}

@Entity
@Table(name = "hr_recruitment_job_postings")
@EntityListeners(AuditingEntityListener::class)
data class JobPosting(
    @Id
    @Column(columnDefinition = "uuid")
    val id: UUID = UUID.randomUUID(),
    @Column(name = "organization_id", columnDefinition = "uuid")
    val organizationId: UUID,
    val title: String,
    val description: String? = null,
    @Column(name = "department_id", columnDefinition = "uuid")
    val departmentId: UUID? = null,
    @Column(name = "position_id", columnDefinition = "uuid")
    val positionId: UUID? = null,
    @Enumerated(EnumType.STRING)
    val status: JobPostingStatus = JobPostingStatus.DRAFT,
    @Column(name = "posted_at")
    val postedAt: LocalDateTime? = null,
    @Column(name = "closed_at")
    val closedAt: LocalDateTime? = null,
    @Column(name = "owner_user_id", columnDefinition = "uuid")
    val ownerUserId: UUID? = null,
    val notes: String? = null,
    @Column(name = "created_by", columnDefinition = "uuid")
    val createdBy: UUID,
    @CreatedDate
    @Column(name = "created_at")
    val createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    val updatedAt: LocalDateTime? = null,
)
