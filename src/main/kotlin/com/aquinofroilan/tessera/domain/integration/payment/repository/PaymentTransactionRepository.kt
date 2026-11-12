package com.aquinofroilan.tessera.domain.integration.payment.repository

import com.aquinofroilan.tessera.domain.integration.payment.model.PaymentTransaction
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface PaymentTransactionRepository : JpaRepository<PaymentTransaction, UUID> {
    fun findByGatewayIntentId(gatewayIntentId: String): PaymentTransaction?
}
