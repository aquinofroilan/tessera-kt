package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.CreatePayrollRunRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import com.aquinofroilan.tessera.model.Account
import com.aquinofroilan.tessera.model.EmploymentStatus
import com.aquinofroilan.tessera.model.JournalEntryLine
import com.aquinofroilan.tessera.model.PayPeriod
import com.aquinofroilan.tessera.model.PayrollRun
import com.aquinofroilan.tessera.model.PayrollRunLine
import com.aquinofroilan.tessera.model.PayrollRunStatus
import com.aquinofroilan.tessera.repository.AccountRepository
import com.aquinofroilan.tessera.repository.OrganizationRepository
import com.aquinofroilan.tessera.repository.PayrollRunRepository
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDateTime
import java.time.ZoneOffset

@Service
class PayrollRunService(
    private val payrollRunRepository: PayrollRunRepository,
    private val employeeService: EmployeeService,
    private val compensationService: EmployeeCompensationService,
    private val currencyService: CurrencyService,
    private val organizationRepository: OrganizationRepository,
    private val accountRepository: AccountRepository,
    private val journalEntryService: JournalEntryService,
) {
    @Transactional
    fun createPayrollRun(
        request: CreatePayrollRunRequest,
        organizationId: String,
        createdBy: String,
    ): PayrollRun {
        val periodStart = request.periodStart ?: throw BusinessRuleException("Period start is required")
        val periodEnd = request.periodEnd ?: throw BusinessRuleException("Period end is required")
        val payDate = request.payDate ?: throw BusinessRuleException("Pay date is required")
        if (periodEnd.isBefore(periodStart)) {
            throw BusinessRuleException("Period end must be on or after period start")
        }

        val organization =
            organizationRepository.findById(organizationId).orElseThrow {
                ResourceNotFoundException("Organization not found")
            }
        val baseCurrency = organization.baseCurrency
        val decimals = currencyService.getCurrency(baseCurrency).decimalPlaces

        val lines =
            employeeService
                .listEmployees(organizationId, EmploymentStatus.ACTIVE, null)
                .mapNotNull { employee ->
                    val comp =
                        compensationService.currentCompensationOrNull(employee.id, organizationId, periodEnd)
                            ?: return@mapNotNull null
                    if (comp.currency != baseCurrency) return@mapNotNull null
                    val gross = monthlyGross(comp.payRate, comp.payPeriod, decimals) ?: return@mapNotNull null
                    Triple(employee, comp.id, gross)
                }.mapIndexed { index, (employee, compId, gross) ->
                    PayrollRunLine(
                        lineNumber = index + 1,
                        employeeId = employee.id,
                        employeeNumber = employee.employeeNumber,
                        employeeName = "${employee.firstName} ${employee.lastName}",
                        compensationId = compId,
                        grossAmount = gross,
                    )
                }

        if (lines.isEmpty()) {
            throw BusinessRuleException("No payable employees with $baseCurrency compensation effective by $periodEnd")
        }

        val total = lines.fold(BigDecimal.ZERO) { sum, line -> sum.add(line.grossAmount) }

        return saveWithRetry(organizationId) { number ->
            PayrollRun(
                runNumber = number,
                periodStart = periodStart,
                periodEnd = periodEnd,
                payDate = payDate,
                organizationId = organizationId,
                lines = lines,
                totalGross = total,
                currency = baseCurrency,
                createdBy = createdBy,
            )
        }
    }

    fun getPayrollRun(
        id: String,
        organizationId: String,
    ): PayrollRun {
        val run =
            payrollRunRepository.findById(id).orElseThrow {
                ResourceNotFoundException("Payroll run not found")
            }
        if (run.organizationId != organizationId) {
            throw ResourceNotFoundException("Payroll run not found")
        }
        return run
    }

    fun listPayrollRuns(
        organizationId: String,
        status: PayrollRunStatus? = null,
    ): List<PayrollRun> =
        if (status != null) {
            payrollRunRepository.findByOrganizationIdAndStatus(organizationId, status)
        } else {
            payrollRunRepository.findByOrganizationId(organizationId)
        }

    @Transactional
    fun approvePayrollRun(
        id: String,
        organizationId: String,
        approvedBy: String,
    ): PayrollRun {
        val run = getPayrollRun(id, organizationId)
        if (run.status != PayrollRunStatus.DRAFT) {
            throw BusinessRuleException("Only draft payroll runs can be approved")
        }
        val salaryExpense = account(organizationId, SALARY_EXPENSE_CODE)
        val wagesPayable = account(organizationId, WAGES_PAYABLE_CODE)
        val entry =
            journalEntryService.createSystemEntry(
                date = run.periodEnd,
                description = "Payroll accrual ${run.runNumber}",
                organizationId = organizationId,
                lines =
                    listOf(
                        line(salaryExpense, debit = run.totalGross, description = "Payroll ${run.runNumber}"),
                        line(wagesPayable, credit = run.totalGross, description = "Payroll ${run.runNumber}"),
                    ),
                sourceReference = "PAYROLL-ACCRUAL-${run.id}",
                createdBy = approvedBy,
            )
        run.status = PayrollRunStatus.APPROVED
        run.accrualJournalEntryId = entry.id
        run.approvedAt = LocalDateTime.now(ZoneOffset.UTC)
        run.approvedBy = approvedBy
        return payrollRunRepository.save(run)
    }

    @Transactional
    fun payPayrollRun(
        id: String,
        organizationId: String,
        paidBy: String,
    ): PayrollRun {
        val run = getPayrollRun(id, organizationId)
        if (run.status != PayrollRunStatus.APPROVED) {
            throw BusinessRuleException("Only approved payroll runs can be paid")
        }
        val wagesPayable = account(organizationId, WAGES_PAYABLE_CODE)
        val cash = account(organizationId, CASH_CODE)
        val entry =
            journalEntryService.createSystemEntry(
                date = run.payDate,
                description = "Payroll payment ${run.runNumber}",
                organizationId = organizationId,
                lines =
                    listOf(
                        line(wagesPayable, debit = run.totalGross, description = "Payroll ${run.runNumber}"),
                        line(cash, credit = run.totalGross, description = "Payroll ${run.runNumber}"),
                    ),
                sourceReference = "PAYROLL-PAYMENT-${run.id}",
                createdBy = paidBy,
            )
        run.status = PayrollRunStatus.PAID
        run.paymentJournalEntryId = entry.id
        run.paidAt = LocalDateTime.now(ZoneOffset.UTC)
        return payrollRunRepository.save(run)
    }

    @Transactional
    fun cancelPayrollRun(
        id: String,
        organizationId: String,
    ): PayrollRun {
        val run = getPayrollRun(id, organizationId)
        if (run.status != PayrollRunStatus.DRAFT) {
            throw BusinessRuleException("Only draft payroll runs can be cancelled; approved runs have posted to the ledger")
        }
        run.status = PayrollRunStatus.CANCELLED
        run.cancelledAt = LocalDateTime.now(ZoneOffset.UTC)
        return payrollRunRepository.save(run)
    }

    private fun account(
        organizationId: String,
        code: String,
    ): Account {
        val account =
            accountRepository.findByOrganizationIdAndCode(organizationId, code).orElseThrow {
                IllegalStateException("Payroll posting account ($code) not found; configure it before approving payroll")
            }
        if (!account.isActive) {
            throw BusinessRuleException("Payroll posting account ($code) is inactive")
        }
        return account
    }

    private fun line(
        account: Account,
        debit: BigDecimal = BigDecimal.ZERO,
        credit: BigDecimal = BigDecimal.ZERO,
        description: String,
    ) = JournalEntryLine(
        accountId = account.id,
        accountCode = account.code,
        accountName = account.name,
        debit = debit,
        credit = credit,
        description = description,
    )

    private fun monthlyGross(
        rate: BigDecimal,
        period: PayPeriod,
        decimals: Int,
    ): BigDecimal? =
        when (period) {
            PayPeriod.ANNUAL -> rate.divide(BigDecimal(12), decimals, RoundingMode.HALF_UP)
            PayPeriod.MONTHLY -> rate.setScale(decimals, RoundingMode.HALF_UP)
            PayPeriod.HOURLY -> null
        }

    private fun saveWithRetry(
        organizationId: String,
        maxRetries: Int = 3,
        build: (String) -> PayrollRun,
    ): PayrollRun {
        repeat(maxRetries) { attempt ->
            val count = payrollRunRepository.countByOrganizationId(organizationId)
            val number = "PAY-${(count + 1).toString().padStart(4, '0')}"
            try {
                return payrollRunRepository.save(build(number))
            } catch (e: DuplicateKeyException) {
                if (attempt == maxRetries - 1) {
                    throw IllegalStateException("Failed to generate unique payroll run number: $number", e)
                }
            }
        }
        throw IllegalStateException("Failed to generate unique payroll run number")
    }

    private companion object {
        const val SALARY_EXPENSE_CODE = "6000"
        const val WAGES_PAYABLE_CODE = "2200"
        const val CASH_CODE = "1000"
    }
}
