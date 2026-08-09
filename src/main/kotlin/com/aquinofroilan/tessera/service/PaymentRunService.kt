package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.CreatePaymentRunRequest
import com.aquinofroilan.tessera.dto.RecordPaymentRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import com.aquinofroilan.tessera.model.BillStatus
import com.aquinofroilan.tessera.model.PaymentMethod
import com.aquinofroilan.tessera.model.PaymentRun
import com.aquinofroilan.tessera.model.PaymentRunLine
import com.aquinofroilan.tessera.model.PaymentRunLineStatus
import com.aquinofroilan.tessera.model.PaymentRunStatus
import com.aquinofroilan.tessera.repository.PaymentRunRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
class PaymentRunService(
    private val paymentRunRepository: PaymentRunRepository,
    private val bankAccountService: BankAccountService,
    private val billService: BillService,
    private val accountService: AccountService,
) {
    @Transactional
    fun createPaymentRun(
        request: CreatePaymentRunRequest,
        organizationId: String,
        userId: String,
    ): PaymentRun {
        val runDate = request.runDate ?: throw BusinessRuleException("runDate is required")
        if (request.billIds.isEmpty()) throw BusinessRuleException("At least one bill ID is required")
        if (request.billIds.distinct().size != request.billIds.size) {
            throw BusinessRuleException("Duplicate bill IDs in request")
        }
        val code = request.code.trim().uppercase()
        if (code.isBlank()) throw BusinessRuleException("Code cannot be blank")
        paymentRunRepository.findByOrganizationIdAndCode(organizationId, code).ifPresent {
            throw BusinessRuleException("Payment run with code '$code' already exists")
        }
        val bankAccount = bankAccountService.getBankAccount(request.bankAccountId, organizationId)
        if (!bankAccount.isActive) throw BusinessRuleException("Bank account '${bankAccount.code}' is inactive")

        val lines =
            request.billIds.mapIndexed { idx, billId ->
                val bill = billService.getBill(billId, organizationId)
                if (bill.status != BillStatus.APPROVED && bill.status != BillStatus.PARTIALLY_PAID) {
                    throw BusinessRuleException(
                        "Bill ${bill.billNumber} is ${bill.status}; only APPROVED / PARTIALLY_PAID bills can be paid",
                    )
                }
                if (bill.currencyCode != bankAccount.currency) {
                    throw BusinessRuleException(
                        "Bill ${bill.billNumber} currency ${bill.currencyCode} does not match bank ${bankAccount.currency}",
                    )
                }
                val remaining = bill.totalAmount.subtract(bill.amountPaid)
                if (remaining.signum() <= 0) {
                    throw BusinessRuleException("Bill ${bill.billNumber} has no remaining balance")
                }
                PaymentRunLine(
                    lineNumber = idx + 1,
                    billId = bill.id,
                    vendorId = bill.vendorId,
                    vendorName = bill.vendorName,
                    billNumber = bill.billNumber,
                    amount = remaining,
                )
            }
        val total = lines.fold(BigDecimal.ZERO) { acc, l -> acc.add(l.amount) }
        return try {
            paymentRunRepository.save(
                PaymentRun(
                    organizationId = organizationId,
                    code = code,
                    bankAccountId = bankAccount.id,
                    runDate = runDate,
                    status = PaymentRunStatus.DRAFT,
                    totalAmount = total,
                    currency = bankAccount.currency,
                    notes = request.notes,
                    lines = lines,
                    createdBy = userId,
                ),
            )
        } catch (e: DataIntegrityViolationException) {
            throw BusinessRuleException("Payment run with code '$code' already exists")
        }
    }

    fun getPaymentRun(
        id: String,
        organizationId: String,
    ): PaymentRun {
        val r =
            paymentRunRepository.findById(id).orElseThrow {
                ResourceNotFoundException("Payment run not found: $id")
            }
        if (r.organizationId != organizationId) {
            throw ResourceNotFoundException("Payment run not found: $id")
        }
        return r
    }

    fun listPaymentRuns(
        organizationId: String,
        status: PaymentRunStatus?,
    ): List<PaymentRun> =
        if (status != null) {
            paymentRunRepository.findByOrganizationIdAndStatus(organizationId, status)
        } else {
            paymentRunRepository.findByOrganizationId(organizationId)
        }

    @Transactional
    fun approvePaymentRun(
        id: String,
        organizationId: String,
        userId: String,
    ): PaymentRun {
        val run = getPaymentRun(id, organizationId)
        if (run.status != PaymentRunStatus.DRAFT) {
            throw BusinessRuleException("Only DRAFT payment runs can be approved (current: ${run.status})")
        }
        return paymentRunRepository.save(
            run.copy(
                status = PaymentRunStatus.APPROVED,
                approvedAt = LocalDateTime.now(),
                approvedBy = userId,
            ),
        )
    }

    @Transactional
    fun executePaymentRun(
        id: String,
        organizationId: String,
        userId: String,
    ): PaymentRun {
        val run = getPaymentRun(id, organizationId)
        if (run.status != PaymentRunStatus.APPROVED) {
            throw BusinessRuleException("Only APPROVED payment runs can be executed (current: ${run.status})")
        }
        val bankAccount = bankAccountService.getBankAccount(run.bankAccountId, organizationId)
        val glAccount = accountService.getAccount(bankAccount.glAccountId, organizationId)

        var paidTotal = BigDecimal.ZERO
        val executedLines =
            run.lines.map { line ->
                if (line.status != PaymentRunLineStatus.PENDING) return@map line
                try {
                    val payment =
                        billService.recordPayment(
                            billId = line.billId,
                            request =
                                RecordPaymentRequest(
                                    paymentDate = run.runDate,
                                    amount = line.amount,
                                    paymentMethod = PaymentMethod.BANK_TRANSFER,
                                    referenceNumber = "PAYRUN-${run.code}",
                                ),
                            organizationId = organizationId,
                            createdBy = userId,
                            cashAccountOverride = glAccount,
                        )
                    paidTotal = paidTotal.add(line.amount)
                    line.copy(status = PaymentRunLineStatus.PAID, billPaymentId = payment.id)
                } catch (e: Exception) {
                    line.copy(status = PaymentRunLineStatus.FAILED, notes = e.message?.take(500))
                }
            }
        if (paidTotal.signum() > 0) {
            bankAccountService.applyBalanceDelta(run.bankAccountId, organizationId, paidTotal.negate())
        }
        return paymentRunRepository.save(
            run.copy(
                status = PaymentRunStatus.EXECUTED,
                executedAt = LocalDateTime.now(),
                executedBy = userId,
                lines = executedLines,
            ),
        )
    }

    @Transactional
    fun cancelPaymentRun(
        id: String,
        organizationId: String,
        userId: String,
    ): PaymentRun {
        val run = getPaymentRun(id, organizationId)
        if (run.status == PaymentRunStatus.EXECUTED) {
            throw BusinessRuleException("Cannot cancel an EXECUTED payment run")
        }
        if (run.status == PaymentRunStatus.CANCELLED) return run
        return paymentRunRepository.save(
            run.copy(
                status = PaymentRunStatus.CANCELLED,
                cancelledAt = LocalDateTime.now(),
                cancelledBy = userId,
            ),
        )
    }
}
