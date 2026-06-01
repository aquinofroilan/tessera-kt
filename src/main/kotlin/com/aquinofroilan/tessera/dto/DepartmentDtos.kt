package com.aquinofroilan.tessera.dto

import com.aquinofroilan.tessera.model.Department
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CreateDepartmentRequest(
    @field:NotBlank(message = "Code is required")
    @field:Size(max = 32, message = "Code must be 32 characters or fewer")
    val code: String,
    @field:NotBlank(message = "Name is required")
    val name: String,
    val description: String? = null,
    val parentId: String? = null,
)

data class UpdateDepartmentRequest(
    val name: String? = null,
    val description: String? = null,
)

/**
 * Sets (or clears) a department's parent for the org chart. A null [parentId]
 * promotes the department to a root.
 */
data class SetDepartmentParentRequest(
    val parentId: String? = null,
)

data class DepartmentResponse(
    val id: String,
    val code: String,
    val name: String,
    val description: String?,
    val parentId: String?,
    val organizationId: String,
    val isActive: Boolean,
    val createdAt: String?,
    val updatedAt: String?,
) {
    companion object {
        fun from(department: Department) =
            DepartmentResponse(
                id = department.id,
                code = department.code,
                name = department.name,
                description = department.description,
                parentId = department.parentId,
                organizationId = department.organizationId,
                isActive = department.isActive,
                createdAt = department.createdAt?.toString(),
                updatedAt = department.updatedAt?.toString(),
            )
    }
}

/**
 * A node in the department org chart: the department plus its nested children.
 */
data class DepartmentTreeNode(
    val id: String,
    val code: String,
    val name: String,
    val isActive: Boolean,
    val children: List<DepartmentTreeNode>,
) {
    companion object {
        fun from(
            department: Department,
            children: List<DepartmentTreeNode>,
        ) = DepartmentTreeNode(
            id = department.id,
            code = department.code,
            name = department.name,
            isActive = department.isActive,
            children = children,
        )
    }
}
