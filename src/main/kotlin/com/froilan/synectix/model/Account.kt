package com.froilan.synectix.model

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime
import java.util.UUID

enum class AccountType {
    ASSET,
    LIABILITY,
    EQUITY,
    REVENUE,
    EXPENSE,
}

@Document(collection = "accounts")
@CompoundIndex(name = "unique_code_per_org", def = "{'organizationId': 1, 'code': 1}", unique = true)
data class Account(
    @Id
    val id: String = UUID.randomUUID().toString(),
    val code: String,
    val name: String,
    val description: String? = null,
    val type: AccountType,
    val parentId: String? = null,
    @Indexed
    val organizationId: String,
    val isActive: Boolean = true,
    val isSystemAccount: Boolean = false,
    @CreatedDate
    var createdAt: LocalDateTime? = null,
    @LastModifiedDate
    var updatedAt: LocalDateTime? = null,
)
