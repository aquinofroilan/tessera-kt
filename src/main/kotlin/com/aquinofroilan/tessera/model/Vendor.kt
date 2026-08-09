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

@Entity
@Table(name = "vendors")
@EntityListeners(AuditingEntityListener::class)
class Vendor(
    @Id
    @Column(columnDefinition = "uuid")
    var id: java.util.UUID = java.util.UUID.ofEpochMillis(System.currentTimeMillis()),
    var name: String,
    @Column(name = "contact_name")
    var contactName: String? = null,
    @Column(name = "contact_email")
    var contactEmail: String? = null,
    @Column(name = "contact_phone")
    var contactPhone: String? = null,
    @Column(name = "payment_term_days")
    var paymentTermDays: Int = 30,
    @Column(name = "default_expense_account_id", columnDefinition = "uuid")
    var defaultExpenseAccountId: java.util.UUID? = null,
    @Column(name = "organization_id", columnDefinition = "uuid")
    var organizationId: java.util.UUID,
    @Column(name = "is_active")
    var isActive: Boolean = true,
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null,
)
