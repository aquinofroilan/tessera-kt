package com.aquinofroilan.tessera.domain.hr.service

import com.aquinofroilan.tessera.domain.hr.dto.CreateTrainingCourseRequest
import com.aquinofroilan.tessera.domain.hr.dto.RecordCertificationRequest
import com.aquinofroilan.tessera.domain.hr.dto.RecordTrainingRequest
import com.aquinofroilan.tessera.domain.hr.model.Certification
import com.aquinofroilan.tessera.domain.hr.model.Employee
import com.aquinofroilan.tessera.domain.hr.model.EmploymentStatus
import com.aquinofroilan.tessera.domain.hr.model.TrainingCourse
import com.aquinofroilan.tessera.domain.hr.model.TrainingRecord
import com.aquinofroilan.tessera.domain.hr.repository.CertificationRepository
import com.aquinofroilan.tessera.domain.hr.repository.TrainingCourseRepository
import com.aquinofroilan.tessera.domain.hr.repository.TrainingRecordRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

class TrainingServiceTest {
    private val trainingCourseRepository: TrainingCourseRepository = mock()
    private val trainingRecordRepository: TrainingRecordRepository = mock()
    private val certificationRepository: CertificationRepository = mock()
    private val employeeService: EmployeeService = mock()

    private val trainingService =
        TrainingService(
            trainingCourseRepository,
            trainingRecordRepository,
            certificationRepository,
            employeeService,
        )

    private val orgId = UUID.randomUUID()
    private val employeeId = UUID.randomUUID()
    private val courseId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
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

        val mockCourse =
            TrainingCourse(
                id = courseId,
                organizationId = orgId,
                name = "Safety Training",
            )
        whenever(trainingCourseRepository.findById(courseId)).thenReturn(Optional.of(mockCourse))

        whenever(trainingCourseRepository.save(any<TrainingCourse>())).thenAnswer { it.arguments[0] }
        whenever(trainingRecordRepository.save(any<TrainingRecord>())).thenAnswer { it.arguments[0] }
        whenever(certificationRepository.save(any<Certification>())).thenAnswer { it.arguments[0] }
    }

    @Test
    fun `createCourse creates a new course`() {
        val request =
            CreateTrainingCourseRequest(
                name = "Security 101",
                provider = "Internal",
            )
        val course = trainingService.createCourse(request, orgId)
        assertThat(course.name).isEqualTo("Security 101")
        assertThat(course.provider).isEqualTo("Internal")
    }

    @Test
    fun `recordTraining records employee completion`() {
        val request =
            RecordTrainingRequest(
                employeeId = employeeId,
                courseId = courseId,
                completionDate = LocalDate.of(2026, 1, 15),
            )
        val record = trainingService.recordTraining(request, orgId)
        assertThat(record.courseId).isEqualTo(courseId)
        assertThat(record.completionDate).isEqualTo(LocalDate.of(2026, 1, 15))
    }

    @Test
    fun `recordCertification records an employee certification`() {
        val request =
            RecordCertificationRequest(
                employeeId = employeeId,
                name = "AWS Certified Developer",
                issuingBody = "Amazon",
                issueDate = LocalDate.of(2026, 2, 1),
                expiryDate = LocalDate.of(2029, 2, 1),
            )
        val cert = trainingService.recordCertification(request, orgId)
        assertThat(cert.name).isEqualTo("AWS Certified Developer")
        assertThat(cert.issuingBody).isEqualTo("Amazon")
        assertThat(cert.expiryDate).isEqualTo(LocalDate.of(2029, 2, 1))
    }

    @Test
    fun `getUpcomingCertificationExpiries fetches soon to expire certs`() {
        val cert =
            Certification(
                organizationId = orgId,
                employeeId = employeeId,
                name = "CPR",
                issueDate = LocalDate.now().minusYears(2),
                expiryDate = LocalDate.now().plusDays(10),
            )
        whenever(certificationRepository.findByOrganizationIdAndExpiryDateBetween(eq(orgId), any(), any()))
            .thenReturn(listOf(cert))

        val expiries = trainingService.getUpcomingCertificationExpiries(orgId, 30)
        assertThat(expiries).hasSize(1)
        assertThat(expiries[0].name).isEqualTo("CPR")
    }
}
