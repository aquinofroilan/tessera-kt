package com.aquinofroilan.tessera.domain.inventory.dto

import java.math.BigDecimal
import java.util.UUID

data class PlaceHoldRequest(
    val productId: UUID,
    val warehouseId: UUID,
    val lotNumber: String? = null,
    val serialNumbers: List<String>? = null,
    val quantity: BigDecimal? = null,
    val reference: String? = null,
    val notes: String? = null,
)
