package com.froilan.synectix.model

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime
import java.util.UUID

@Document(collection = "warehouses")
@CompoundIndexes(
    CompoundIndex(
        name = "unique_code_per_org",
        def = "{'organizationId': 1, 'code': 1}",
        unique = true,
    ),
    CompoundIndex(
        name = "warehouses_org_active",
        def = "{'organizationId': 1, 'isActive': 1}",
    ),
)
data class Warehouse(
    @Id
    val id: String = UUID.randomUUID().toString(),
    val code: String,
    val name: String,
    val description: String? = null,
    val addressLine: String? = null,
    val city: String? = null,
    val country: String? = null,
    val allowNegativeStock: Boolean = false,
    @Indexed
    val organizationId: String,
    val isActive: Boolean = true,
    @CreatedDate
    var createdAt: LocalDateTime? = null,
    @LastModifiedDate
    var updatedAt: LocalDateTime? = null,
)
