package com.aquinofroilan.tessera.domain.organization.service

import com.aquinofroilan.tessera.domain.organization.dto.OrganizationStatusResponse
import com.aquinofroilan.tessera.domain.organization.model.OrganizationStatus
import com.aquinofroilan.tessera.domain.organization.repository.OrganizationRepository
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import com.github.benmanes.caffeine.cache.Caffeine
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID
import java.util.concurrent.TimeUnit

@Service
class OrganizationLifecycleService(
    private val organizationRepository: OrganizationRepository,
) {
    private val log = LoggerFactory.getLogger(OrganizationLifecycleService::class.java)

    private val statusCache =
        Caffeine
            .newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .maximumSize(10_000)
            .build<UUID, OrganizationStatus>()

    fun getAllowedTransitions(currentStatus: OrganizationStatus): List<OrganizationStatus> =
        when (currentStatus) {
            OrganizationStatus.ACTIVE -> listOf(OrganizationStatus.SUSPENDED, OrganizationStatus.ARCHIVED)
            OrganizationStatus.SUSPENDED -> listOf(OrganizationStatus.ACTIVE, OrganizationStatus.ARCHIVED)
            OrganizationStatus.ARCHIVED -> listOf(OrganizationStatus.ACTIVE, OrganizationStatus.SUSPENDED)
        }

    @Transactional(readOnly = true)
    fun getStatus(organizationId: UUID): OrganizationStatusResponse {
        val org =
            organizationRepository.findById(organizationId).orElseThrow {
                ResourceNotFoundException("Organization $organizationId not found")
            }
        statusCache.put(organizationId, org.status)
        return OrganizationStatusResponse.from(org, getAllowedTransitions(org.status))
    }

    fun getCachedStatus(organizationId: UUID): OrganizationStatus =
        statusCache.get(organizationId) { id ->
            organizationRepository.findById(id).map { it.status }.orElse(OrganizationStatus.ACTIVE)
        } ?: OrganizationStatus.ACTIVE

    @Transactional
    fun transitionStatus(
        organizationId: UUID,
        targetStatus: OrganizationStatus,
        reason: String? = null,
    ): OrganizationStatusResponse {
        val org =
            organizationRepository.findById(organizationId).orElseThrow {
                ResourceNotFoundException("Organization $organizationId not found")
            }

        val currentStatus = org.status
        if (currentStatus == targetStatus) {
            throw BusinessRuleException("Organization is already in status $targetStatus")
        }

        val allowedTransitions = getAllowedTransitions(currentStatus)
        if (targetStatus !in allowedTransitions) {
            throw BusinessRuleException("Cannot transition organization from $currentStatus to $targetStatus")
        }

        log.info(
            "Transitioning organization {} ({}) from {} to {}. Reason: {}",
            org.uuid,
            org.orgSlug,
            currentStatus,
            targetStatus,
            reason ?: "None",
        )

        org.status = targetStatus
        org.isActive = (targetStatus == OrganizationStatus.ACTIVE)

        val saved = organizationRepository.save(org)
        statusCache.put(organizationId, targetStatus)

        return OrganizationStatusResponse.from(saved, getAllowedTransitions(saved.status))
    }

    fun invalidateCache(organizationId: UUID) {
        statusCache.invalidate(organizationId)
    }
}
