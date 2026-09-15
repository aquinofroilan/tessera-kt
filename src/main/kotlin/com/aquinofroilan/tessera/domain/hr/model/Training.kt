package com.aquinofroilan.tessera.domain.hr.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "training_courses")
@EntityListeners(AuditingEntityListener::class)
class TrainingCourse(
    @Id
    @Column(columnDefinition = "uuid")
    var id: java.util.UUID = java.util.UUID.ofEpochMillis(System.currentTimeMillis()),
    @Column(name = "organization_id", columnDefinition = "uuid")
    var organizationId: java.util.UUID,
    var name: String,
    var description: String? = null,
    var provider: String? = null,
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null,
)

@Entity
@Table(name = "training_records")
@EntityListeners(AuditingEntityListener::class)
class TrainingRecord(
    @Id
    @Column(columnDefinition = "uuid")
    var id: java.util.UUID = java.util.UUID.ofEpochMillis(System.currentTimeMillis()),
    @Column(name = "organization_id", columnDefinition = "uuid")
    var organizationId: java.util.UUID,
    @Column(name = "employee_id", columnDefinition = "uuid")
    var employeeId: java.util.UUID,
    @Column(name = "course_id", columnDefinition = "uuid")
    var courseId: java.util.UUID,
    @Column(name = "completion_date")
    var completionDate: LocalDate,
    @Column(name = "attachment_id", columnDefinition = "uuid")
    var attachmentId: java.util.UUID? = null,
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null,
)

@Entity
@Table(name = "certifications")
@EntityListeners(AuditingEntityListener::class)
class Certification(
    @Id
    @Column(columnDefinition = "uuid")
    var id: java.util.UUID = java.util.UUID.ofEpochMillis(System.currentTimeMillis()),
    @Column(name = "organization_id", columnDefinition = "uuid")
    var organizationId: java.util.UUID,
    @Column(name = "employee_id", columnDefinition = "uuid")
    var employeeId: java.util.UUID,
    var name: String,
    @Column(name = "issuing_body")
    var issuingBody: String? = null,
    @Column(name = "issue_date")
    var issueDate: LocalDate,
    @Column(name = "expiry_date")
    var expiryDate: LocalDate? = null,
    @Column(name = "credential_id")
    var credentialId: String? = null,
    @Column(name = "attachment_id", columnDefinition = "uuid")
    var attachmentId: java.util.UUID? = null,
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null,
)
