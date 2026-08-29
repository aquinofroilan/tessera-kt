package com.aquinofroilan.tessera.domain.sales.service

import com.aquinofroilan.tessera.domain.sales.dto.CreateDiscountRuleRequest
import com.aquinofroilan.tessera.domain.sales.dto.UpdateDiscountRuleRequest
import com.aquinofroilan.tessera.domain.sales.model.CustomerSegment
import com.aquinofroilan.tessera.domain.sales.model.DiscountRule
import com.aquinofroilan.tessera.domain.sales.model.DiscountType
import com.aquinofroilan.tessera.domain.sales.repository.DiscountRuleRepository
import com.aquinofroilan.tessera.exception.BusinessRuleException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import java.math.BigDecimal
import java.util.Optional
import java.util.UUID

class DiscountRuleServiceTest {
    private lateinit var discountRuleRepository: DiscountRuleRepository
    private lateinit var service: DiscountRuleService

    private val orgId = UUID.fromString("4abe9f6d-6df3-6e5c-953e-3695db9a5216")
    private val ruleId = UUID.fromString("33333333-4444-5555-6666-777777777777")

    @BeforeEach
    fun setUp() {
        discountRuleRepository = mock(DiscountRuleRepository::class.java)
        service = DiscountRuleService(discountRuleRepository)
    }

    private fun createRule() =
        DiscountRule(
            id = ruleId,
            organizationId = orgId,
            name = "VIP 10% Off",
            code = "DISC-VIP-10",
            discountType = DiscountType.PERCENTAGE,
            discountValue = BigDecimal("10.00"),
            customerSegment = CustomerSegment.VIP,
            priority = 10,
        )

    @Test
    fun `createDiscountRule creates rule successfully`() {
        val request =
            CreateDiscountRuleRequest(
                name = "VIP 10% Off",
                code = "DISC-VIP-10",
                discountType = DiscountType.PERCENTAGE,
                discountValue = BigDecimal("10.00"),
                customerSegment = CustomerSegment.VIP,
                priority = 10,
            )

        `when`(discountRuleRepository.existsByOrganizationIdAndCode(orgId, "DISC-VIP-10")).thenReturn(false)
        `when`(discountRuleRepository.save(any<DiscountRule>())).thenAnswer { it.arguments[0] }

        val response = service.createDiscountRule(orgId, request)

        assertEquals("VIP 10% Off", response.name)
        assertEquals("DISC-VIP-10", response.code)
        assertEquals(DiscountType.PERCENTAGE, response.discountType)
        assertEquals(BigDecimal("10.00"), response.discountValue)
        assertEquals(CustomerSegment.VIP, response.customerSegment)
        assertEquals(10, response.priority)
    }

    @Test
    fun `createDiscountRule throws BusinessRuleException on duplicate code`() {
        val request =
            CreateDiscountRuleRequest(
                name = "VIP 10% Off",
                code = "DISC-VIP-10",
                discountType = DiscountType.PERCENTAGE,
                discountValue = BigDecimal("10.00"),
            )

        `when`(discountRuleRepository.existsByOrganizationIdAndCode(orgId, "DISC-VIP-10")).thenReturn(true)

        assertThrows<BusinessRuleException> {
            service.createDiscountRule(orgId, request)
        }
    }

    @Test
    fun `updateDiscountRule updates fields`() {
        val rule = createRule()
        `when`(discountRuleRepository.findByIdAndOrganizationId(ruleId, orgId)).thenReturn(Optional.of(rule))
        `when`(discountRuleRepository.save(any<DiscountRule>())).thenAnswer { it.arguments[0] }

        val updateReq =
            UpdateDiscountRuleRequest(
                name = "VIP 15% Off",
                discountValue = BigDecimal("15.00"),
                isActive = false,
            )

        val response = service.updateDiscountRule(ruleId, orgId, updateReq)

        assertEquals("VIP 15% Off", response.name)
        assertEquals(BigDecimal("15.00"), response.discountValue)
        assertFalse(response.isActive)
    }
}
