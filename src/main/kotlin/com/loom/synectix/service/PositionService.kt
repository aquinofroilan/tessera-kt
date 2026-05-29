package com.loom.synectix.service

import com.loom.synectix.dto.CreatePositionRequest
import com.loom.synectix.dto.UpdatePositionRequest
import com.loom.synectix.exception.BusinessRuleException
import com.loom.synectix.exception.ResourceNotFoundException
import com.loom.synectix.model.Position
import com.loom.synectix.repository.PositionRepository
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
        return positionRepository.save(
            position.copy(
                title = request.title?.trim() ?: position.title,
                departmentId = request.departmentId ?: position.departmentId,
                payGrade = request.payGrade?.trim() ?: position.payGrade,
            ),
        )
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
        return positionRepository.save(position.copy(isActive = false))
    }
}
