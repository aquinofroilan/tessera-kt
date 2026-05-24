package com.froilan.synectix.model

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
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

enum class AccountType {
    ASSET,
    LIABILITY,
    EQUITY,
    REVENUE,
    EXPENSE,
    ;

    fun signedBalance(
        debits: BigDecimal,
        credits: BigDecimal,
    ): BigDecimal =
        when (this) {
            ASSET, EXPENSE -> debits.subtract(credits)
            LIABILITY, EQUITY, REVENUE -> credits.subtract(debits)
        }
}

@Entity
@Table(name = "accounts")
@EntityListeners(AuditingEntityListener::class)
data class Account(
    @Id
    @Column(columnDefinition = "uuid")
    val id: String = UUID.randomUUID().toString(),
    val code: String,
    val name: String,
    val description: String? = null,
    @Enumerated(EnumType.STRING)
    val type: AccountType,
    @Column(name = "parent_id", columnDefinition = "uuid")
    val parentId: String? = null,
    @Column(name = "organization_id", columnDefinition = "uuid")
    val organizationId: String,
    @Column(name = "is_active")
    val isActive: Boolean = true,
    @Column(name = "is_system_account")
    val isSystemAccount: Boolean = false,
    @CreatedDate
    @Column(name = "created_at")
    var createdAt: LocalDateTime? = null,
    @LastModifiedDate
    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null,
)
