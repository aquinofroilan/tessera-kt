package com.aquinofroilan.tessera.domain.crm.dto

import com.aquinofroilan.tessera.domain.crm.model.Contact
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Size
import java.util.UUID

data class CreateContactRequest(
    val customerId: UUID? = null,
    @field:Size(max = 128)
    val firstName: String?,
    @field:Size(max = 128)
    val lastName: String?,
    @field:Email
    val email: String? = null,
    val phone: String? = null,
    val jobTitle: String? = null,
    val department: String? = null,
    val notes: String? = null,
)

data class UpdateContactRequest(
    val customerId: UUID? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    @field:Email
    val email: String? = null,
    val phone: String? = null,
    val jobTitle: String? = null,
    val department: String? = null,
    val notes: String? = null,
    val isActive: Boolean? = null,
)

data class ContactResponse(
    val id: UUID,
    val customerId: UUID?,
    val firstName: String,
    val lastName: String,
    val email: String?,
    val phone: String?,
    val jobTitle: String?,
    val department: String?,
    val notes: String?,
    val isActive: Boolean,
) {
    companion object {
        fun from(c: Contact) =
            ContactResponse(
                id = c.id,
                customerId = c.customerId,
                firstName = c.firstName,
                lastName = c.lastName,
                email = c.email,
                phone = c.phone,
                jobTitle = c.jobTitle,
                department = c.department,
                notes = c.notes,
                isActive = c.isActive,
            )
    }
}
