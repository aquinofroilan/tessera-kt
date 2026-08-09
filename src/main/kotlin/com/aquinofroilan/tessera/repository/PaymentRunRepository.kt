package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.PaymentRun
import com.aquinofroilan.tessera.model.PaymentRunStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface PaymentRunRepository : JpaRepository<PaymentRun, String> {
    fun findByOrganizationId(organizationId: String): List<PaymentRun>

    fun findByOrganizationIdAndStatus(
        organizationId: String,
        status: PaymentRunStatus,
    ): List<PaymentRun>

    fun findByOrganizationIdAndCode(
        organizationId: String,
        code: String,
    ): Optional<PaymentRun>
}
