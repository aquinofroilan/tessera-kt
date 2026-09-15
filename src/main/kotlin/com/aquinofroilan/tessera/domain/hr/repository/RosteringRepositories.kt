package com.aquinofroilan.tessera.domain.hr.repository

import com.aquinofroilan.tessera.domain.hr.model.Roster
import com.aquinofroilan.tessera.domain.hr.model.Shift
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ShiftRepository : JpaRepository<Shift, java.util.UUID> {
    fun findByOrganizationId(organizationId: java.util.UUID): List<Shift>
}

@Repository
interface RosterRepository : JpaRepository<Roster, java.util.UUID> {
    fun findByOrganizationId(organizationId: java.util.UUID): List<Roster>
}
