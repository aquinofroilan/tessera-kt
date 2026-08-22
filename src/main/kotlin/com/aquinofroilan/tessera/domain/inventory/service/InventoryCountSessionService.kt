package com.aquinofroilan.tessera.domain.inventory.service

import com.aquinofroilan.tessera.domain.inventory.dto.CreateCountSessionRequest
import com.aquinofroilan.tessera.domain.inventory.dto.CreateStockMovementRequest
import com.aquinofroilan.tessera.domain.inventory.dto.RecordCountRequest
import com.aquinofroilan.tessera.domain.inventory.model.InventoryCountLine
import com.aquinofroilan.tessera.domain.inventory.model.InventoryCountSession
import com.aquinofroilan.tessera.domain.inventory.model.InventoryCountStatus
import com.aquinofroilan.tessera.domain.inventory.model.StockMovementType
import com.aquinofroilan.tessera.domain.inventory.repository.InventoryCountSessionRepository
import com.aquinofroilan.tessera.domain.inventory.repository.StockOnHandRepository
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Service
class InventoryCountSessionService(
    private val sessionRepository: InventoryCountSessionRepository,
    private val stockOnHandRepository: StockOnHandRepository,
    private val warehouseService: WarehouseService,
    private val productService: ProductService,
    private val stockMovementService: StockMovementService,
) {
    @Transactional
    fun createSession(
        request: CreateCountSessionRequest,
        organizationId: UUID,
        userId: UUID,
    ): InventoryCountSession {
        val warehouse = warehouseService.getWarehouse(request.warehouseId, organizationId)
        if (!warehouse.isActive) {
            throw BusinessRuleException("Warehouse '${warehouse.code}' is inactive")
        }
        sessionRepository.findByOrganizationIdAndCode(organizationId, request.code).ifPresent {
            throw BusinessRuleException("Count session with code '${request.code}' already exists")
        }
        // Snapshot stock-on-hand for this warehouse into count lines.
        val sohRows =
            stockOnHandRepository
                .findByOrganizationId(organizationId)
                .filter { it.warehouseId == warehouse.id && it.quantity.signum() != 0 }
        val lines =
            sohRows.mapIndexed { idx, soh ->
                val product = productService.getProduct(soh.productId, organizationId)
                InventoryCountLine(
                    lineNumber = idx + 1,
                    productId = product.id,
                    productSku = product.sku,
                    productName = product.name,
                    expectedQuantity = soh.quantity,
                )
            }
        val session =
            InventoryCountSession(
                organizationId = organizationId,
                code = request.code.trim(),
                warehouseId = warehouse.id,
                status = InventoryCountStatus.DRAFT,
                scheduledFor = request.scheduledFor,
                notes = request.notes,
                lines = lines,
                createdBy = userId,
            )
        return try {
            sessionRepository.save(session)
        } catch (e: DataIntegrityViolationException) {
            throw BusinessRuleException("Count session with code '${request.code}' already exists")
        }
    }

    fun getSession(
        id: UUID,
        organizationId: UUID,
    ): InventoryCountSession {
        val s =
            sessionRepository.findById(id).orElseThrow {
                ResourceNotFoundException("Count session not found: $id")
            }
        if (s.organizationId != organizationId) {
            throw ResourceNotFoundException("Count session not found: $id")
        }
        return s
    }

    fun listSessions(
        organizationId: UUID,
        status: InventoryCountStatus?,
    ): List<InventoryCountSession> =
        if (status != null) {
            sessionRepository.findByOrganizationIdAndStatus(organizationId, status)
        } else {
            sessionRepository.findByOrganizationId(organizationId)
        }

    @Transactional
    fun recordCount(
        sessionId: UUID,
        lineId: UUID,
        request: RecordCountRequest,
        organizationId: UUID,
    ): InventoryCountSession {
        val session = getSession(sessionId, organizationId)
        if (session.status == InventoryCountStatus.POSTED || session.status == InventoryCountStatus.CANCELLED) {
            throw BusinessRuleException("Cannot record counts on a ${session.status} session")
        }
        val counted = request.countedQuantity ?: throw BusinessRuleException("countedQuantity is required")
        val updatedLines =
            session.lines.map { line ->
                if (line.id == lineId) {
                    line.copy(countedQuantity = counted, notes = request.notes ?: line.notes)
                } else {
                    line
                }
            }
        if (updatedLines == session.lines) {
            throw ResourceNotFoundException("Count line not found in this session: $lineId")
        }
        val nextStatus =
            if (session.status == InventoryCountStatus.DRAFT) InventoryCountStatus.COUNTING else session.status
        return sessionRepository.save(
            session.copy(
                status = nextStatus,
                startedAt = session.startedAt ?: LocalDateTime.now(),
                lines = updatedLines,
            ),
        )
    }

    @Transactional
    fun postSession(
        sessionId: UUID,
        organizationId: UUID,
        userId: UUID,
    ): InventoryCountSession {
        val session = getSession(sessionId, organizationId)
        if (session.status == InventoryCountStatus.POSTED) {
            throw BusinessRuleException("Session is already POSTED")
        }
        if (session.status == InventoryCountStatus.CANCELLED) {
            throw BusinessRuleException("Cannot post a CANCELLED session")
        }
        val missing = session.lines.filter { it.countedQuantity == null }
        if (missing.isNotEmpty()) {
            throw BusinessRuleException("Cannot post: ${missing.size} line(s) have not been counted")
        }
        val postedLines =
            session.lines.map { line ->
                val counted = line.countedQuantity!!
                val variance = counted.subtract(line.expectedQuantity)
                if (variance.signum() == 0) {
                    line.copy(varianceQuantity = BigDecimal.ZERO)
                } else {
                    val movement =
                        stockMovementService.createMovement(
                            CreateStockMovementRequest(
                                type = StockMovementType.ADJUSTMENT,
                                productId = line.productId,
                                warehouseId = session.warehouseId,
                                quantity = variance,
                                reference = "COUNT-${session.code}",
                                notes = "Physical count adjustment (session ${session.code})",
                            ),
                            organizationId,
                            userId,
                        )
                    line.copy(
                        varianceQuantity = variance,
                        adjustmentMovementId = movement.id,
                    )
                }
            }
        return sessionRepository.save(
            session.copy(
                status = InventoryCountStatus.POSTED,
                postedAt = LocalDateTime.now(),
                postedBy = userId,
                lines = postedLines,
            ),
        )
    }

    @Transactional
    fun cancelSession(
        sessionId: UUID,
        organizationId: UUID,
    ): InventoryCountSession {
        val session = getSession(sessionId, organizationId)
        if (session.status == InventoryCountStatus.POSTED) {
            throw BusinessRuleException("Cannot cancel a POSTED session")
        }
        if (session.status == InventoryCountStatus.CANCELLED) return session
        return sessionRepository.save(session.copy(status = InventoryCountStatus.CANCELLED))
    }
}
