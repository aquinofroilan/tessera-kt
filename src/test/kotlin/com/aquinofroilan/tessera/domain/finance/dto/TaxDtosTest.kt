package com.aquinofroilan.tessera.domain.finance.dto

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import tools.jackson.databind.ObjectMapper
import java.math.BigDecimal
import java.util.UUID

class TaxDtosTest {
    @Test
    fun `tax rate response serializes active property as isActive`() {
        val response =
            TaxRateResponse(
                id = UUID.fromString("00000000-0000-0000-0000-000000000001"),
                name = "Sales Tax",
                code = "SALE",
                percentage = BigDecimal("8.00"),
                authority = "State",
                organizationId = UUID.fromString("00000000-0000-0000-0000-000000000002"),
                isActive = true,
                createdAt = null,
                updatedAt = null,
            )

        val json = ObjectMapper().writeValueAsString(response)

        assertThat(json).contains("\"isActive\":true")
        assertThat(json).doesNotContain("\"active\":true")
    }
}
