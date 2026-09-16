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
import java.time.LocalDate
import java.time.LocalDateTime

enum class DocumentCategory {
    CONTRACT,
    ID_DOCUMENT,
    POLICY_ACKNOWLEDGMENT,
    CERTIFICATION,
    OTHER,
}

@Entity
@Table(name = "employee_documents")
@EntityListeners(AuditingEntityListener::class)
class EmployeeDocument(
    @Id
    @Column(columnDefinition = "uuid")
    var id: java.util.UUID = java.util.UUID.ofEpochMillis(System.currentTimeMillis()),
    @Column(name = "organization_id", columnDefinition = "uuid")
    var organizationId: java.util.UUID,
    @Column(name = "employee_id", columnDefinition = "uuid")
    var employeeId: java.util.UUID,
    @Column(name = "attachment_id", columnDefinition = "uuid")
    var attachmentId: java.util.UUID,
    @Enumerated(EnumType.STRING)
    var category: DocumentCategory,
    @Column(name = "expiry_date")
    var expiryDate: LocalDate? = null,
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null,
)
