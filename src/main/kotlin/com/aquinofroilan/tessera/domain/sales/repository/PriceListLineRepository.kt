package com.aquinofroilan.tessera.domain.sales.repository

import com.aquinofroilan.tessera.domain.sales.model.PriceListLine
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.util.Optional
import java.util.UUID

@Repository
interface PriceListLineRepository : JpaRepository<PriceListLine, UUID> {
    fun findByPriceListId(priceListId: UUID): List<PriceListLine>

    fun findByPriceListIdAndProductId(
        priceListId: UUID,
        productId: UUID,
    ): List<PriceListLine>

    fun findByPriceListIdAndProductIdAndMinQuantityLessThanEqualOrderByMinQuantityDesc(
        priceListId: UUID,
        productId: UUID,
        quantity: BigDecimal,
    ): List<PriceListLine>

    fun findByPriceListIdAndProductIdAndMinQuantity(
        priceListId: UUID,
        productId: UUID,
        minQuantity: BigDecimal,
    ): Optional<PriceListLine>

    fun deleteByPriceListIdAndId(
        priceListId: UUID,
        id: UUID,
    ): Long
}
