package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.CreatePositionRequest
import com.aquinofroilan.tessera.dto.UpdatePositionRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import com.aquinofroilan.tessera.model.Position
import com.aquinofroilan.tessera.repository.PositionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PositionService(
    private val positionRepository: PositionRepository,
    private val departmentService: DepartmentService,
) {
    @Transactional
    fun createPosition(
        request: CreatePositionRequest,
        organizationId: String,
    ): Position {
        val code = request.code.trim()
        if (positionRepository.findByOrganizationIdAndCode(organizationId, code).isPresent) {
            throw BusinessRuleException("Position code '$code' already exists")
        }
        request.departmentId?.let { departmentService.getDepartment(it, organizationId) }
        return positionRepository.save(
            Position(
                code = code,
                title = request.title.trim(),
                departmentId = request.departmentId,
                payGrade = request.payGrade?.trim(),
                organizationId = organizationId,
            ),
        )
    }

    fun getPosition(
        id: String,
        organizationId: String,
    ): Position {
        val position =
            positionRepository.findById(id).orElseThrow {
                ResourceNotFoundException("Position not found")
            }
        if (position.organizationId != organizationId) {
            throw ResourceNotFoundException("Position not found")
        }
        return position
    }

    fun listPositions(
        organizationId: String,
        activeOnly: Boolean = false,
    ): List<Position> =
        if (activeOnly) {
            positionRepository.findByOrganizationIdAndIsActive(organizationId, true)
        } else {
            positionRepository.findByOrganizationId(organizationId)
        }

    @Transactional
    fun updatePosition(
        id: String,
        request: UpdatePositionRequest,
        organizationId: String,
    ): Position {
        val position = getPosition(id, organizationId)
        request.departmentId?.let { departmentService.getDepartment(it, organizationId) }
        position.apply {
            title = request.title?.trim() ?: position.title
            departmentId = request.departmentId ?: position.departmentId
            payGrade = request.payGrade?.trim() ?: position.payGrade
        }
        return positionRepository.save(position)
    }

    @Transactional
    fun deactivatePosition(
        id: String,
        organizationId: String,
    ): Position {
        val position = getPosition(id, organizationId)
        if (!position.isActive) {
            throw BusinessRuleException("Position is already inactive")
        }
        position.isActive = false
        return positionRepository.save(position)
    }
}
