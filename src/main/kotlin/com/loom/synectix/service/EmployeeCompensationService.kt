package com.loom.synectix.service

import com.loom.synectix.dto.CreateEmployeeCompensationRequest
import com.loom.synectix.exception.BusinessRuleException
import com.loom.synectix.exception.ResourceNotFoundException
import com.loom.synectix.model.EmployeeCompensation
import com.loom.synectix.repository.EmployeeCompensationRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class EmployeeCompensationService(
    private val compensationRepository: EmployeeCompensationRepository,
    private val employeeService: EmployeeService,
    private val positionService: PositionService,
    private val currencyService: CurrencyService,
) {
    @Transactional
    fun addCompensation(
        employeeId: String,
        request: CreateEmployeeCompensationRequest,
        organizationId: String,
        createdBy: String,
    ): EmployeeCompensation {
        val employee = employeeService.getEmployee(employeeId, organizationId)
        val payRate = request.payRate ?: throw BusinessRuleException("Pay rate is required")
        val payPeriod = request.payPeriod ?: throw BusinessRuleException("Pay period is required")
        val effectiveDate = request.effectiveDate ?: throw BusinessRuleException("Effective date is required")
        val currency = request.currency.uppercase()
        currencyService.getCurrency(currency)
        request.positionId?.let { positionService.getPosition(it, organizationId) }

        return compensationRepository.save(
            EmployeeCompensation(
                employeeId = employee.id,
                positionId = request.positionId,
                payRate = payRate,
                currency = currency,
                payPeriod = payPeriod,
                effectiveDate = effectiveDate,
                organizationId = organizationId,
                createdBy = createdBy,
            ),
        )
    }

    fun listCompensation(
        employeeId: String,
        organizationId: String,
    ): List<EmployeeCompensation> {
        employeeService.getEmployee(employeeId, organizationId)
        return compensationRepository.findByOrganizationIdAndEmployeeIdOrderByEffectiveDateDesc(organizationId, employeeId)
    }

    /** The compensation record in effect on [asOf] — the latest with effectiveDate on or before it. */
    fun currentCompensation(
        employeeId: String,
        organizationId: String,
        asOf: LocalDate,
    ): EmployeeCompensation {
        employeeService.getEmployee(employeeId, organizationId)
        return compensationRepository
            .findByOrganizationIdAndEmployeeIdOrderByEffectiveDateDesc(organizationId, employeeId)
            .firstOrNull { !it.effectiveDate.isAfter(asOf) }
            ?: throw ResourceNotFoundException("No compensation effective on or before $asOf for this employee")
    }
}
