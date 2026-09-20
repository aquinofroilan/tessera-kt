package com.aquinofroilan.tessera.domain.reporting.dto

import com.aquinofroilan.tessera.domain.reporting.model.ScheduledReport
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import java.time.LocalDateTime
import java.util.UUID

data class ScheduledReportResponse(
    val id: UUID,
    val name: String,
    val reportType: String,
    val format: String,
    val cronExpression: String,
    val recipientEmails: List<String>,
    val queryParams: Map<String, String>?,
    val isActive: Boolean,
    val createdAt: LocalDateTime,
) {
    constructor(entity: ScheduledReport) : this(
        id = entity.id,
        name = entity.name,
        reportType = entity.reportType,
        format = entity.format,
        cronExpression = entity.cronExpression,
        recipientEmails = entity.recipientEmails,
        queryParams = entity.queryParams,
        isActive = entity.isActive,
        createdAt = entity.createdAt,
    )
}

data class CreateScheduledReportRequest(
    @field:NotBlank val name: String,
    @field:NotBlank val reportType: String,
    @field:NotBlank val format: String,
    @field:NotBlank val cronExpression: String,
    @field:NotEmpty val recipientEmails: List<String>,
    val queryParams: Map<String, String>? = null,
)

data class UpdateScheduledReportRequest(
    val name: String? = null,
    val reportType: String? = null,
    val format: String? = null,
    val cronExpression: String? = null,
    val recipientEmails: List<String>? = null,
    val queryParams: Map<String, String>? = null,
    val isActive: Boolean? = null,
)
