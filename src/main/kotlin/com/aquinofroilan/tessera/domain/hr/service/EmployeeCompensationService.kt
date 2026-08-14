package com.aquinofroilan.tessera.domain.hr.service

import com.aquinofroilan.tessera.domain.finance.service.CurrencyService
import com.aquinofroilan.tessera.domain.hr.dto.CreateEmployeeCompensationRequest
import com.aquinofroilan.tessera.domain.hr.model.EmployeeCompensation
import com.aquinofroilan.tessera.domain.hr.repository.EmployeeCompensationRepository
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
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
        employeeId: java.util.UUID,
        request: CreateEmployeeCompensationRequest,
        organizationId: java.util.UUID,
        createdBy: java.util.UUID,
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
        employeeId: java.util.UUID,
        organizationId: java.util.UUID,
    ): List<EmployeeCompensation> {
        employeeService.getEmployee(employeeId, organizationId)
        return compensationRepository.findByOrganizationIdAndEmployeeIdOrderByEffectiveDateDesc(organizationId, employeeId)
    }

    /** The compensation record in effect on [asOf] — the latest with effectiveDate on or before it. */
    fun currentCompensation(
        employeeId: java.util.UUID,
        organizationId: java.util.UUID,
        asOf: LocalDate,
    ): EmployeeCompensation =
        currentCompensationOrNull(employeeId, organizationId, asOf)
            ?: throw ResourceNotFoundException("No compensation effective on or before $asOf for this employee")

    /** Like [currentCompensation] but returns null instead of throwing when none applies. */
    fun currentCompensationOrNull(
        employeeId: java.util.UUID,
        organizationId: java.util.UUID,
        asOf: LocalDate,
    ): EmployeeCompensation? {
        employeeService.getEmployee(employeeId, organizationId)
        return compensationRepository
            .findByOrganizationIdAndEmployeeIdOrderByEffectiveDateDesc(organizationId, employeeId)
            .firstOrNull { !it.effectiveDate.isAfter(asOf) }
    }
}
