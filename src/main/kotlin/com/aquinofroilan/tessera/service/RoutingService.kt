package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.CreateRoutingOperationRequest
import com.aquinofroilan.tessera.dto.CreateRoutingRequest
import com.aquinofroilan.tessera.dto.UpdateRoutingRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import com.aquinofroilan.tessera.model.Routing
import com.aquinofroilan.tessera.model.RoutingOperation
import com.aquinofroilan.tessera.model.RoutingStatus
import com.aquinofroilan.tessera.repository.RoutingRepository
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Service
class RoutingService(
    private val routingRepository: RoutingRepository,
    private val productService: ProductService,
    private val workCenterService: WorkCenterService,
) {
    @Transactional
    fun createRouting(
        request: CreateRoutingRequest,
        organizationId: String,
        createdBy: String,
    ): Routing {
        val product = productService.getProduct(request.productId, organizationId)
        if (!product.isActive) {
            throw BusinessRuleException("Product '${product.sku}' is inactive")
        }
        validateDates(request.effectiveFrom, request.effectiveTo)
        routingRepository.findByOrganizationIdAndCode(organizationId, request.code).ifPresent {
            throw BusinessRuleException("Routing with code '${request.code}' already exists")
        }
        val operations = buildOperations(request.operations, organizationId)
        val routing =
            Routing(
                organizationId = organizationId,
                productId = product.id,
                code = request.code,
                name = request.name,
                version = request.version ?: nextVersion(organizationId, product.id),
                status = RoutingStatus.DRAFT,
                effectiveFrom = request.effectiveFrom,
                effectiveTo = request.effectiveTo,
                notes = request.notes,
                operations = operations,
                createdBy = createdBy,
            )
        return try {
            routingRepository.save(routing)
        } catch (e: DataIntegrityViolationException) {
            throw BusinessRuleException("Routing with code '${request.code}' already exists")
        }
    }

    fun getRouting(
        id: String,
        organizationId: String,
    ): Routing {
        val r =
            routingRepository.findById(id).orElseThrow {
                ResourceNotFoundException("Routing not found: $id")
            }
        if (r.organizationId != organizationId) {
            throw ResourceNotFoundException("Routing not found: $id")
        }
        return r
    }

    fun listRoutings(
        organizationId: String,
        status: RoutingStatus?,
        productId: String?,
    ): List<Routing> =
        when {
            status != null && productId != null ->
                routingRepository.findByOrganizationIdAndProductIdAndStatus(organizationId, productId, status)
            status != null -> routingRepository.findByOrganizationIdAndStatus(organizationId, status)
            productId != null -> routingRepository.findByOrganizationIdAndProductId(organizationId, productId)
            else -> routingRepository.findByOrganizationId(organizationId)
        }

    @Transactional
    fun updateRouting(
        id: String,
        request: UpdateRoutingRequest,
        organizationId: String,
    ): Routing {
        val routing = getRouting(id, organizationId)
        if (routing.status != RoutingStatus.DRAFT) {
            throw BusinessRuleException("Only DRAFT routings can be edited; this routing is ${routing.status}")
        }
        validateDates(
            request.effectiveFrom ?: routing.effectiveFrom,
            request.effectiveTo ?: routing.effectiveTo,
        )
        val updatedOps =
            request.operations?.let { buildOperations(it, organizationId) } ?: routing.operations
        return routingRepository.save(
            routing.copy(
                name = request.name ?: routing.name,
                effectiveFrom = request.effectiveFrom ?: routing.effectiveFrom,
                effectiveTo = request.effectiveTo ?: routing.effectiveTo,
                notes = request.notes ?: routing.notes,
                operations = updatedOps,
            ),
        )
    }

    @Transactional
    fun activateRouting(
        id: String,
        organizationId: String,
        userId: String,
        makeDefault: Boolean,
    ): Routing {
        val routing = getRouting(id, organizationId)
        if (routing.status == RoutingStatus.OBSOLETE) {
            throw BusinessRuleException("Cannot activate an OBSOLETE routing")
        }
        if (routing.status == RoutingStatus.ACTIVE && !makeDefault) {
            return routing
        }
        if (makeDefault) {
            routingRepository
                .findByOrganizationIdAndProductIdAndIsDefaultTrue(organizationId, routing.productId)
                .ifPresent {
                    if (it.id != routing.id) {
                        routingRepository.save(it.copy(isDefault = false))
                    }
                }
        }
        return routingRepository.save(
            routing.copy(
                status = RoutingStatus.ACTIVE,
                isDefault = if (makeDefault) true else routing.isDefault,
                activatedAt = routing.activatedAt ?: LocalDateTime.now(),
                activatedBy = routing.activatedBy ?: userId,
            ),
        )
    }

    @Transactional
    fun obsoleteRouting(
        id: String,
        organizationId: String,
        userId: String,
    ): Routing {
        val routing = getRouting(id, organizationId)
        if (routing.status == RoutingStatus.OBSOLETE) {
            return routing
        }
        return routingRepository.save(
            routing.copy(
                status = RoutingStatus.OBSOLETE,
                isDefault = false,
                obsoletedAt = LocalDateTime.now(),
                obsoletedBy = userId,
            ),
        )
    }

    @Transactional
    fun deleteRouting(
        id: String,
        organizationId: String,
    ) {
        val routing = getRouting(id, organizationId)
        if (routing.status != RoutingStatus.DRAFT) {
            throw BusinessRuleException("Only DRAFT routings can be deleted")
        }
        routingRepository.delete(routing)
    }

    private fun buildOperations(
        requests: List<CreateRoutingOperationRequest>,
        organizationId: String,
    ): List<RoutingOperation> {
        if (requests.isEmpty()) {
            throw BusinessRuleException("Routing must have at least one operation")
        }
        return requests.mapIndexed { index, opReq ->
            val wc = workCenterService.getWorkCenter(opReq.workCenterId, organizationId)
            if (!wc.isActive) {
                throw BusinessRuleException("Work center '${wc.code}' is inactive")
            }
            val setup = opReq.setupMinutes ?: BigDecimal.ZERO
            val run = opReq.runMinutesPerUnit ?: BigDecimal.ZERO
            val queue = opReq.queueMinutes ?: BigDecimal.ZERO
            if (setup.signum() < 0 || run.signum() < 0 || queue.signum() < 0) {
                throw BusinessRuleException("Operation timings cannot be negative")
            }
            if (setup.signum() == 0 && run.signum() == 0) {
                throw BusinessRuleException("Operation must have positive setup or run time")
            }
            RoutingOperation(
                operationNumber = (index + 1) * 10,
                workCenterId = wc.id,
                workCenterCode = wc.code,
                description = opReq.description,
                setupMinutes = setup,
                runMinutesPerUnit = run,
                queueMinutes = queue,
                instructions = opReq.instructions,
            )
        }
    }

    private fun nextVersion(
        organizationId: String,
        productId: String,
    ): Int {
        val existing = routingRepository.findByOrganizationIdAndProductId(organizationId, productId)
        return (existing.maxOfOrNull { it.version } ?: 0) + 1
    }

    private fun validateDates(
        from: LocalDate?,
        to: LocalDate?,
    ) {
        if (from != null && to != null && to.isBefore(from)) {
            throw BusinessRuleException("Effective-to date must be on or after effective-from date")
        }
    }
}
