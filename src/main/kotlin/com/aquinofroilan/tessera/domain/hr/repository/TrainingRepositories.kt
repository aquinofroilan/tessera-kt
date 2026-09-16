package com.aquinofroilan.tessera.domain.hr.repository

import com.aquinofroilan.tessera.domain.hr.model.Certification
import com.aquinofroilan.tessera.domain.hr.model.TrainingCourse
import com.aquinofroilan.tessera.domain.hr.model.TrainingRecord
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface TrainingCourseRepository : JpaRepository<TrainingCourse, java.util.UUID> {
    fun findByOrganizationId(organizationId: java.util.UUID): List<TrainingCourse>
}

@Repository
interface TrainingRecordRepository : JpaRepository<TrainingRecord, java.util.UUID> {
    fun findByOrganizationIdAndEmployeeId(
        organizationId: java.util.UUID,
        employeeId: java.util.UUID,
    ): List<TrainingRecord>
}

@Repository
interface CertificationRepository : JpaRepository<Certification, java.util.UUID> {
    fun findByOrganizationIdAndEmployeeId(
        organizationId: java.util.UUID,
        employeeId: java.util.UUID,
    ): List<Certification>

    fun findByOrganizationIdAndExpiryDateBetween(
        organizationId: java.util.UUID,
        start: LocalDate,
        end: LocalDate,
    ): List<Certification>
}
