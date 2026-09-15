package com.aquinofroilan.tessera.domain.hr.service

import com.aquinofroilan.tessera.domain.hr.dto.CreateTrainingCourseRequest
import com.aquinofroilan.tessera.domain.hr.dto.RecordCertificationRequest
import com.aquinofroilan.tessera.domain.hr.dto.RecordTrainingRequest
import com.aquinofroilan.tessera.domain.hr.model.Certification
import com.aquinofroilan.tessera.domain.hr.model.TrainingCourse
import com.aquinofroilan.tessera.domain.hr.model.TrainingRecord
import com.aquinofroilan.tessera.domain.hr.repository.CertificationRepository
import com.aquinofroilan.tessera.domain.hr.repository.TrainingCourseRepository
import com.aquinofroilan.tessera.domain.hr.repository.TrainingRecordRepository
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class TrainingService(
    private val trainingCourseRepository: TrainingCourseRepository,
    private val trainingRecordRepository: TrainingRecordRepository,
    private val certificationRepository: CertificationRepository,
    private val employeeService: EmployeeService,
) {
    @Transactional
    fun createCourse(
        request: CreateTrainingCourseRequest,
        organizationId: UUID,
    ): TrainingCourse {
        val course =
            TrainingCourse(
                organizationId = organizationId,
                name = request.name ?: throw BusinessRuleException("Name is required"),
                description = request.description,
                provider = request.provider,
            )
        return trainingCourseRepository.save(course)
    }

    fun getCourse(
        courseId: UUID,
        organizationId: UUID,
    ): TrainingCourse {
        val course =
            trainingCourseRepository.findById(courseId).orElseThrow {
                ResourceNotFoundException("Course not found")
            }
        if (course.organizationId != organizationId) {
            throw ResourceNotFoundException("Course not found")
        }
        return course
    }

    @Transactional
    fun recordTraining(
        request: RecordTrainingRequest,
        organizationId: UUID,
    ): TrainingRecord {
        val employeeId = request.employeeId ?: throw BusinessRuleException("Employee ID is required")
        val courseId = request.courseId ?: throw BusinessRuleException("Course ID is required")
        val completionDate = request.completionDate ?: throw BusinessRuleException("Completion date is required")

        employeeService.getEmployee(employeeId, organizationId)
        getCourse(courseId, organizationId)

        val record =
            TrainingRecord(
                organizationId = organizationId,
                employeeId = employeeId,
                courseId = courseId,
                completionDate = completionDate,
                attachmentId = request.attachmentId,
            )
        return trainingRecordRepository.save(record)
    }

    fun getEmployeeTrainingRecords(
        employeeId: UUID,
        organizationId: UUID,
    ): List<TrainingRecord> {
        employeeService.getEmployee(employeeId, organizationId)
        return trainingRecordRepository.findByOrganizationIdAndEmployeeId(organizationId, employeeId)
    }

    @Transactional
    fun recordCertification(
        request: RecordCertificationRequest,
        organizationId: UUID,
    ): Certification {
        val employeeId = request.employeeId ?: throw BusinessRuleException("Employee ID is required")
        val issueDate = request.issueDate ?: throw BusinessRuleException("Issue date is required")

        employeeService.getEmployee(employeeId, organizationId)

        val certification =
            Certification(
                organizationId = organizationId,
                employeeId = employeeId,
                name = request.name ?: throw BusinessRuleException("Name is required"),
                issuingBody = request.issuingBody,
                issueDate = issueDate,
                expiryDate = request.expiryDate,
                credentialId = request.credentialId,
                attachmentId = request.attachmentId,
            )
        return certificationRepository.save(certification)
    }

    fun getUpcomingCertificationExpiries(
        organizationId: UUID,
        daysAhead: Long = 30,
    ): List<Certification> {
        val today = LocalDate.now()
        val endDate = today.plusDays(daysAhead)
        return certificationRepository.findByOrganizationIdAndExpiryDateBetween(organizationId, today, endDate)
    }
}
