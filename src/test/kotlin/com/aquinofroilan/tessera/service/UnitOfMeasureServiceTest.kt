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

    private val orgId = "org-1"

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
        whenever(repository.findById("base")).thenReturn(Optional.of(baseUom("KG", "base")))
        val u =
            service.createUom(
                CreateUomRequest(code = "G", name = "Gram", baseUomId = "base", conversionFactor = BigDecimal("0.001")),
                orgId,
            )
        assertThat(u.baseUomId).isEqualTo("base")
        assertThat(u.conversionFactor).isEqualByComparingTo(BigDecimal("0.001"))
    }

    @Test
    fun `create rejects chained conversion (base must itself be a base)`() {
        whenever(repository.findById("not-a-base")).thenReturn(Optional.of(baseUom("G", "not-a-base").copy(baseUomId = "kg")))
        assertThatThrownBy {
            service.createUom(
                CreateUomRequest(code = "MG", name = "Milligram", baseUomId = "not-a-base"),
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
        val kg = baseUom("KG", "kg")
        val g = baseUom("G", "g").copy(baseUomId = "kg", conversionFactor = BigDecimal("0.001"))
        whenever(repository.findById("kg")).thenReturn(Optional.of(kg))
        whenever(repository.findById("g")).thenReturn(Optional.of(g))
        val result = service.convert(BigDecimal("2"), "kg", "g", orgId)
        assertThat(result).isEqualByComparingTo(BigDecimal("2000"))
    }

    @Test
    fun `convert rejects cross-base UoMs`() {
        val kg = baseUom("KG", "kg")
        val ea = baseUom("EA", "ea")
        whenever(repository.findById("kg")).thenReturn(Optional.of(kg))
        whenever(repository.findById("ea")).thenReturn(Optional.of(ea))
        assertThatThrownBy { service.convert(BigDecimal.ONE, "kg", "ea", orgId) }
            .isInstanceOf(BusinessRuleException::class.java)
    }

    private fun baseUom(
        code: String,
        id: String,
    ) = UnitOfMeasure(
        id = id,
        organizationId = orgId,
        code = code,
        name = code,
        conversionFactor = BigDecimal.ONE,
    )
}
