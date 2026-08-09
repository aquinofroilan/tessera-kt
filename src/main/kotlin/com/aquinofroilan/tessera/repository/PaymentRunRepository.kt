package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.PaymentRun
import com.aquinofroilan.tessera.model.PaymentRunStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface PaymentRunRepository : JpaRepository<PaymentRun, java.util.UUID> {
    fun findByOrganizationId(organizationId: java.util.UUID): List<PaymentRun>

    fun findByOrganizationIdAndStatus(
        organizationId: java.util.UUID,
        status: PaymentRunStatus,
    ): List<PaymentRun>

    fun findByOrganizationIdAndCode(
        organizationId: java.util.UUID,
        code: String,
    ): Optional<PaymentRun>
}
