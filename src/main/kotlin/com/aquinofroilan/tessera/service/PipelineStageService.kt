package com.aquinofroilan.tessera.service
import com.aquinofroilan.tessera.dto.CreatePipelineStageRequest
import com.aquinofroilan.tessera.dto.UpdatePipelineStageRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import com.aquinofroilan.tessera.model.PipelineStage
import com.aquinofroilan.tessera.repository.PipelineStageRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.util.UUID

@Service
class PipelineStageService(
    private val pipelineStageRepository: PipelineStageRepository,
) {
    @Transactional
    fun createStage(
        request: CreatePipelineStageRequest,
        organizationId: UUID,
    ): PipelineStage {
        val code = request.code.trim().uppercase()
        if (code.isBlank()) throw BusinessRuleException("Code cannot be blank")
        if (request.isWon && request.isLost) {
            throw BusinessRuleException("A stage cannot be both won and lost")
        }
        pipelineStageRepository.findByOrganizationIdAndCode(organizationId, code).ifPresent {
            throw BusinessRuleException("Pipeline stage with code '$code' already exists")
        }
        val nextSort =
            request.sortOrder
                ?: (
                    (
                        pipelineStageRepository
                            .findByOrganizationIdOrderBySortOrderAsc(organizationId)
                            .maxOfOrNull { it.sortOrder } ?: 0
                    ) + 10
                )
        val stage =
            PipelineStage(
                organizationId = organizationId,
                code = code,
                name = request.name.trim(),
                description = request.description,
                sortOrder = nextSort,
                probabilityPct = request.probabilityPct ?: BigDecimal.ZERO,
                isWon = request.isWon,
                isLost = request.isLost,
            )
        return try {
            pipelineStageRepository.save(stage)
        } catch (e: DataIntegrityViolationException) {
            throw BusinessRuleException("Pipeline stage with code '$code' already exists")
        }
    }

    fun getStage(
        id: UUID,
        organizationId: UUID,
    ): PipelineStage {
        val s =
            pipelineStageRepository.findById(id).orElseThrow {
                ResourceNotFoundException("Pipeline stage not found: $id")
            }
        if (s.organizationId != organizationId) {
            throw ResourceNotFoundException("Pipeline stage not found: $id")
        }
        return s
    }

    fun listStages(
        organizationId: UUID,
        activeOnly: Boolean,
    ): List<PipelineStage> =
        if (activeOnly) {
            pipelineStageRepository.findByOrganizationIdAndIsActiveOrderBySortOrderAsc(organizationId, true)
        } else {
            pipelineStageRepository.findByOrganizationIdOrderBySortOrderAsc(organizationId)
        }

    @Transactional
    fun updateStage(
        id: UUID,
        request: UpdatePipelineStageRequest,
        organizationId: UUID,
    ): PipelineStage {
        val s = getStage(id, organizationId)
        val nextWon = request.isWon ?: s.isWon
        val nextLost = request.isLost ?: s.isLost
        if (nextWon && nextLost) {
            throw BusinessRuleException("A stage cannot be both won and lost")
        }
        return pipelineStageRepository.save(
            s.copy(
                name = request.name?.trim() ?: s.name,
                description = request.description ?: s.description,
                sortOrder = request.sortOrder ?: s.sortOrder,
                probabilityPct = request.probabilityPct ?: s.probabilityPct,
                isWon = nextWon,
                isLost = nextLost,
                isActive = request.isActive ?: s.isActive,
            ),
        )
    }

    @Transactional
    fun deactivateStage(
        id: UUID,
        organizationId: UUID,
    ): PipelineStage {
        val s = getStage(id, organizationId)
        if (!s.isActive) {
            throw BusinessRuleException("Pipeline stage '${s.code}' is already inactive")
        }
        return pipelineStageRepository.save(s.copy(isActive = false))
    }
}
