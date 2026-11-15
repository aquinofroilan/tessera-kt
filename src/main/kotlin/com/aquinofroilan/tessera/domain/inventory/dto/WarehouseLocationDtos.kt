package com.aquinofroilan.tessera.domain.inventory.dto

import com.aquinofroilan.tessera.domain.inventory.model.LocationType
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSetter
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
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
)

class UpdateWarehouseLocationRequest(
    @field:Size(max = 64, message = "Code must be 64 characters or fewer")
    val code: String? = null,
    val type: LocationType? = null,
    @field:Size(max = 128, message = "Barcode must be 128 characters or fewer")
    val barcode: String? = null,
    val isActive: Boolean? = null,
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
}
