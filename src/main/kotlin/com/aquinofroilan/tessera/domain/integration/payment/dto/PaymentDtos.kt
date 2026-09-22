package com.aquinofroilan.tessera.domain.integration.payment.dto

import com.aquinofroilan.tessera.domain.integration.payment.model.GatewayType
import jakarta.validation.constraints.NotNull
import java.util.UUID

data class PaymentCheckoutRequest(
    @field:NotNull
    val invoiceId: UUID?,
    @field:NotNull
    val gateway: GatewayType?,
)

data class PaymentCheckoutResponse(
    val paymentUrl: String,
    val intentId: String,
)
