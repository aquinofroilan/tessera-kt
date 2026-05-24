package com.froilan.synectix.model

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

@Document(collection = "organizations")
data class Organizations(
    @Id
    val uuid: String = UUID.randomUUID().toString(),
    @Indexed(unique = true)
    val orgSlug: String,
    @Indexed(unique = true)
    val name: String,
    val description: String? = null,
    val legalName: String,
    val tradeName: String,
    val baseCurrency: String = "USD",
    val fiscalYearStart: LocalDateTime,
    val timezone: String,
    val status: String = "ACTIVE",
    val inventoryCostingMethod: InventoryCostingMethod = InventoryCostingMethod.WEIGHTED_AVERAGE,
    val createdAt: LocalDateTime = LocalDateTime.now(ZoneOffset.UTC),
    val isActive: Boolean = true,
)

enum class InventoryCostingMethod {
    FIFO,
    WEIGHTED_AVERAGE,
}
