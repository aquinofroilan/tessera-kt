package com.aquinofroilan.tessera.domain.hr.service

import com.aquinofroilan.tessera.domain.hr.dto.CreateLeaveTypeRequest
import com.aquinofroilan.tessera.domain.hr.dto.UpdateLeaveTypeRequest
import com.aquinofroilan.tessera.domain.hr.model.LeaveType
import com.aquinofroilan.tessera.domain.hr.repository.LeaveTypeRepository
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class LeaveTypeService(
    private val leaveTypeRepository: LeaveTypeRepository,
) {
    @Transactional
    fun createLeaveType(
        request: CreateLeaveTypeRequest,
        organizationId: java.util.UUID,
    ): LeaveType {
        val code = request.code.trim()
        if (leaveTypeRepository.findByOrganizationIdAndCode(organizationId, code).isPresent) {
            throw BusinessRuleException("Leave type code '$code' already exists")
        }
        return leaveTypeRepository.save(
            LeaveType(
                code = code,
                name = request.name.trim(),
                paid = request.paid,
                defaultAnnualDays = request.defaultAnnualDays,
                organizationId = organizationId,
            ),
        )
    }

    fun getLeaveType(
        id: java.util.UUID,
        organizationId: java.util.UUID,
    ): LeaveType {
        val leaveType =
            leaveTypeRepository.findById(id).orElseThrow {
                ResourceNotFoundException("Leave type not found")
            }
        if (leaveType.organizationId != organizationId) {
            throw ResourceNotFoundException("Leave type not found")
        }
        return leaveType
    }

    fun listLeaveTypes(
        organizationId: java.util.UUID,
        activeOnly: Boolean = false,
    ): List<LeaveType> =
        if (activeOnly) {
            leaveTypeRepository.findByOrganizationIdAndIsActive(organizationId, true)
        } else {
            leaveTypeRepository.findByOrganizationId(organizationId)
        }

    @Transactional
    fun updateLeaveType(
        id: java.util.UUID,
        request: UpdateLeaveTypeRequest,
        organizationId: java.util.UUID,
    ): LeaveType {
        val leaveType = getLeaveType(id, organizationId)
        leaveType.apply {
            name = request.name?.trim() ?: leaveType.name
            paid = request.paid ?: leaveType.paid
            defaultAnnualDays = request.defaultAnnualDays ?: leaveType.defaultAnnualDays
        }
        return leaveTypeRepository.save(leaveType)
    }

    @Transactional
    fun deactivateLeaveType(
        id: java.util.UUID,
        organizationId: java.util.UUID,
    ): LeaveType {
        val leaveType = getLeaveType(id, organizationId)
        if (!leaveType.isActive) {
            throw BusinessRuleException("Leave type is already inactive")
        }
        leaveType.isActive = false
        return leaveTypeRepository.save(leaveType)
    }
}
