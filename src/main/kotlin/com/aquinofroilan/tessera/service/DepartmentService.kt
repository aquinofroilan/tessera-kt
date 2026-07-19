package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.CreateDepartmentRequest
import com.aquinofroilan.tessera.dto.DepartmentTreeNode
import com.aquinofroilan.tessera.dto.UpdateDepartmentRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import com.aquinofroilan.tessera.model.Department
import com.aquinofroilan.tessera.repository.DepartmentRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class DepartmentService(
    private val departmentRepository: DepartmentRepository,
) {
    @Transactional
    fun createDepartment(
        request: CreateDepartmentRequest,
        organizationId: java.util.UUID,
    ): Department {
        val code = request.code.trim()
        if (departmentRepository.findByOrganizationIdAndCode(organizationId, code).isPresent) {
            throw BusinessRuleException("Department code '$code' already exists")
        }
        request.parentId?.let { requireParentInOrg(it, organizationId) }
        return departmentRepository.save(
            Department(
                code = code,
                name = request.name.trim(),
                description = request.description,
                parentId = request.parentId,
                organizationId = organizationId,
            ),
        )
    }

    fun getDepartment(
        id: java.util.UUID,
        organizationId: java.util.UUID,
    ): Department {
        val department =
            departmentRepository.findById(id).orElseThrow {
                ResourceNotFoundException("Department not found")
            }
        if (department.organizationId != organizationId) {
            throw ResourceNotFoundException("Department not found")
        }
        return department
    }

    fun listDepartments(
        organizationId: java.util.UUID,
        activeOnly: Boolean = false,
    ): List<Department> =
        if (activeOnly) {
            departmentRepository.findByOrganizationIdAndIsActive(organizationId, true)
        } else {
            departmentRepository.findByOrganizationId(organizationId)
        }

    @Transactional
    fun updateDepartment(
        id: java.util.UUID,
        request: UpdateDepartmentRequest,
        organizationId: java.util.UUID,
    ): Department {
        val department = getDepartment(id, organizationId)
        department.apply {
            name = request.name?.trim() ?: department.name
            description = request.description ?: department.description
        }
        return departmentRepository.save(department)
    }

    @Transactional
    fun deactivateDepartment(
        id: java.util.UUID,
        organizationId: java.util.UUID,
    ): Department {
        val department = getDepartment(id, organizationId)
        if (!department.isActive) {
            throw BusinessRuleException("Department is already inactive")
        }
        department.isActive = false
        return departmentRepository.save(department)
    }

    /**
     * Sets or clears a department's parent. A null [parentId] promotes the
     * department to a root. Rejects self-parenting and any move that would
     * introduce a cycle (i.e. making a department a child of its own descendant).
     */
    @Transactional
    fun setParent(
        id: java.util.UUID,
        parentId: java.util.UUID?,
        organizationId: java.util.UUID,
    ): Department {
        val department = getDepartment(id, organizationId)
        if (parentId == null) {
            department.parentId = null
            return departmentRepository.save(department)
        }
        if (parentId == id) {
            throw BusinessRuleException("A department cannot be its own parent")
        }
        requireParentInOrg(parentId, organizationId)
        if (descendantIds(id, organizationId).contains(parentId)) {
            throw BusinessRuleException("Cannot move a department under one of its own descendants")
        }
        department.parentId = parentId
        return departmentRepository.save(department)
    }

    /**
     * Builds the department org chart for an organization: root departments
     * (those without a parent) with their descendants nested beneath them.
     */
    fun getOrgChart(organizationId: java.util.UUID): List<DepartmentTreeNode> {
        val all = departmentRepository.findByOrganizationId(organizationId)
        val childrenByParent = all.groupBy { it.parentId }

        fun build(department: Department): DepartmentTreeNode =
            DepartmentTreeNode.from(
                department,
                childrenByParent[department.id].orEmpty().sortedBy { it.code }.map(::build),
            )
        return childrenByParent[null].orEmpty().sortedBy { it.code }.map(::build)
    }

    private fun requireParentInOrg(
        parentId: java.util.UUID,
        organizationId: java.util.UUID,
    ) {
        // Reuses the cross-org guard: a parent in another org surfaces as "not found".
        getDepartment(parentId, organizationId)
    }

    private fun descendantIds(
        id: java.util.UUID,
        organizationId: java.util.UUID,
    ): Set<java.util.UUID> {
        val childrenByParent = departmentRepository.findByOrganizationId(organizationId).groupBy { it.parentId }
        val descendants = mutableSetOf<java.util.UUID>()
        val queue = ArrayDeque(childrenByParent[id].orEmpty().map { it.id })
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (descendants.add(current)) {
                childrenByParent[current].orEmpty().forEach { queue.addLast(it.id) }
            }
        }
        return descendants
    }
}
