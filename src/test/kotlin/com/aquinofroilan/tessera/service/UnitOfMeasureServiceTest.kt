package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.CreateUomRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.model.UnitOfMeasure
import com.aquinofroilan.tessera.repository.UnitOfMeasureRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.util.Optional

class UnitOfMeasureServiceTest {
    private lateinit var repository: UnitOfMeasureRepository
    private lateinit var service: UnitOfMeasureService

    private val orgId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000001")

    @BeforeEach
    fun setup() {
        repository = mock(UnitOfMeasureRepository::class.java)
        whenever(repository.save(any<UnitOfMeasure>())).thenAnswer { it.arguments[0] }
        whenever(repository.findByOrganizationIdAndCode(any(), any())).thenReturn(Optional.empty())
        service = UnitOfMeasureService(repository)
    }

    @Test
    fun `create base UoM normalises code and defaults factor to 1`() {
        val u = service.createUom(CreateUomRequest(code = " ea ", name = "Each"), orgId)
        assertThat(u.code).isEqualTo("EA")
        assertThat(u.conversionFactor).isEqualByComparingTo(BigDecimal.ONE)
        assertThat(u.baseUomId).isNull()
    }

    @Test
    fun `create non-base requires a real base UoM`() {
        whenever(repository.findById(java.util.UUID.fromString("00000000-0000-0000-0000-000000000002"))).thenReturn(Optional.of(baseUom("KG", java.util.UUID.fromString("00000000-0000-0000-0000-000000000002"))))
        val u =
            service.createUom(
                CreateUomRequest(code = "G", name = "Gram", baseUomId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000002"), conversionFactor = BigDecimal("0.001")),
                orgId,
            )
        assertThat(u.baseUomId).isEqualTo(java.util.UUID.fromString("00000000-0000-0000-0000-000000000002"))
        assertThat(u.conversionFactor).isEqualByComparingTo(BigDecimal("0.001"))
    }

    @Test
    fun `create rejects chained conversion (base must itself be a base)`() {
        whenever(repository.findById(java.util.UUID.fromString("00000000-0000-0000-0000-000000000003"))).thenReturn(Optional.of(baseUom("G", java.util.UUID.fromString("00000000-0000-0000-0000-000000000003")).copy(baseUomId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000004"))))
        assertThatThrownBy {
            service.createUom(
                CreateUomRequest(code = "MG", name = "Milligram", baseUomId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000003")),
                orgId,
            )
        }.isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `create rejects a base UoM with a non-1 conversion factor`() {
        assertThatThrownBy {
            service.createUom(CreateUomRequest(code = "KG", name = "Kilogram", conversionFactor = BigDecimal("2")), orgId)
        }.isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `convert between same-base units works`() {
        val kg = baseUom("KG", java.util.UUID.fromString("00000000-0000-0000-0000-000000000004"))
        val g = baseUom("G", java.util.UUID.fromString("00000000-0000-0000-0000-000000000012")).copy(baseUomId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000004"), conversionFactor = BigDecimal("0.001"))
        whenever(repository.findById(java.util.UUID.fromString("00000000-0000-0000-0000-000000000004"))).thenReturn(Optional.of(kg))
        whenever(repository.findById(java.util.UUID.fromString("00000000-0000-0000-0000-000000000012"))).thenReturn(Optional.of(g))
        val result = service.convert(BigDecimal("2"), java.util.UUID.fromString("00000000-0000-0000-0000-000000000004"), java.util.UUID.fromString("00000000-0000-0000-0000-000000000012"), orgId)
        assertThat(result).isEqualByComparingTo(BigDecimal("2000"))
    }

    @Test
    fun `convert rejects cross-base UoMs`() {
        val kg = baseUom("KG", java.util.UUID.fromString("00000000-0000-0000-0000-000000000004"))
        val ea = baseUom("EA", java.util.UUID.fromString("00000000-0000-0000-0000-000000000013"))
        whenever(repository.findById(java.util.UUID.fromString("00000000-0000-0000-0000-000000000004"))).thenReturn(Optional.of(kg))
        whenever(repository.findById(java.util.UUID.fromString("00000000-0000-0000-0000-000000000013"))).thenReturn(Optional.of(ea))
        assertThatThrownBy { service.convert(BigDecimal.ONE, java.util.UUID.fromString("00000000-0000-0000-0000-000000000004"), java.util.UUID.fromString("00000000-0000-0000-0000-000000000013"), orgId) }
            .isInstanceOf(BusinessRuleException::class.java)
    }

    private fun baseUom(
        code: String,
        id: java.util.UUID,
    ) = UnitOfMeasure(
        id = id,
        organizationId = orgId,
        code = code,
        name = code,
        conversionFactor = BigDecimal.ONE,
    )
}
