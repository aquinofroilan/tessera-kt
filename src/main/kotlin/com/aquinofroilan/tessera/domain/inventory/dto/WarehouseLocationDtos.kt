package com.aquinofroilan.tessera.domain.inventory.dto

import com.aquinofroilan.tessera.domain.inventory.model.LocationType
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSetter
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.util.UUID

data class CreateWarehouseLocationRequest(
    @field:NotNull(message = "Warehouse ID is required")
    val warehouseId: UUID,
    val parentLocationId: UUID? = null,
    @field:NotBlank(message = "Code is required")
    @field:Size(max = 64, message = "Code must be 64 characters or fewer")
    val code: String,
    @field:NotNull(message = "Type is required")
    val type: LocationType?,
    @field:Size(max = 128, message = "Barcode must be 128 characters or fewer")
    val barcode: String? = null,
    @param:JsonProperty("isActive")
    @get:JsonProperty("isActive")
    val isActive: Boolean = true,
    @param:JsonProperty("isLocked")
    @get:JsonProperty("isLocked")
    val isLocked: Boolean = false,
    @param:JsonProperty("isPickable")
    @get:JsonProperty("isPickable")
    val isPickable: Boolean = true,
    @param:JsonProperty("isReceivable")
    @get:JsonProperty("isReceivable")
    val isReceivable: Boolean = true,
    @field:DecimalMin(value = "0.0", inclusive = false, message = "Maximum weight must be positive")
    val maxWeightKg: BigDecimal? = null,
    @field:DecimalMin(value = "0.0", inclusive = false, message = "Maximum volume must be positive")
    val maxVolumeCbm: BigDecimal? = null,
)

data class WarehouseLocationResponse(
    val id: UUID,
    val warehouseId: UUID,
    val parentLocationId: UUID?,
    val code: String,
    val type: LocationType,
    val barcode: String?,
    @param:JsonProperty("isActive")
    @get:JsonProperty("isActive")
    val isActive: Boolean,
    @param:JsonProperty("isLocked")
    @get:JsonProperty("isLocked")
    val isLocked: Boolean,
    @param:JsonProperty("isPickable")
    @get:JsonProperty("isPickable")
    val isPickable: Boolean,
    @param:JsonProperty("isReceivable")
    @get:JsonProperty("isReceivable")
    val isReceivable: Boolean,
    val maxWeightKg: BigDecimal?,
    val maxVolumeCbm: BigDecimal?,
)

class UpdateWarehouseLocationRequest(
    @field:Size(max = 64, message = "Code must be 64 characters or fewer")
    val code: String? = null,
    val type: LocationType? = null,
    @field:Size(max = 128, message = "Barcode must be 128 characters or fewer")
    val barcode: String? = null,
    @param:JsonProperty("isActive")
    @get:JsonProperty("isActive")
    val isActive: Boolean? = null,
    @param:JsonProperty("isLocked")
    @get:JsonProperty("isLocked")
    val isLocked: Boolean? = null,
    @param:JsonProperty("isPickable")
    @get:JsonProperty("isPickable")
    val isPickable: Boolean? = null,
    @param:JsonProperty("isReceivable")
    @get:JsonProperty("isReceivable")
    val isReceivable: Boolean? = null,
) {
    var parentLocationId: UUID? = null
        private set

    @get:JsonIgnore
    var parentLocationIdProvided: Boolean = false
        private set

    @JsonSetter("parentLocationId")
    fun assignParentLocationId(value: UUID?) {
        parentLocationId = value
        parentLocationIdProvided = true
    }

    @field:DecimalMin(value = "0.0", inclusive = false, message = "Maximum weight must be positive")
    var maxWeightKg: BigDecimal? = null
        private set

    @get:JsonIgnore
    var maxWeightKgProvided: Boolean = false
        private set

    @JsonSetter("maxWeightKg")
    fun assignMaxWeightKg(value: BigDecimal?) {
        maxWeightKg = value
        maxWeightKgProvided = true
    }

    @field:DecimalMin(value = "0.0", inclusive = false, message = "Maximum volume must be positive")
    var maxVolumeCbm: BigDecimal? = null
        private set

    @get:JsonIgnore
    var maxVolumeCbmProvided: Boolean = false
        private set

    @JsonSetter("maxVolumeCbm")
    fun assignMaxVolumeCbm(value: BigDecimal?) {
        maxVolumeCbm = value
        maxVolumeCbmProvided = true
    }
}
