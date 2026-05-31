package com.aquinofroilan.tessera.dto

import com.aquinofroilan.tessera.model.Position
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreatePositionRequest(
    @field:NotBlank(message = "Code is required")
    @field:Size(max = 32, message = "Code must be 32 characters or fewer")
    val code: String,
    @field:NotBlank(message = "Title is required")
    val title: String,
    val departmentId: String? = null,
    val payGrade: String? = null,
)

data class UpdatePositionRequest(
    val title: String? = null,
    val departmentId: String? = null,
    val payGrade: String? = null,
)

data class PositionResponse(
    val id: String,
    val code: String,
    val title: String,
    val departmentId: String?,
    val payGrade: String?,
    val organizationId: String,
    val isActive: Boolean,
    val createdAt: String?,
    val updatedAt: String?,
) {
    companion object {
        fun from(position: Position) =
            PositionResponse(
                id = position.id,
                code = position.code,
                title = position.title,
                departmentId = position.departmentId,
                payGrade = position.payGrade,
                organizationId = position.organizationId,
                isActive = position.isActive,
                createdAt = position.createdAt?.toString(),
                updatedAt = position.updatedAt?.toString(),
            )
    }
}
