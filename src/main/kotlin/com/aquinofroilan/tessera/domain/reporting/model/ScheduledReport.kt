package com.aquinofroilan.tessera.domain.reporting.model

import com.aquinofroilan.tessera.domain.organization.model.Organizations
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "scheduled_reports")
class ScheduledReport(
    @Id
    val id: UUID = UUID.randomUUID(),
    @Column(name = "organization_id", insertable = false, updatable = false)
    val organizationId: UUID,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    val organization: Organizations? = null,
    @Column(nullable = false)
    var name: String,
    @Column(nullable = false)
    var reportType: String,
    @Column(nullable = false)
    var format: String,
    @Column(nullable = false)
    var cronExpression: String,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    var recipientEmails: List<String>,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    var queryParams: Map<String, String>?,
    @Column(nullable = false)
    var isActive: Boolean = true,
) {
    @CreatedDate
    @Column(nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now()

    @LastModifiedDate
    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now()
}
