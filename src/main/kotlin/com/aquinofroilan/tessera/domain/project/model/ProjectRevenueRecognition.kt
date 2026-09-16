package com.aquinofroilan.tessera.domain.project.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "project_revenue_recognitions")
@EntityListeners(AuditingEntityListener::class)
class ProjectRevenueRecognition(
    @Id
    @Column(columnDefinition = "uuid")
    var id: UUID = UUID.randomUUID(),
    @Column(name = "organization_id", columnDefinition = "uuid")
    var organizationId: UUID,
    @Column(name = "project_id", columnDefinition = "uuid")
    var projectId: UUID,
    @Column(name = "recognized_date")
    var recognizedDate: LocalDate,
    @Column(name = "recognized_revenue")
    var recognizedRevenue: BigDecimal,
    @Column(name = "recognized_cost")
    var recognizedCost: BigDecimal,
    @Column(name = "percent_complete")
    var percentComplete: BigDecimal? = null,
    @Column(name = "journal_entry_id", columnDefinition = "uuid")
    var journalEntryId: UUID? = null,
    @Column(name = "created_by", columnDefinition = "uuid")
    var createdBy: UUID,
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null,
)
