package com.aquinofroilan.tessera.domain.hr.service

import com.aquinofroilan.tessera.domain.hr.dto.CreateAppraisalCycleRequest
import com.aquinofroilan.tessera.domain.hr.dto.CreateAppraisalGoalRequest
import com.aquinofroilan.tessera.domain.hr.dto.CreateAppraisalRequest
import com.aquinofroilan.tessera.domain.hr.dto.SubmitAppraisalReviewRequest
import com.aquinofroilan.tessera.domain.hr.dto.UpdateAppraisalGoalRequest
import com.aquinofroilan.tessera.domain.hr.model.Appraisal
import com.aquinofroilan.tessera.domain.hr.model.AppraisalCycle
import com.aquinofroilan.tessera.domain.hr.model.AppraisalCycleStatus
import com.aquinofroilan.tessera.domain.hr.model.AppraisalGoal
import com.aquinofroilan.tessera.domain.hr.model.AppraisalStatus
import com.aquinofroilan.tessera.domain.hr.model.Employee
import com.aquinofroilan.tessera.domain.hr.model.EmploymentStatus
import com.aquinofroilan.tessera.domain.hr.repository.AppraisalCycleRepository
import com.aquinofroilan.tessera.domain.hr.repository.AppraisalRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

class AppraisalServiceTest {
    private val appraisalCycleRepository: AppraisalCycleRepository = mock()
    private val appraisalRepository: AppraisalRepository = mock()
    private val employeeService: EmployeeService = mock()

    private val appraisalService =
        AppraisalService(
            appraisalCycleRepository,
            appraisalRepository,
            employeeService,
        )

    private val orgId = UUID.randomUUID()
    private val cycleId = UUID.randomUUID()
    private val employeeId = UUID.randomUUID()
    private val managerId = UUID.randomUUID()
    private val appraisalId = UUID.randomUUID()
    private val goalId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        val mockCycle =
            AppraisalCycle(
                id = cycleId,
                organizationId = orgId,
                name = "2026 Q1",
                startDate = LocalDate.now(),
                endDate = LocalDate.now().plusMonths(3),
                status = AppraisalCycleStatus.DRAFT,
            )
        whenever(appraisalCycleRepository.findById(cycleId)).thenReturn(Optional.of(mockCycle))
        whenever(appraisalCycleRepository.save(any<AppraisalCycle>())).thenAnswer { it.arguments[0] }

        val mockEmployee =
            Employee(
                id = employeeId,
                employeeNumber = "E001",
                firstName = "John",
                lastName = "Doe",
                hireDate = LocalDate.now(),
                organizationId = orgId,
                status = EmploymentStatus.ACTIVE,
            )
        whenever(employeeService.getEmployee(employeeId, orgId)).thenReturn(mockEmployee)

        val mockAppraisal =
            Appraisal(
                id = appraisalId,
                organizationId = orgId,
                cycleId = cycleId,
                employeeId = employeeId,
                managerId = managerId,
                status = AppraisalStatus.DRAFT,
            )
        val mockGoal =
            AppraisalGoal(
                id = goalId,
                appraisalId = appraisalId,
                title = "Goal 1",
                weight = 50,
            )
        mockAppraisal.goals.add(mockGoal)
        whenever(appraisalRepository.findById(appraisalId)).thenReturn(Optional.of(mockAppraisal))
        whenever(appraisalRepository.save(any<Appraisal>())).thenAnswer { it.arguments[0] }
    }

    @Test
    fun `createCycle creates a new draft cycle`() {
        val request =
            CreateAppraisalCycleRequest(
                name = "2026 Q2",
                startDate = LocalDate.now(),
                endDate = LocalDate.now().plusMonths(3),
            )
        val cycle = appraisalService.createCycle(request, orgId)
        assertThat(cycle.name).isEqualTo("2026 Q2")
        assertThat(cycle.status).isEqualTo(AppraisalCycleStatus.DRAFT)
    }

    @Test
    fun `activateCycle changes status to ACTIVE`() {
        val cycle = appraisalService.activateCycle(cycleId, orgId)
        assertThat(cycle.status).isEqualTo(AppraisalCycleStatus.ACTIVE)
    }

    @Test
    fun `createAppraisal adds goals and saves in DRAFT`() {
        whenever(appraisalCycleRepository.findById(cycleId)).thenReturn(
            Optional.of(
                AppraisalCycle(
                    id = cycleId,
                    organizationId = orgId,
                    name = "2026 Q1",
                    startDate = LocalDate.now(),
                    endDate = LocalDate.now().plusMonths(3),
                    status = AppraisalCycleStatus.ACTIVE,
                ),
            ),
        )

        val request =
            CreateAppraisalRequest(
                employeeId = employeeId,
                managerId = managerId,
                goals =
                    listOf(
                        CreateAppraisalGoalRequest(title = "Improve Sales", weight = 60),
                        CreateAppraisalGoalRequest(title = "Mentorship", weight = 40),
                    ),
            )
        val appraisal = appraisalService.createAppraisal(cycleId, request, orgId)
        assertThat(appraisal.status).isEqualTo(AppraisalStatus.DRAFT)
        assertThat(appraisal.goals).hasSize(2)
        assertThat(appraisal.goals[0].title).isEqualTo("Improve Sales")
    }

    @Test
    fun `submitSelfReview updates self ratings and changes status`() {
        val request =
            SubmitAppraisalReviewRequest(
                goals =
                    listOf(
                        UpdateAppraisalGoalRequest(
                            goalId = goalId,
                            selfRating = 4,
                            selfComments = "Did well",
                        ),
                    ),
            )
        val appraisal = appraisalService.submitSelfReview(appraisalId, request, orgId)
        assertThat(appraisal.status).isEqualTo(AppraisalStatus.MANAGER_REVIEW)
        assertThat(appraisal.goals[0].selfRating).isEqualTo(4)
        assertThat(appraisal.goals[0].selfComments).isEqualTo("Did well")
    }

    @Test
    fun `submitManagerReview updates overall rating and completes`() {
        val mockAppraisal =
            Appraisal(
                id = appraisalId,
                organizationId = orgId,
                cycleId = cycleId,
                employeeId = employeeId,
                managerId = managerId,
                status = AppraisalStatus.MANAGER_REVIEW,
            )
        mockAppraisal.goals.add(AppraisalGoal(id = goalId, appraisalId = appraisalId, title = "Goal 1"))
        whenever(appraisalRepository.findById(appraisalId)).thenReturn(Optional.of(mockAppraisal))

        val request =
            SubmitAppraisalReviewRequest(
                goals =
                    listOf(
                        UpdateAppraisalGoalRequest(
                            goalId = goalId,
                            managerRating = 5,
                            managerComments = "Excellent",
                        ),
                    ),
                overallRating = 4.5,
                managerSummary = "Great year",
            )
        val appraisal = appraisalService.submitManagerReview(appraisalId, request, orgId)
        assertThat(appraisal.status).isEqualTo(AppraisalStatus.COMPLETED)
        assertThat(appraisal.goals[0].managerRating).isEqualTo(5)
        assertThat(appraisal.overallRating).isEqualTo(4.5)
    }
}
