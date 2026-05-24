package com.froilan.synectix.repository

import com.froilan.synectix.model.Bill
import com.froilan.synectix.model.BillStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
interface BillRepository : JpaRepository<Bill, String> {
    fun findByOrganizationId(organizationId: String): List<Bill>

    fun findByOrganizationIdAndStatus(
        organizationId: String,
        status: BillStatus,
    ): List<Bill>

    fun findByOrganizationIdAndVendorId(
        organizationId: String,
        vendorId: String,
    ): List<Bill>

    fun findByOrganizationIdAndStatusAndVendorId(
        organizationId: String,
        status: BillStatus,
        vendorId: String,
    ): List<Bill>

    fun findByOrganizationIdAndStatusIn(
        organizationId: String,
        statuses: List<BillStatus>,
    ): List<Bill>

    fun findByOrganizationIdAndDateBetween(
        organizationId: String,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<Bill>

    fun countByOrganizationId(organizationId: String): Long
}
