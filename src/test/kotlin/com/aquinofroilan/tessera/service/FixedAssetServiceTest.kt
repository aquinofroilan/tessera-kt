package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.CreateFixedAssetRequest
import com.aquinofroilan.tessera.dto.UpdateFixedAssetRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import com.aquinofroilan.tessera.model.AssetCategory
import com.aquinofroilan.tessera.model.AssetStatus
import com.aquinofroilan.tessera.model.DepreciationMethod
import com.aquinofroilan.tessera.model.FixedAsset
import com.aquinofroilan.tessera.repository.FixedAssetRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.LocalDate
import java.util.Optional
import java.util.UUID

class FixedAssetServiceTest {
    private lateinit var repository: FixedAssetRepository
    private lateinit var categoryService: AssetCategoryService
    private lateinit var service: FixedAssetService

    private val orgId: UUID = UUID.randomUUID()

    @BeforeEach
    fun setup() {
        repository = mock(FixedAssetRepository::class.java)
        categoryService = mock(AssetCategoryService::class.java)
        whenever(repository.countByOrganizationId(orgId)).thenReturn(0L)
        whenever(repository.save(any<FixedAsset>())).thenAnswer { it.arguments[0] }
        service = FixedAssetService(repository, categoryService)
    }

    @Test
    fun `createAsset assigns a sequential FA number and persists the canonical shape`() {
        val saved =
            service.createAsset(
                CreateFixedAssetRequest(
                    name = "Lathe #1",
                    description = "Floor lathe",
                    acquisitionDate = LocalDate.of(2026, 1, 1),
                    acquisitionCost = BigDecimal("12000"),
                    salvageValue = BigDecimal("1000"),
                    usefulLifeMonths = 60,
                ),
                orgId,
            )

        val captor = argumentCaptor<FixedAsset>()
        verify(repository).save(captor.capture())
        assertThat(captor.firstValue.assetNumber).isEqualTo("FA-00001")
        assertThat(captor.firstValue.name).isEqualTo("Lathe #1")
        assertThat(captor.firstValue.depreciationMethod).isEqualTo(DepreciationMethod.STRAIGHT_LINE)
        assertThat(captor.firstValue.status).isEqualTo(AssetStatus.ACTIVE)
        assertThat(captor.firstValue.accumulatedDepreciation).isEqualByComparingTo(BigDecimal.ZERO)
        assertThat(saved.assetNumber).isEqualTo("FA-00001")
    }

    @Test
    fun `createAsset rejects salvage above cost`() {
        assertThatThrownBy {
            service.createAsset(
                CreateFixedAssetRequest(
                    name = "X",
                    acquisitionDate = LocalDate.of(2026, 1, 1),
                    acquisitionCost = BigDecimal("100"),
                    salvageValue = BigDecimal("200"),
                    usefulLifeMonths = 12,
                ),
                orgId,
            )
        }.isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `createAsset validates the referenced category lives in the same org`() {
        val catId = UUID.randomUUID()
        whenever(categoryService.getCategory(catId, orgId)).thenReturn(category(id = catId))

        service.createAsset(
            CreateFixedAssetRequest(
                name = "X",
                categoryId = catId.toString(),
                acquisitionDate = LocalDate.of(2026, 1, 1),
                acquisitionCost = BigDecimal("100"),
                usefulLifeMonths = 12,
            ),
            orgId,
        )

        verify(categoryService).getCategory(catId, orgId)
    }

    @Test
    fun `getAsset 404s for cross-org access`() {
        val assetId = UUID.randomUUID()
        whenever(repository.findById(assetId)).thenReturn(
            Optional.of(asset(id = assetId, organizationId = UUID.randomUUID())),
        )

        assertThatThrownBy { service.getAsset(assetId, orgId) }
            .isInstanceOf(ResourceNotFoundException::class.java)
    }

    @Test
    fun `updateAsset only overwrites fields that were sent`() {
        val assetId = UUID.randomUUID()
        val existing = asset(id = assetId, name = "Original", location = "Building A")
        whenever(repository.findById(assetId)).thenReturn(Optional.of(existing))

        service.updateAsset(
            assetId,
            UpdateFixedAssetRequest(name = "Renamed"),
            orgId,
        )

        val captor = argumentCaptor<FixedAsset>()
        verify(repository).save(captor.capture())
        assertThat(captor.firstValue.name).isEqualTo("Renamed")
        assertThat(captor.firstValue.location).isEqualTo("Building A")
    }

    private fun asset(
        id: UUID = UUID.randomUUID(),
        organizationId: UUID = orgId,
        name: String = "Asset",
        location: String? = null,
    ): FixedAsset =
        FixedAsset(
            id = id,
            organizationId = organizationId,
            assetNumber = "FA-00001",
            name = name,
            acquisitionDate = LocalDate.of(2026, 1, 1),
            acquisitionCost = BigDecimal("1000"),
            usefulLifeMonths = 12,
            location = location,
        )

    private fun category(id: UUID = UUID.randomUUID()): AssetCategory =
        AssetCategory(
            id = id,
            organizationId = orgId,
            code = "MACH",
            name = "Machinery",
        )
}
