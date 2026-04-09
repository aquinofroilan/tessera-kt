package com.froilan.synectix.model

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.Indexed
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

enum class FiscalYearStatus {
    ACTIVE,
    CLOSED,
}

enum class FiscalPeriodStatus {
    OPEN,
    CLOSED,
    REOPENED,
}

data class FiscalPeriod(
    val id: String = UUID.randomUUID().toString(),
    val periodNumber: Int,
    val name: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val status: FiscalPeriodStatus = FiscalPeriodStatus.OPEN,
    val closedAt: LocalDateTime? = null,
    val closedBy: String? = null,
    val reopenedAt: LocalDateTime? = null,
    val reopenedBy: String? = null,
)

@Document(collection = "fiscal_years")
@CompoundIndex(
    name = "unique_name_per_org",
    def = "{'organizationId': 1, 'name': 1}",
    unique = true,
)
data class FiscalYear(
    @Id
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val status: FiscalYearStatus = FiscalYearStatus.ACTIVE,
    @Indexed
    val organizationId: String,
    val periods: List<FiscalPeriod> = emptyList(),
    val closedAt: LocalDateTime? = null,
    val closedBy: String? = null,
    val closingEntryId: String? = null,
    @CreatedDate
    var createdAt: LocalDateTime? = null,
    @LastModifiedDate
    var updatedAt: LocalDateTime? = null,
)
