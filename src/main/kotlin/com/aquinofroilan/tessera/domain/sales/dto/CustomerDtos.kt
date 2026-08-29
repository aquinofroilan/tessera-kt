package com.aquinofroilan.tessera.domain.sales.dto

import com.aquinofroilan.tessera.domain.sales.model.CustomerSegment
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import java.util.UUID

data class CreateCustomerRequest(
    @field:NotBlank(message = "Customer name is required")
    val name: String,
    val contactName: String? = null,
    @field:Email(message = "Invalid email format")
    val contactEmail: String? = null,
    val contactPhone: String? = null,
    @field:Min(value = 0, message = "Payment term days must be zero or positive")
    val paymentTermDays: Int = 30,
    val defaultRevenueAccountId: UUID? = null,
    val customerSegment: CustomerSegment = CustomerSegment.RETAIL,
    val defaultPriceListId: UUID? = null,
)

data class UpdateCustomerRequest(
    val name: String? = null,
    val contactName: String? = null,
    @field:Email(message = "Invalid email format")
    val contactEmail: String? = null,
    val contactPhone: String? = null,
    @field:Min(value = 0, message = "Payment term days must be zero or positive")
    val paymentTermDays: Int? = null,
    val defaultRevenueAccountId: UUID? = null,
    val customerSegment: CustomerSegment? = null,
    val defaultPriceListId: UUID? = null,
)

data class CustomerResponse(
    val id: UUID,
    val name: String,
    val contactName: String?,
    val contactEmail: String?,
    val contactPhone: String?,
    val paymentTermDays: Int,
    val defaultRevenueAccountId: UUID?,
    val organizationId: UUID,
    val customerSegment: CustomerSegment,
    val defaultPriceListId: UUID?,
    val isActive: Boolean,
    val createdAt: String?,
    val updatedAt: String?,
)
