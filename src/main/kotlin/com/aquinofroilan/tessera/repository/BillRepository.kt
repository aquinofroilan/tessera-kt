package com.aquinofroilan.tessera.repository

import com.aquinofroilan.tessera.model.Bill
import com.aquinofroilan.tessera.model.BillStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface BillRepository : JpaRepository<Bill, java.util.UUID> {
    fun findByOrganizationId(organizationId: java.util.UUID): List<Bill>

    fun findByOrganizationIdAndStatus(
        organizationId: java.util.UUID,
        status: BillStatus,
    ): List<Bill>

    fun findByOrganizationIdAndVendorId(
        organizationId: java.util.UUID,
        vendorId: java.util.UUID,
    ): List<Bill>

    fun findByOrganizationIdAndStatusAndVendorId(
        organizationId: java.util.UUID,
        status: BillStatus,
        vendorId: java.util.UUID,
    ): List<Bill>

    fun findByOrganizationIdAndStatusIn(
        organizationId: java.util.UUID,
        statuses: List<BillStatus>,
    ): List<Bill>

    fun findByOrganizationIdAndDateBetween(
        organizationId: java.util.UUID,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<Bill>

    fun countByOrganizationId(organizationId: java.util.UUID): Long
}
