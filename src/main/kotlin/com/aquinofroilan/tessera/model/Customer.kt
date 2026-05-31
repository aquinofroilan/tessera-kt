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
@Table(name = "customers")
@EntityListeners(AuditingEntityListener::class)
data class Customer(
    @Id
    @Column(columnDefinition = "uuid")
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    @Column(name = "contact_name")
    val contactName: String? = null,
    @Column(name = "contact_email")
    val contactEmail: String? = null,
    @Column(name = "contact_phone")
    val contactPhone: String? = null,
    @Column(name = "payment_term_days")
    val paymentTermDays: Int = 30,
    @Column(name = "default_revenue_account_id", columnDefinition = "uuid")
    val defaultRevenueAccountId: String? = null,
    @Column(name = "organization_id", columnDefinition = "uuid")
    val organizationId: String,
    @Column(name = "is_active")
    val isActive: Boolean = true,
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null,
)
