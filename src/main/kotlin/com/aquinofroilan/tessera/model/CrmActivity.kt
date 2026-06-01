package com.aquinofroilan.tessera.model

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

enum class CrmActivityType {
    CALL,
    EMAIL,
    MEETING,
    NOTE,
    TASK,
}

@Entity
@Table(name = "crm_activities")
@EntityListeners(AuditingEntityListener::class)
data class CrmActivity(
    @Id
    @Column(columnDefinition = "uuid")
    val id: String = UUID.randomUUID().toString(),
    @Column(name = "organization_id", columnDefinition = "uuid")
    val organizationId: String,
    @Enumerated(EnumType.STRING)
    val type: CrmActivityType,
    val subject: String,
    val body: String? = null,
    @Column(name = "related_lead_id", columnDefinition = "uuid")
    val relatedLeadId: String? = null,
    @Column(name = "related_opportunity_id", columnDefinition = "uuid")
    val relatedOpportunityId: String? = null,
    @Column(name = "related_contact_id", columnDefinition = "uuid")
    val relatedContactId: String? = null,
    @Column(name = "related_customer_id", columnDefinition = "uuid")
    val relatedCustomerId: String? = null,
    @Column(name = "owner_user_id", columnDefinition = "uuid")
    val ownerUserId: String? = null,
    @Column(name = "occurred_at")
    val occurredAt: LocalDateTime = LocalDateTime.now(),
    @Column(name = "due_at")
    val dueAt: LocalDateTime? = null,
    val completed: Boolean = false,
    @Column(name = "completed_at")
    val completedAt: LocalDateTime? = null,
    @Column(name = "completed_by", columnDefinition = "uuid")
    val completedBy: String? = null,
    @Column(name = "created_by", columnDefinition = "uuid")
    val createdBy: String,
    @CreatedDate
    @Column(name = "created_at")
    val createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    val updatedAt: LocalDateTime? = null,
)
