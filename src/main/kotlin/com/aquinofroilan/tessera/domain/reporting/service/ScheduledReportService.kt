package com.aquinofroilan.tessera.domain.reporting.service

import com.aquinofroilan.tessera.domain.reporting.dto.CreateScheduledReportRequest
import com.aquinofroilan.tessera.domain.reporting.dto.ScheduledReportResponse
import com.aquinofroilan.tessera.domain.reporting.dto.UpdateScheduledReportRequest
import com.aquinofroilan.tessera.domain.reporting.model.ScheduledReport
import com.aquinofroilan.tessera.domain.reporting.repository.ScheduledReportRepository
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import org.jobrunr.scheduling.JobScheduler
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ScheduledReportService(
    private val scheduledReportRepository: ScheduledReportRepository,
    private val jobScheduler: JobScheduler,
) {
    @Transactional(readOnly = true)
    fun listReports(
        organizationId: UUID,
        pageable: Pageable,
    ): Page<ScheduledReportResponse> =
        scheduledReportRepository.findByOrganizationId(organizationId, pageable).map {
            ScheduledReportResponse(it)
        }

    @Transactional(readOnly = true)
    fun getReport(
        organizationId: UUID,
        reportId: UUID,
    ): ScheduledReportResponse {
        val report = findReportOrThrow(organizationId, reportId)
        return ScheduledReportResponse(report)
    }

    @Transactional
    fun createReport(
        organizationId: UUID,
        request: CreateScheduledReportRequest,
    ): ScheduledReportResponse {
        val report =
            ScheduledReport(
                organizationId = organizationId,
                name = request.name,
                reportType = request.reportType,
                format = request.format,
                cronExpression = request.cronExpression,
                recipientEmails = request.recipientEmails,
                queryParams = request.queryParams,
            )

        val saved = scheduledReportRepository.save(report)
        scheduleJob(saved)
        return ScheduledReportResponse(saved)
    }

    @Transactional
    fun updateReport(
        organizationId: UUID,
        reportId: UUID,
        request: UpdateScheduledReportRequest,
    ): ScheduledReportResponse {
        val report = findReportOrThrow(organizationId, reportId)

        request.name?.let { report.name = it }
        request.reportType?.let { report.reportType = it }
        request.format?.let { report.format = it }
        request.cronExpression?.let { report.cronExpression = it }
        request.recipientEmails?.let { report.recipientEmails = it }
        if (request.queryParams != null) {
            report.queryParams = request.queryParams
        }
        request.isActive?.let { report.isActive = it }

        val updated = scheduledReportRepository.save(report)

        if (updated.isActive) {
            scheduleJob(updated)
        } else {
            jobScheduler.delete(updated.id)
        }

        return ScheduledReportResponse(updated)
    }

    @Transactional
    fun deleteReport(
        organizationId: UUID,
        reportId: UUID,
    ) {
        val report = findReportOrThrow(organizationId, reportId)
        jobScheduler.delete(report.id)
        scheduledReportRepository.delete(report)
    }

    private fun scheduleJob(report: ScheduledReport) {
        jobScheduler.scheduleRecurrently<ReportDeliveryJob>(report.id.toString(), report.cronExpression) {
            it.deliverReport(report.id)
        }
    }

    private fun findReportOrThrow(
        organizationId: UUID,
        reportId: UUID,
    ): ScheduledReport {
        val report =
            scheduledReportRepository
                .findById(reportId)
                .orElseThrow { ResourceNotFoundException("Scheduled report not found") }
        if (report.organizationId != organizationId) {
            throw ResourceNotFoundException("Scheduled report not found")
        }
        return report
    }
}
