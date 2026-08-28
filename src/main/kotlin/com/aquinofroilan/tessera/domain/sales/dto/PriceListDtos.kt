package com.aquinofroilan.tessera.domain.sales.dto

import com.aquinofroilan.tessera.domain.sales.model.CustomerSegment
import com.aquinofroilan.tessera.domain.sales.model.DiscountRule
import com.aquinofroilan.tessera.domain.sales.model.DiscountType
import com.aquinofroilan.tessera.domain.sales.model.PriceList
import com.aquinofroilan.tessera.domain.sales.model.PriceListLine
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class PriceListLineDto(
    val id: UUID,
    val productId: UUID,
    val productSku: String,
    val unitPrice: BigDecimal,
    val minQuantity: BigDecimal,
) {
    companion object {
        fun from(line: PriceListLine): PriceListLineDto =
            PriceListLineDto(
                id = line.id,
                productId = line.productId,
                productSku = line.productSku,
                unitPrice = line.unitPrice,
                minQuantity = line.minQuantity,
            )
    }
}

data class PriceListResponse(
    val id: UUID,
    val organizationId: UUID,
    val name: String,
    val code: String,
    val currency: String,
    val customerSegment: CustomerSegment?,
    val isDefault: Boolean,
    val isActive: Boolean,
    val validFrom: LocalDate?,
    val validTo: LocalDate?,
    val description: String?,
    val lines: List<PriceListLineDto>,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
) {
    companion object {
        fun from(priceList: PriceList): PriceListResponse =
            PriceListResponse(
                id = priceList.id,
                organizationId = priceList.organizationId,
                name = priceList.name,
                code = priceList.code,
                currency = priceList.currency,
                customerSegment = priceList.customerSegment,
                isDefault = priceList.isDefault,
                isActive = priceList.isActive,
                validFrom = priceList.validFrom,
                validTo = priceList.validTo,
                description = priceList.description,
                lines = priceList.lines.map { PriceListLineDto.from(it) },
                createdAt = priceList.createdAt,
                updatedAt = priceList.updatedAt,
            )
    }
}

data class CreatePriceListRequest(
    @field:NotBlank(message = "Price list name is required")
    val name: String,
    @field:NotBlank(message = "Price list code is required")
    val code: String,
    @field:NotBlank(message = "Currency is required")
    val currency: String,
    val customerSegment: CustomerSegment? = null,
    val isDefault: Boolean? = null,
    val validFrom: LocalDate? = null,
    val validTo: LocalDate? = null,
    val description: String? = null,
    val lines: List<CreatePriceListLineRequest>? = null,
)

data class UpdatePriceListRequest(
    val name: String? = null,
    val customerSegment: CustomerSegment? = null,
    val isDefault: Boolean? = null,
    val isActive: Boolean? = null,
    val validFrom: LocalDate? = null,
    val validTo: LocalDate? = null,
    val description: String? = null,
)

data class CreatePriceListLineRequest(
    @field:NotNull(message = "Product ID is required")
    val productId: UUID,
    @field:NotNull(message = "Unit price is required")
    @field:DecimalMin(value = "0.0", message = "Unit price cannot be negative")
    val unitPrice: BigDecimal,
    val minQuantity: BigDecimal? = null,
)

data class BatchSetPriceListLinesRequest(
    @field:NotEmpty(message = "Lines cannot be empty")
    val lines: List<CreatePriceListLineRequest>,
)

data class DiscountRuleResponse(
    val id: UUID,
    val organizationId: UUID,
    val name: String,
    val code: String,
    val discountType: DiscountType,
    val discountValue: BigDecimal,
    val customerSegment: CustomerSegment?,
    val customerId: UUID?,
    val productId: UUID?,
    val priceListId: UUID?,
    val minQuantity: BigDecimal?,
    val minOrderAmount: BigDecimal?,
    val validFrom: LocalDate?,
    val validTo: LocalDate?,
    val isActive: Boolean,
    val priority: Int,
    val description: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
) {
    companion object {
        fun from(rule: DiscountRule): DiscountRuleResponse =
            DiscountRuleResponse(
                id = rule.id,
                organizationId = rule.organizationId,
                name = rule.name,
                code = rule.code,
                discountType = rule.discountType,
                discountValue = rule.discountValue,
                customerSegment = rule.customerSegment,
                customerId = rule.customerId,
                productId = rule.productId,
                priceListId = rule.priceListId,
                minQuantity = rule.minQuantity,
                minOrderAmount = rule.minOrderAmount,
                validFrom = rule.validFrom,
                validTo = rule.validTo,
                isActive = rule.isActive,
                priority = rule.priority,
                description = rule.description,
                createdAt = rule.createdAt,
                updatedAt = rule.updatedAt,
            )
    }
}

data class CreateDiscountRuleRequest(
    @field:NotBlank(message = "Rule name is required")
    val name: String,
    @field:NotBlank(message = "Rule code is required")
    val code: String,
    @field:NotNull(message = "Discount type is required")
    val discountType: DiscountType,
    @field:NotNull(message = "Discount value is required")
    @field:DecimalMin(value = "0.0", message = "Discount value cannot be negative")
    val discountValue: BigDecimal,
    val customerSegment: CustomerSegment? = null,
    val customerId: UUID? = null,
    val productId: UUID? = null,
    val priceListId: UUID? = null,
    val minQuantity: BigDecimal? = null,
    val minOrderAmount: BigDecimal? = null,
    val validFrom: LocalDate? = null,
    val validTo: LocalDate? = null,
    val priority: Int? = null,
    val description: String? = null,
)

data class UpdateDiscountRuleRequest(
    val name: String? = null,
    val discountType: DiscountType? = null,
    val discountValue: BigDecimal? = null,
    val customerSegment: CustomerSegment? = null,
    val customerId: UUID? = null,
    val productId: UUID? = null,
    val priceListId: UUID? = null,
    val minQuantity: BigDecimal? = null,
    val minOrderAmount: BigDecimal? = null,
    val validFrom: LocalDate? = null,
    val validTo: LocalDate? = null,
    val isActive: Boolean? = null,
    val priority: Int? = null,
    val description: String? = null,
)

data class CalculatePriceRequest(
    @field:NotNull(message = "Product ID is required")
    val productId: UUID,
    val customerId: UUID? = null,
    val currency: String? = null,
    @field:DecimalMin(value = "0.0001", message = "Quantity must be greater than zero")
    val quantity: BigDecimal? = null,
    val orderDate: LocalDate? = null,
)

data class CalculatePriceResponse(
    val productId: UUID,
    val productSku: String,
    val productName: String,
    val currency: String,
    val quantity: BigDecimal,
    val priceListId: UUID?,
    val priceListName: String?,
    val baseCatalogPrice: BigDecimal,
    val baseUnitPrice: BigDecimal,
    val discountType: DiscountType?,
    val discountValue: BigDecimal?,
    val discountAmountPerUnit: BigDecimal,
    val effectiveUnitPrice: BigDecimal,
    val totalAmount: BigDecimal,
    val appliedDiscountRuleCode: String?,
    val appliedDiscountRuleName: String?,
)
