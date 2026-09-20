package com.aquinofroilan.tessera.domain.reporting.service

import com.aquinofroilan.tessera.config.export.CsvHttpMessageConverter
import com.aquinofroilan.tessera.config.export.ExcelHttpMessageConverter
import com.aquinofroilan.tessera.config.export.PdfHttpMessageConverter
import com.aquinofroilan.tessera.domain.inventory.service.InventoryReorderRuleService
import com.aquinofroilan.tessera.domain.inventory.service.InventoryReportsService
import com.aquinofroilan.tessera.domain.inventory.service.InventoryValuationService
import com.aquinofroilan.tessera.domain.reporting.repository.ScheduledReportRepository
import jakarta.mail.internet.MimeMessage
import org.jobrunr.jobs.annotations.Job
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpOutputMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

@Service
class ScheduledReportDeliveryJob(
    private val scheduledReportRepository: ScheduledReportRepository,
    private val inventoryValuationService: InventoryValuationService,
    private val inventoryReportsService: InventoryReportsService,
    private val reorderRuleService: InventoryReorderRuleService,
    private val csvHttpMessageConverter: CsvHttpMessageConverter,
    private val excelHttpMessageConverter: ExcelHttpMessageConverter,
    private val pdfHttpMessageConverter: PdfHttpMessageConverter,
    private val mailSender: JavaMailSender,
) {
    @Job(name = "Deliver Scheduled Report")
    fun deliverReport(reportId: UUID) {
        val report = scheduledReportRepository.findById(reportId).orElse(null) ?: return
        if (!report.isActive) return

        val data = fetchReportData(report.reportType, report.organizationId, report.queryParams ?: emptyMap())

        val byteOutput = ByteArrayOutputStream()
        val mockHttpOutput =
            object : HttpOutputMessage {
                override fun getHeaders() = HttpHeaders()

                override fun getBody(): OutputStream = byteOutput
            }

        when (report.format.lowercase()) {
            "csv" -> csvHttpMessageConverter.write(data, null, mockHttpOutput)
            "xlsx", "excel" -> excelHttpMessageConverter.write(data, null, mockHttpOutput)
            "pdf" -> pdfHttpMessageConverter.write(data, null, mockHttpOutput)
            else -> throw IllegalArgumentException("Unsupported format: ${report.format}")
        }

        val fileBytes = byteOutput.toByteArray()
        val filename = "${report.name.replace(
            " ",
            "_",
        )}_${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))}.${report.format.lowercase()}"

        val message: MimeMessage = mailSender.createMimeMessage()
        val helper = MimeMessageHelper(message, true)

        helper.setSubject("Scheduled Report: ${report.name}")
        helper.setText("Please find attached the scheduled report for ${report.name}.")
        helper.setFrom("reports@tessera.local")

        report.recipientEmails.forEach { helper.addTo(it) }

        val contentType =
            when (report.format.lowercase()) {
                "csv" -> "text/csv"
                "xlsx", "excel" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                "pdf" -> "application/pdf"
                else -> "application/octet-stream"
            }

        helper.addAttachment(
            filename,
            org.springframework.core.io
                .ByteArrayResource(fileBytes),
            contentType,
        )

        mailSender.send(message)
    }

    private fun fetchReportData(
        reportType: String,
        orgId: UUID,
        params: Map<String, String>,
    ): Any =
        when (reportType) {
            "INVENTORY_VALUATION" -> inventoryValuationService.valuation(orgId)
            "STOCK_ON_HAND" -> {
                val productId = params["productId"]?.let { UUID.fromString(it) }
                val warehouseId = params["warehouseId"]?.let { UUID.fromString(it) }
                val asOfDate = params["asOfDate"]?.let { LocalDateTime.parse(it) }
                inventoryReportsService.stockOnHand(orgId, productId, warehouseId, asOfDate)
            }
            "LOW_STOCK" -> reorderRuleService.lowStockReport(orgId)
            "EXPIRY" -> {
                val days = params["daysUntilExpiry"]?.toIntOrNull() ?: 30
                inventoryReportsService.expiryReport(orgId, days)
            }
            else -> emptyList<Any>()
        }
}
