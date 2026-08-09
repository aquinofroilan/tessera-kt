package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.CashFlowBucket
import com.aquinofroilan.tessera.dto.CashFlowForecastResponse
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.model.BillStatus
import com.aquinofroilan.tessera.model.InvoiceStatus
import com.aquinofroilan.tessera.repository.BankAccountRepository
import com.aquinofroilan.tessera.repository.BillRepository
import com.aquinofroilan.tessera.repository.InvoiceRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/**
 * Read-only cash-flow projection. Starts from the current cash position
 * (sum of active bank accounts' current_balance), then walks open AR
 * invoices and open AP bills with due dates inside the horizon to
 * project per-week inflow and outflow. Anything overdue at the start of
 * the run is reported separately so a user can see latent risk before
 * deciding what to chase or pay.
 *
 * Single-currency for now: amounts are summed in their native currency
 * regardless of bank account currency mix; multi-currency normalisation
 * is a follow-up that depends on the org-level base currency exchange
 * rates the FX service exposes.
 */
@Service
class CashFlowForecastService(
    private val bankAccountRepository: BankAccountRepository,
    private val invoiceRepository: InvoiceRepository,
    private val billRepository: BillRepository,
) {
    fun forecast(
        organizationId: String,
        asOfDate: LocalDate?,
        horizonEnd: LocalDate?,
    ): CashFlowForecastResponse {
        val asOf = asOfDate ?: LocalDate.now()
        val horizon = horizonEnd ?: asOf.plusWeeks(13)
        if (!horizon.isAfter(asOf)) {
            throw BusinessRuleException("horizonEnd must be after asOfDate")
        }

        val activeBanks = bankAccountRepository.findByOrganizationIdAndIsActive(organizationId, true)
        val startingCash = activeBanks.fold(BigDecimal.ZERO) { acc, b -> acc.add(b.currentBalance) }
        val currency = activeBanks.firstOrNull()?.currency ?: "USD"

        val openInvoices =
            invoiceRepository
                .findByOrganizationIdAndStatusIn(organizationId, listOf(InvoiceStatus.APPROVED, InvoiceStatus.PARTIALLY_PAID))
                .map { inv ->
                    OpenItem(
                        dueDate = inv.dueDate,
                        amountRemaining = inv.totalAmount.subtract(inv.amountReceived),
                    )
                }.filter { it.amountRemaining.signum() > 0 }
        val openBills =
            billRepository
                .findByOrganizationIdAndStatusIn(organizationId, listOf(BillStatus.APPROVED, BillStatus.PARTIALLY_PAID))
                .map { bill ->
                    OpenItem(
                        dueDate = bill.dueDate,
                        amountRemaining = bill.totalAmount.subtract(bill.amountPaid),
                    )
                }.filter { it.amountRemaining.signum() > 0 }

        val overdueAr =
            openInvoices.filter { it.dueDate.isBefore(asOf) }.fold(BigDecimal.ZERO) { acc, i -> acc.add(i.amountRemaining) }
        val overdueAp =
            openBills.filter { it.dueDate.isBefore(asOf) }.fold(BigDecimal.ZERO) { acc, i -> acc.add(i.amountRemaining) }

        // Bucket future items by ISO week ending (Sunday).
        val futureInflows = openInvoices.filter { !it.dueDate.isBefore(asOf) && !it.dueDate.isAfter(horizon) }
        val futureOutflows = openBills.filter { !it.dueDate.isBefore(asOf) && !it.dueDate.isAfter(horizon) }

        val bucketEnds = generateWeekEnds(asOf, horizon)
        val inflowByBucket =
            futureInflows
                .groupBy { bucketEndFor(it.dueDate, bucketEnds) }
                .mapValues { (_, items) -> items.fold(BigDecimal.ZERO) { acc, i -> acc.add(i.amountRemaining) } }
        val outflowByBucket =
            futureOutflows
                .groupBy { bucketEndFor(it.dueDate, bucketEnds) }
                .mapValues { (_, items) -> items.fold(BigDecimal.ZERO) { acc, i -> acc.add(i.amountRemaining) } }

        var running = startingCash
        val buckets =
            bucketEnds.map { end ->
                val inflow = inflowByBucket[end] ?: BigDecimal.ZERO
                val outflow = outflowByBucket[end] ?: BigDecimal.ZERO
                val net = inflow.subtract(outflow)
                running = running.add(net)
                CashFlowBucket(periodEnd = end, inflow = inflow, outflow = outflow, net = net, runningBalance = running)
            }

        val totalIn = buckets.fold(BigDecimal.ZERO) { acc, b -> acc.add(b.inflow) }
        val totalOut = buckets.fold(BigDecimal.ZERO) { acc, b -> acc.add(b.outflow) }

        return CashFlowForecastResponse(
            asOfDate = asOf,
            horizonEnd = horizon,
            currency = currency,
            startingCash = startingCash,
            totalInflow = totalIn,
            totalOutflow = totalOut,
            projectedEndingCash = running,
            overdueAr = overdueAr,
            overdueAp = overdueAp,
            buckets = buckets,
        )
    }

    private data class OpenItem(
        val dueDate: LocalDate,
        val amountRemaining: BigDecimal,
    )

    private fun generateWeekEnds(
        asOf: LocalDate,
        horizon: LocalDate,
    ): List<LocalDate> {
        val first = asOf.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
        val result = mutableListOf<LocalDate>()
        var cursor = first
        while (!cursor.isAfter(horizon)) {
            result.add(cursor)
            cursor = cursor.plusWeeks(1)
        }
        if (result.isEmpty() || result.last() != horizon) result.add(horizon)
        return result
    }

    private fun bucketEndFor(
        date: LocalDate,
        bucketEnds: List<LocalDate>,
    ): LocalDate = bucketEnds.firstOrNull { !date.isAfter(it) } ?: bucketEnds.last()
}
