package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.CreateLeaveTypeRequest
import com.aquinofroilan.tessera.dto.UpdateLeaveTypeRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import com.aquinofroilan.tessera.model.LeaveType
import com.aquinofroilan.tessera.repository.LeaveTypeRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class LeaveTypeService(
    private val leaveTypeRepository: LeaveTypeRepository,
) {
    @Transactional
    fun createLeaveType(
        request: CreateLeaveTypeRequest,
        organizationId: String,
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
        id: String,
        organizationId: String,
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
        organizationId: String,
        activeOnly: Boolean = false,
    ): List<LeaveType> =
        if (activeOnly) {
            leaveTypeRepository.findByOrganizationIdAndIsActive(organizationId, true)
        } else {
            leaveTypeRepository.findByOrganizationId(organizationId)
        }

    @Transactional
    fun updateLeaveType(
        id: String,
        request: UpdateLeaveTypeRequest,
        organizationId: String,
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
        id: String,
        organizationId: String,
    ): LeaveType {
        val leaveType = getLeaveType(id, organizationId)
        if (!leaveType.isActive) {
            throw BusinessRuleException("Leave type is already inactive")
        }
        leaveType.isActive = false
        return leaveTypeRepository.save(leaveType)
    }
}
