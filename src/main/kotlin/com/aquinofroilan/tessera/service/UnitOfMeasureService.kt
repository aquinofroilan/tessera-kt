package com.aquinofroilan.tessera.service

import java.util.UUID

import com.aquinofroilan.tessera.dto.CreateUomRequest
import com.aquinofroilan.tessera.dto.UpdateUomRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import com.aquinofroilan.tessera.model.UnitOfMeasure
import com.aquinofroilan.tessera.repository.UnitOfMeasureRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode

@Service
class UnitOfMeasureService(
    private val uomRepository: UnitOfMeasureRepository,
) {
    @Transactional
    fun createUom(
        request: CreateUomRequest,
        organizationId: UUID,
    ): UnitOfMeasure {
        val code = request.code.trim().uppercase()
        if (code.isBlank()) throw BusinessRuleException("Code cannot be blank")
        uomRepository.findByOrganizationIdAndCode(organizationId, code).ifPresent {
            throw BusinessRuleException("UoM with code '$code' already exists")
        }
        if (request.baseUomId != null) {
            val base = getUom(request.baseUomId, organizationId)
            if (base.baseUomId != null) {
                throw BusinessRuleException("base UoM must itself be a base (no chained conversions)")
            }
        }
        val factor = request.conversionFactor ?: BigDecimal.ONE
        if (factor.signum() <= 0) throw BusinessRuleException("Conversion factor must be positive")
        if (request.baseUomId == null && factor.compareTo(BigDecimal.ONE) != 0) {
            throw BusinessRuleException("A base UoM must have a conversion factor of 1")
        }
        val uom =
            UnitOfMeasure(
                organizationId = organizationId,
                code = code,
                name = request.name.trim(),
                description = request.description,
                baseUomId = request.baseUomId,
                conversionFactor = factor,
            )
        return try {
            uomRepository.save(uom)
        } catch (e: DataIntegrityViolationException) {
            throw BusinessRuleException("UoM with code '$code' already exists")
        }
    }

    fun getUom(
        id: UUID,
        organizationId: UUID,
    ): UnitOfMeasure {
        val u =
            uomRepository.findById(id).orElseThrow {
                ResourceNotFoundException("UoM not found: $id")
            }
        if (u.organizationId != organizationId) {
            throw ResourceNotFoundException("UoM not found: $id")
        }
        return u
    }

    fun listUoms(
        organizationId: UUID,
        activeOnly: Boolean,
    ): List<UnitOfMeasure> =
        if (activeOnly) {
            uomRepository.findByOrganizationIdAndIsActive(organizationId, true)
        } else {
            uomRepository.findByOrganizationId(organizationId)
        }

    @Transactional
    fun updateUom(
        id: UUID,
        request: UpdateUomRequest,
        organizationId: UUID,
    ): UnitOfMeasure {
        val u = getUom(id, organizationId)
        val newBase = request.baseUomId ?: u.baseUomId
        if (request.baseUomId != null) {
            val base = getUom(request.baseUomId, organizationId)
            if (base.baseUomId != null) {
                throw BusinessRuleException("base UoM must itself be a base (no chained conversions)")
            }
        }
        val newFactor = request.conversionFactor ?: u.conversionFactor
        if (newFactor.signum() <= 0) throw BusinessRuleException("Conversion factor must be positive")
        if (newBase == null && newFactor.compareTo(BigDecimal.ONE) != 0) {
            throw BusinessRuleException("A base UoM must have a conversion factor of 1")
        }
        return uomRepository.save(
            u.copy(
                name = request.name?.trim() ?: u.name,
                description = request.description ?: u.description,
                baseUomId = newBase,
                conversionFactor = newFactor,
                isActive = request.isActive ?: u.isActive,
            ),
        )
    }

    @Transactional
    fun deactivateUom(
        id: UUID,
        organizationId: UUID,
    ): UnitOfMeasure {
        val u = getUom(id, organizationId)
        if (!u.isActive) throw BusinessRuleException("UoM '${u.code}' is already inactive")
        return uomRepository.save(u.copy(isActive = false))
    }

    /**
     * Convert a quantity expressed in [fromUomId] to the equivalent in [toUomId].
     * Both UoMs must share the same base UoM (or be the same base) or this throws.
     */
    fun convert(
        quantity: BigDecimal,
        fromUomId: UUID,
        toUomId: UUID,
        organizationId: UUID,
    ): BigDecimal {
        if (fromUomId == toUomId) return quantity
        val from = getUom(fromUomId, organizationId)
        val to = getUom(toUomId, organizationId)
        val fromBase = from.baseUomId ?: from.id
        val toBase = to.baseUomId ?: to.id
        if (fromBase != toBase) {
            throw BusinessRuleException("Cannot convert between UoMs with different base units")
        }
        val inBase = quantity.multiply(from.conversionFactor)
        return inBase.divide(to.conversionFactor, 6, RoundingMode.HALF_UP)
    }
}
