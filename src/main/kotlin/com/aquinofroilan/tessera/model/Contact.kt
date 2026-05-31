package com.aquinofroilan.tessera.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime
import java.util.UUID
import com.github.f4b6a3.uuid.UuidCreator

@Entity
@Table(name = "crm_contacts")
@EntityListeners(AuditingEntityListener::class)
data class Contact(
    @Id
    @Column(columnDefinition = "uuid")
    val id: UUID = UuidCreator.getTimeOrderedEpoch(),
    @Column(name = "organization_id", columnDefinition = "uuid")
    val organizationId: UUID,
    @Column(name = "customer_id", columnDefinition = "uuid")
    val customerId: UUID? = null,
    @Column(name = "first_name")
    val firstName: String,
    @Column(name = "last_name")
    val lastName: String,
    val email: String? = null,
    val phone: String? = null,
    @Column(name = "job_title")
    val jobTitle: String? = null,
    val department: String? = null,
    val notes: String? = null,
    @Column(name = "is_active")
    val isActive: Boolean = true,
    @Column(name = "created_by", columnDefinition = "uuid")
    val createdBy: UUID,
    @CreatedDate
    @Column(name = "created_at")
    val createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    val updatedAt: LocalDateTime? = null,
)
