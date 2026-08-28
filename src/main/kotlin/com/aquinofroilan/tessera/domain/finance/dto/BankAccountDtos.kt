package com.aquinofroilan.tessera.domain.finance.dto

import com.aquinofroilan.tessera.domain.finance.model.BankAccount
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import java.math.BigDecimal

data class CreateBankAccountRequest(
    @field:NotBlank(message = "Code is required")
    @field:Size(max = 64)
    val code: String,
    @field:NotBlank(message = "Name is required")
    val name: String,
    val bankName: String? = null,
    @field:Size(min = 4, max = 4, message = "Account number last4 must be exactly 4 characters")
    val accountNumberLast4: String? = null,
    val currency: String? = null,
    @field:NotBlank(message = "GL account ID is required")
    val glAccountId: java.util.UUID,
    @field:PositiveOrZero(message = "Opening balance cannot be negative")
    val openingBalance: BigDecimal? = null,
    val notes: String? = null,
)

data class UpdateBankAccountRequest(
    val name: String? = null,
    val bankName: String? = null,
    @field:Size(min = 4, max = 4)
    val accountNumberLast4: String? = null,
    val isActive: Boolean? = null,
    val notes: String? = null,
)

data class BankAccountResponse(
    val id: java.util.UUID,
    val code: String,
    val name: String,
    val bankName: String?,
    val accountNumberLast4: String?,
    val currency: String,
    val glAccountId: java.util.UUID,
    val openingBalance: BigDecimal,
    val currentBalance: BigDecimal,
    val isActive: Boolean,
    val notes: String?,
) {
    companion object {
        fun from(b: BankAccount) =
            BankAccountResponse(
                id = b.id,
                code = b.code,
                name = b.name,
                bankName = b.bankName,
                accountNumberLast4 = b.accountNumberLast4,
                currency = b.currency,
                glAccountId = b.glAccountId,
                openingBalance = b.openingBalance,
                currentBalance = b.currentBalance,
                isActive = b.isActive,
                notes = b.notes,
            )
    }
}
