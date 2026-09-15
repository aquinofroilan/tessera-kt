package com.aquinofroilan.tessera.domain.hr.dto

import com.aquinofroilan.tessera.domain.hr.model.ChecklistType
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull

data class CreateChecklistTemplateRequest(
    @field:NotEmpty(message = "Name is required")
    val name: String?,
    @field:NotNull(message = "Type is required")
    val type: ChecklistType?,
    val tasks: List<ChecklistTaskTemplateDto> = emptyList(),
)

data class ChecklistTaskTemplateDto(
    @field:NotEmpty(message = "Description is required")
    val description: String?,
    val isRequired: Boolean = true,
    val sortOrder: Int = 0,
)
