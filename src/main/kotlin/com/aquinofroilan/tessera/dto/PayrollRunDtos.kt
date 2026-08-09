package com.aquinofroilan.tessera.dto

import com.aquinofroilan.tessera.model.PayrollRun
import com.aquinofroilan.tessera.model.PayrollRunStatus
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.LocalDate

data class CreatePayrollRunRequest(
    @field:NotNull(message = "Period start is required")
    val periodStart: LocalDate?,
    @field:NotNull(message = "Period end is required")
    val periodEnd: LocalDate?,
    @field:NotNull(message = "Pay date is required")
    val payDate: LocalDate?,
)

data class PayrollRunLineResponse(
    val id: java.util.UUID,
    val lineNumber: Int,
    val employeeId: java.util.UUID,
    val employeeNumber: String,
    val employeeName: String,
    val compensationId: java.util.UUID,
    val grossAmount: BigDecimal,
)

data class PayrollRunResponse(
    val id: java.util.UUID,
    val runNumber: String,
    val periodStart: String,
    val periodEnd: String,
    val payDate: String,
    val organizationId: java.util.UUID,
    val status: PayrollRunStatus,
    val lines: List<PayrollRunLineResponse>,
    val totalGross: BigDecimal,
    val currency: String,
    val createdBy: java.util.UUID,
    val accrualJournalEntryId: java.util.UUID?,
    val paymentJournalEntryId: java.util.UUID?,
    val approvedAt: String?,
    val paidAt: String?,
    val cancelledAt: String?,
    val createdAt: String?,
    val updatedAt: String?,
) {
    companion object {
        fun from(run: PayrollRun) =
            PayrollRunResponse(
                id = run.id,
                runNumber = run.runNumber,
                periodStart = run.periodStart.toString(),
                periodEnd = run.periodEnd.toString(),
                payDate = run.payDate.toString(),
                organizationId = run.organizationId,
                status = run.status,
                lines =
                    run.lines.map { line ->
                        PayrollRunLineResponse(
                            id = line.id,
                            lineNumber = line.lineNumber,
                            employeeId = line.employeeId,
                            employeeNumber = line.employeeNumber,
                            employeeName = line.employeeName,
                            compensationId = line.compensationId,
                            grossAmount = line.grossAmount,
                        )
                    },
                totalGross = run.totalGross,
                currency = run.currency,
                createdBy = run.createdBy,
                accrualJournalEntryId = run.accrualJournalEntryId,
                paymentJournalEntryId = run.paymentJournalEntryId,
                approvedAt = run.approvedAt?.toString(),
                paidAt = run.paidAt?.toString(),
                cancelledAt = run.cancelledAt?.toString(),
                createdAt = run.createdAt?.toString(),
                updatedAt = run.updatedAt?.toString(),
            )
    }
}
