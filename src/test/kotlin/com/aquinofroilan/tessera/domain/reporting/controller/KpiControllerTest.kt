package com.aquinofroilan.tessera.domain.reporting.controller

import com.aquinofroilan.tessera.aspect.LoggingAspect
import com.aquinofroilan.tessera.config.TestSecurityConfig
import com.aquinofroilan.tessera.config.WebMvcConfig
import com.aquinofroilan.tessera.domain.reporting.dto.KpiDefinitionRequest
import com.aquinofroilan.tessera.domain.reporting.dto.KpiStatus
import com.aquinofroilan.tessera.domain.reporting.dto.KpiTrendDataPoint
import com.aquinofroilan.tessera.domain.reporting.dto.KpiTrendResponse
import com.aquinofroilan.tessera.domain.reporting.dto.KpiValueRequest
import com.aquinofroilan.tessera.domain.reporting.model.KpiDefinition
import com.aquinofroilan.tessera.domain.reporting.model.KpiValue
import com.aquinofroilan.tessera.domain.reporting.model.MetricType
import com.aquinofroilan.tessera.domain.reporting.service.KpiService
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.security.TesseraPermissionEvaluator
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

@WebMvcTest(controllers = [KpiController::class])
@Import(WebMvcConfig::class, LoggingAspect::class, TestSecurityConfig::class, TesseraPermissionEvaluator::class)
@ActiveProfiles("test")
class KpiControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var kpiService: KpiService

    @MockitoBean
    private lateinit var authContext: AuthenticationContext

    @MockitoBean
    private lateinit var sessionTokenRepository: com.aquinofroilan.tessera.domain.auth.repository.SessionTokenRepository

    @MockitoBean
    private lateinit var userRepository: com.aquinofroilan.tessera.domain.auth.repository.UserRepository

    @MockitoBean
    private lateinit var apiKeyService: com.aquinofroilan.tessera.domain.auth.service.ApiKeyService

    @MockitoBean
    private lateinit var rolePermissionCache: com.aquinofroilan.tessera.security.RolePermissionCache

    private val orgId = UUID.randomUUID()
    private val kpiId = UUID.randomUUID()

    @Test
    @WithMockUser(authorities = ["reporting:read"])
    fun `listDefinitions should return 200`() {
        whenever(authContext.organizationId()).thenReturn(orgId)

        val def =
            KpiDefinition(
                id = kpiId,
                organizationId = orgId,
                name = "MRR",
                metricType = MetricType.CURRENCY,
                targetValue = BigDecimal("10000.00"),
            )
        whenever(kpiService.listDefinitions(orgId)).thenReturn(listOf(def))

        mockMvc
            .perform(get("/api/v1/reporting/kpis"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].name").value("MRR"))
    }

    @Test
    @WithMockUser(authorities = ["reporting:write"])
    fun `createDefinition should return 200`() {
        whenever(authContext.organizationId()).thenReturn(orgId)

        val request =
            KpiDefinitionRequest(
                name = "MRR",
                metricType = MetricType.CURRENCY,
                targetValue = BigDecimal("10000.00"),
            )

        val def =
            KpiDefinition(
                id = kpiId,
                organizationId = orgId,
                name = "MRR",
                metricType = MetricType.CURRENCY,
                targetValue = BigDecimal("10000.00"),
            )

        whenever(kpiService.createDefinition(eq(orgId), any())).thenReturn(def)

        mockMvc
            .perform(
                post("/api/v1/reporting/kpis")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("MRR"))
    }

    @Test
    @WithMockUser(authorities = ["reporting:write"])
    fun `recordValue should return 200`() {
        whenever(authContext.organizationId()).thenReturn(orgId)

        val request =
            KpiValueRequest(
                periodDate = LocalDate.parse("2026-09-01"),
                actualValue = BigDecimal("10500.00"),
            )

        val value =
            KpiValue(
                kpiId = kpiId,
                periodDate = request.periodDate,
                actualValue = request.actualValue,
            )

        whenever(kpiService.recordValue(eq(orgId), eq(kpiId), any())).thenReturn(value)

        mockMvc
            .perform(
                post("/api/v1/reporting/kpis/$kpiId/values")
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.actualValue").value(10500.0))
    }

    @Test
    @WithMockUser(authorities = ["reporting:read"])
    fun `getTrend should return 200`() {
        whenever(authContext.organizationId()).thenReturn(orgId)

        val response =
            KpiTrendResponse(
                kpiId = kpiId,
                name = "MRR",
                metricType = MetricType.CURRENCY,
                targetValue = BigDecimal("10000.00"),
                warningThreshold = BigDecimal("9000.00"),
                criticalThreshold = BigDecimal("8000.00"),
                higherIsBetter = true,
                values =
                    listOf(
                        KpiTrendDataPoint(
                            id = UUID.randomUUID(),
                            periodDate = LocalDate.parse("2026-09-01"),
                            actualValue = BigDecimal("10500.00"),
                            status = KpiStatus.ON_TRACK,
                        ),
                    ),
            )

        whenever(kpiService.getKpiTrend(eq(orgId), eq(kpiId), any(), any())).thenReturn(response)

        mockMvc
            .perform(
                get("/api/v1/reporting/kpis/$kpiId/trend")
                    .param("startDate", "2026-08-01")
                    .param("endDate", "2026-09-30"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("MRR"))
            .andExpect(jsonPath("$.values[0].status").value("ON_TRACK"))
    }
}
