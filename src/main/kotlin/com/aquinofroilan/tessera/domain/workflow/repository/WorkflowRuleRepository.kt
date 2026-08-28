package com.aquinofroilan.tessera.domain.workflow.repository

import com.aquinofroilan.tessera.domain.workflow.model.WorkflowRule
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface WorkflowRuleRepository : JpaRepository<WorkflowRule, java.util.UUID> {
    fun findByOrganizationId(organizationId: java.util.UUID): List<WorkflowRule>

    fun findByOrganizationIdAndEventKind(
        organizationId: java.util.UUID,
        eventKind: String,
    ): List<WorkflowRule>

    fun findByOrganizationIdAndEventKindAndEnabledTrue(
        organizationId: java.util.UUID,
        eventKind: String,
    ): List<WorkflowRule>
}
