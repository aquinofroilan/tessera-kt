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

enum class JobApplicationStatus {
    APPLIED,
    SCREENING,
    INTERVIEW,
    OFFERED,
    HIRED,
    REJECTED,
    WITHDRAWN,
}

@Entity
@Table(name = "hr_recruitment_applications")
@EntityListeners(AuditingEntityListener::class)
data class JobApplication(
    @Id
    @Column(columnDefinition = "uuid")
    val id: UUID = UUID.randomUUID(),
    @Column(name = "organization_id", columnDefinition = "uuid")
    val organizationId: UUID,
    @Column(name = "job_posting_id", columnDefinition = "uuid")
    val jobPostingId: UUID,
    @Column(name = "candidate_full_name")
    val candidateFullName: String,
    val email: String? = null,
    val phone: String? = null,
    @Column(name = "resume_url")
    val resumeUrl: String? = null,
    val source: String? = null,
    @Enumerated(EnumType.STRING)
    val status: JobApplicationStatus = JobApplicationStatus.APPLIED,
    @Column(name = "applied_at")
    val appliedAt: LocalDateTime = LocalDateTime.now(),
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
