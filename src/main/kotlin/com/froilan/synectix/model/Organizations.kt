package com.froilan.synectix.model

import java.time.LocalDateTime
import java.util.UUID
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "organizations")
data class Organization(
    @Id
    val uuid: String = UUID.randomUUID().toString(),

    @Indexed(unique = true)
    val orgSlug: String,

    val name: String,

    val description: String? = null,

    val legalName: String,

    val tradeName: String,

    val baseCurrency: String,

    val fiscalYearStart: LocalDateTime,

    val timezone: String,

    val status: String = "ACTIVE",

    val createdAt: LocalDateTime = LocalDateTime.now(),

    val isActive: Boolean = true

)
