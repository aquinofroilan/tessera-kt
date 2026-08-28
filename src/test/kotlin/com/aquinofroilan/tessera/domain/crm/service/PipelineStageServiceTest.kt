package com.aquinofroilan.tessera.domain.crm.service

import com.aquinofroilan.tessera.domain.crm.dto.CreatePipelineStageRequest
import com.aquinofroilan.tessera.domain.crm.dto.UpdatePipelineStageRequest
import com.aquinofroilan.tessera.domain.crm.model.PipelineStage
import com.aquinofroilan.tessera.domain.crm.repository.PipelineStageRepository
import com.aquinofroilan.tessera.exception.BusinessRuleException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.util.Optional
import java.util.UUID

class PipelineStageServiceTest {
    private lateinit var repository: PipelineStageRepository
    private lateinit var service: PipelineStageService

    private val orgId = UUID.randomUUID()

    @BeforeEach
    fun setup() {
        repository = mock(PipelineStageRepository::class.java)
        whenever(repository.save(any<PipelineStage>())).thenAnswer { it.arguments[0] }
        whenever(repository.findByOrganizationIdAndCode(any(), any())).thenReturn(Optional.empty())
        whenever(repository.findByOrganizationIdOrderBySortOrderAsc(any())).thenReturn(emptyList())
        service = PipelineStageService(repository)
    }

    @Test
    fun `create normalises code and auto-increments sort order by 10`() {
        whenever(repository.findByOrganizationIdOrderBySortOrderAsc(orgId)).thenReturn(
            listOf(stage("PROSPECT", 10), stage("QUALIFIED", 20)),
        )
        val s =
            service.createStage(
                CreatePipelineStageRequest(code = " proposal ", name = " Proposal "),
                orgId,
            )
        assertThat(s.code).isEqualTo("PROPOSAL")
        assertThat(s.sortOrder).isEqualTo(30)
    }

    @Test
    fun `create rejects duplicate code`() {
        whenever(repository.findByOrganizationIdAndCode(orgId, "WON")).thenReturn(Optional.of(stage("WON", 100)))
        assertThatThrownBy {
            service.createStage(CreatePipelineStageRequest(code = "WON", name = "Won"), orgId)
        }.isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `create rejects a stage flagged as both won and lost`() {
        assertThatThrownBy {
            service.createStage(
                CreatePipelineStageRequest(code = "C", name = "C", isWon = true, isLost = true),
                orgId,
            )
        }.isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `update rejects setting both won and lost`() {
        val s1Id = UUID.randomUUID()
        whenever(repository.findById(s1Id)).thenReturn(Optional.of(stage("S", 10).copy(id = s1Id, isWon = true)))
        assertThatThrownBy {
            service.updateStage(s1Id, UpdatePipelineStageRequest(isLost = true), orgId)
        }.isInstanceOf(BusinessRuleException::class.java)
    }

    private fun stage(
        code: String,
        sortOrder: Int,
    ) = PipelineStage(
        organizationId = orgId,
        code = code,
        name = code,
        sortOrder = sortOrder,
        probabilityPct = BigDecimal.ZERO,
    )
}
