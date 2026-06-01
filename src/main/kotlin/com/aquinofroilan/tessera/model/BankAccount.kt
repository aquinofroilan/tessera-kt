package com.aquinofroilan.tessera.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "finance_bank_accounts")
@EntityListeners(AuditingEntityListener::class)
data class BankAccount(
    @Id
    @Column(columnDefinition = "uuid")
    val id: String = UUID.randomUUID().toString(),
    @Column(name = "organization_id", columnDefinition = "uuid")
    val organizationId: String,
    val code: String,
    val name: String,
    @Column(name = "bank_name")
    val bankName: String? = null,
    @Column(name = "account_number_last4")
    val accountNumberLast4: String? = null,
    @Column(columnDefinition = "char(3)")
    val currency: String,
    @Column(name = "gl_account_id", columnDefinition = "uuid")
    val glAccountId: String,
    @Column(name = "opening_balance")
    val openingBalance: BigDecimal = BigDecimal.ZERO,
    @Column(name = "current_balance")
    val currentBalance: BigDecimal = BigDecimal.ZERO,
    @Column(name = "is_active")
    val isActive: Boolean = true,
    val notes: String? = null,
    @Column(name = "created_by", columnDefinition = "uuid")
    val createdBy: String,
    @CreatedDate
    @Column(name = "created_at")
    val createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    val updatedAt: LocalDateTime? = null,
)
