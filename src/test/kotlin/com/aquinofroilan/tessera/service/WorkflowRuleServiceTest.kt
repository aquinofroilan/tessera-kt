package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.CreateWorkflowRuleRequest
import com.aquinofroilan.tessera.dto.UpdateWorkflowRuleRequest
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import com.aquinofroilan.tessera.model.WorkflowRule
import com.aquinofroilan.tessera.model.WorkflowRuleActionType
import com.aquinofroilan.tessera.repository.WorkflowRuleRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Optional

class WorkflowRuleServiceTest {
    private lateinit var repository: WorkflowRuleRepository
    private lateinit var service: WorkflowRuleService

    private val orgId: java.util.UUID = java.util.UUID.fromString("00000000-0000-0000-0000-000000000100")

    @BeforeEach
    fun setup() {
        repository = mock(WorkflowRuleRepository::class.java)
        whenever(repository.save(any<WorkflowRule>())).thenAnswer { it.arguments[0] }
        service = WorkflowRuleService(repository)
    }

    @Test
    fun `createRule persists the canonical shape with trimmed strings`() {
        service.createRule(
            CreateWorkflowRuleRequest(
                name = "  Notify finance  ",
                description = "  Loop in finance on every PR  ",
                eventKind = "  purchase_request.approved  ",
                actionType = WorkflowRuleActionType.NOTIFY_ROLE,
                actionTarget = "  FINANCE  ",
                enabled = true,
            ),
            orgId,
        )

        val captor = argumentCaptor<WorkflowRule>()
        verify(repository).save(captor.capture())
        val saved = captor.firstValue
        assertThat(saved.name).isEqualTo("Notify finance")
        assertThat(saved.description).isEqualTo("Loop in finance on every PR")
        assertThat(saved.eventKind).isEqualTo("purchase_request.approved")
        assertThat(saved.actionType).isEqualTo(WorkflowRuleActionType.NOTIFY_ROLE)
        assertThat(saved.actionTarget).isEqualTo("FINANCE")
        assertThat(saved.enabled).isTrue()
        assertThat(saved.organizationId).isEqualTo(orgId)
    }

    @Test
    fun `getRule 404s for a rule in a different org`() {
        whenever(repository.findById(java.util.UUID.fromString("00000000-0000-0000-0000-000000000600"))).thenReturn(
            Optional.of(rule(id = java.util.UUID.fromString("00000000-0000-0000-0000-000000000600"), organizationId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000205"))),
        )

        assertThatThrownBy { service.getRule(java.util.UUID.fromString("00000000-0000-0000-0000-000000000600"), orgId) }
            .isInstanceOf(ResourceNotFoundException::class.java)
    }

    @Test
    fun `updateRule only overwrites the fields that were sent`() {
        val existing =
            rule(
                id = java.util.UUID.fromString("00000000-0000-0000-0000-000000000600"),
                name = "Original",
                eventKind = "leave_request.approved",
                actionType = WorkflowRuleActionType.NOTIFY_USER,
                actionTarget = "00000000-0000-0000-0000-000000000101",
                enabled = true,
            )
        whenever(repository.findById(java.util.UUID.fromString("00000000-0000-0000-0000-000000000600"))).thenReturn(Optional.of(existing))

        service.updateRule(
            java.util.UUID.fromString("00000000-0000-0000-0000-000000000600"),
            orgId,
            UpdateWorkflowRuleRequest(enabled = false),
        )

        val captor = argumentCaptor<WorkflowRule>()
        verify(repository).save(captor.capture())
        val saved = captor.firstValue
        assertThat(saved.enabled).isFalse()
        assertThat(saved.name).isEqualTo("Original")
        assertThat(saved.eventKind).isEqualTo("leave_request.approved")
        assertThat(saved.actionType).isEqualTo(WorkflowRuleActionType.NOTIFY_USER)
        assertThat(saved.actionTarget).isEqualTo(java.util.UUID.fromString("00000000-0000-0000-0000-000000000101"))
    }

    @Test
    fun `findEnabledFor delegates to the indexed enabled-true query`() {
        whenever(
            repository.findByOrganizationIdAndEventKindAndEnabledTrue(orgId, "leave_request.approved"),
        ).thenReturn(listOf(rule(id = java.util.UUID.fromString("00000000-0000-0000-0000-000000000600"))))

        assertThat(service.findEnabledFor(orgId, "leave_request.approved")).hasSize(1)
    }

    private fun rule(
        id: java.util.UUID = java.util.UUID.randomUUID(),
        organizationId: java.util.UUID = orgId,
        name: String = "Rule",
        eventKind: String = "leave_request.approved",
        actionType: WorkflowRuleActionType = WorkflowRuleActionType.NOTIFY_USER,
        actionTarget: String = "00000000-0000-0000-0000-000000000101",
        enabled: Boolean = true,
    ): WorkflowRule =
        WorkflowRule(
            id = id,
            organizationId = organizationId,
            name = name,
            eventKind = eventKind,
            actionType = actionType,
            actionTarget = actionTarget,
            enabled = enabled,
        )
}
