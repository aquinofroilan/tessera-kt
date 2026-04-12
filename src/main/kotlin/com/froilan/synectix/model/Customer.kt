package com.froilan.synectix.model

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime
import java.util.UUID

@Document(collection = "customers")
@CompoundIndex(
    name = "unique_name_per_org",
    def = "{'organizationId': 1, 'name': 1}",
    unique = true,
)
data class Customer(
    @Id
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val contactName: String? = null,
    val contactEmail: String? = null,
    val contactPhone: String? = null,
    val paymentTermDays: Int = 30,
    val defaultRevenueAccountId: String? = null,
    @Indexed
    val organizationId: String,
    val isActive: Boolean = true,
    @CreatedDate
    var createdAt: LocalDateTime? = null,
    @LastModifiedDate
    var updatedAt: LocalDateTime? = null,
)
