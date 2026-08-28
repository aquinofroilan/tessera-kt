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

enum class InterviewMode {
    PHONE,
    VIDEO,
    ONSITE,
}

enum class InterviewStatus {
    SCHEDULED,
    COMPLETED,
    CANCELLED,
}

enum class InterviewOutcome {
    PASS,
    FAIL,
    UNDECIDED,
}

@Entity
@Table(name = "hr_recruitment_interviews")
@EntityListeners(AuditingEntityListener::class)
data class Interview(
    @Id
    @Column(columnDefinition = "uuid")
    val id: UUID = UUID.randomUUID(),
    @Column(name = "organization_id", columnDefinition = "uuid")
    val organizationId: UUID,
    @Column(name = "application_id", columnDefinition = "uuid")
    val applicationId: UUID,
    @Column(name = "scheduled_at")
    val scheduledAt: LocalDateTime,
    @Column(name = "interviewer_user_id", columnDefinition = "uuid")
    val interviewerUserId: UUID? = null,
    @Enumerated(EnumType.STRING)
    val mode: InterviewMode = InterviewMode.VIDEO,
    @Enumerated(EnumType.STRING)
    val status: InterviewStatus = InterviewStatus.SCHEDULED,
    @Enumerated(EnumType.STRING)
    val outcome: InterviewOutcome? = null,
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
