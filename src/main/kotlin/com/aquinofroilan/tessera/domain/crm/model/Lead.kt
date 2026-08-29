package com.aquinofroilan.tessera.domain.crm.model

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

enum class LeadStatus {
    NEW,
    QUALIFIED,
    CONVERTED,
    DISQUALIFIED,
}

@Entity
@Table(name = "crm_leads")
@EntityListeners(AuditingEntityListener::class)
data class Lead(
    @Id
    @Column(columnDefinition = "uuid")
    val id: UUID = UUID.ofEpochMillis(System.currentTimeMillis()),
    @Column(name = "organization_id", columnDefinition = "uuid")
    val organizationId: UUID,
    @Column(name = "full_name")
    val fullName: String,
    val company: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val source: String? = null,
    @Enumerated(EnumType.STRING)
    val status: LeadStatus = LeadStatus.NEW,
    @Column(name = "owner_user_id", columnDefinition = "uuid")
    val ownerUserId: UUID? = null,
    val notes: String? = null,
    @Column(name = "converted_to_opportunity_id", columnDefinition = "uuid")
    val convertedToOpportunityId: UUID? = null,
    @Column(name = "converted_at")
    val convertedAt: LocalDateTime? = null,
    @Column(name = "created_by", columnDefinition = "uuid")
    val createdBy: UUID,
    @CreatedDate
    @Column(name = "created_at")
    val createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    val updatedAt: LocalDateTime? = null,
)
