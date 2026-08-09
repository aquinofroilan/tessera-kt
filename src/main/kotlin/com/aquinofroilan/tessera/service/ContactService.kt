package com.aquinofroilan.tessera.service
import com.aquinofroilan.tessera.dto.CreateContactRequest
import com.aquinofroilan.tessera.dto.UpdateContactRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import com.aquinofroilan.tessera.model.Contact
import com.aquinofroilan.tessera.repository.ContactRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ContactService(
    private val contactRepository: ContactRepository,
    private val customerService: CustomerService,
) {
    @Transactional
    fun createContact(
        request: CreateContactRequest,
        organizationId: UUID,
        userId: UUID,
    ): Contact {
        val firstName = (request.firstName ?: "").trim()
        val lastName = (request.lastName ?: "").trim()
        if (firstName.isEmpty() && lastName.isEmpty()) {
            throw BusinessRuleException("At least one of firstName or lastName is required")
        }
        if (request.customerId != null) {
            customerService.getCustomer(request.customerId!!, organizationId)
        }
        return contactRepository.save(
            Contact(
                organizationId = organizationId,
                customerId = request.customerId,
                firstName = firstName,
                lastName = lastName,
                email = request.email?.trim(),
                phone = request.phone?.trim(),
                jobTitle = request.jobTitle,
                department = request.department,
                notes = request.notes,
                createdBy = userId,
            ),
        )
    }

    fun getContact(
        id: UUID,
        organizationId: UUID,
    ): Contact {
        val c =
            contactRepository.findById(id).orElseThrow {
                ResourceNotFoundException("Contact not found: $id")
            }
        if (c.organizationId != organizationId) {
            throw ResourceNotFoundException("Contact not found: $id")
        }
        return c
    }

    fun listContacts(
        organizationId: UUID,
        activeOnly: Boolean,
        customerId: UUID?,
    ): List<Contact> =
        when {
            customerId != null -> contactRepository.findByOrganizationIdAndCustomerId(organizationId, customerId)
            activeOnly -> contactRepository.findByOrganizationIdAndIsActive(organizationId, true)
            else -> contactRepository.findByOrganizationId(organizationId)
        }

    @Transactional
    fun updateContact(
        id: UUID,
        request: UpdateContactRequest,
        organizationId: UUID,
    ): Contact {
        val c = getContact(id, organizationId)
        if (request.customerId != null && request.customerId != c.customerId) {
            customerService.getCustomer(request.customerId!!, organizationId)
        }
        val firstName = request.firstName?.trim() ?: c.firstName
        val lastName = request.lastName?.trim() ?: c.lastName
        if (firstName.isEmpty() && lastName.isEmpty()) {
            throw BusinessRuleException("At least one of firstName or lastName is required")
        }
        return contactRepository.save(
            c.copy(
                customerId = request.customerId ?: c.customerId,
                firstName = firstName,
                lastName = lastName,
                email = request.email?.trim() ?: c.email,
                phone = request.phone?.trim() ?: c.phone,
                jobTitle = request.jobTitle ?: c.jobTitle,
                department = request.department ?: c.department,
                notes = request.notes ?: c.notes,
                isActive = request.isActive ?: c.isActive,
            ),
        )
    }

    @Transactional
    fun deactivateContact(
        id: UUID,
        organizationId: UUID,
    ): Contact {
        val c = getContact(id, organizationId)
        if (!c.isActive) {
            throw BusinessRuleException("Contact is already inactive")
        }
        return contactRepository.save(c.copy(isActive = false))
    }
}
