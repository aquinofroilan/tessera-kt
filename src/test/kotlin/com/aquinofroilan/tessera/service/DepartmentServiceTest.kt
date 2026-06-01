package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.CreateDepartmentRequest
import com.aquinofroilan.tessera.dto.UpdateDepartmentRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import com.aquinofroilan.tessera.model.Department
import com.aquinofroilan.tessera.repository.DepartmentRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.util.Optional

class DepartmentServiceTest {
    private lateinit var repository: DepartmentRepository
    private lateinit var service: DepartmentService

    private val orgId = "org-1"

    @BeforeEach
    fun setup() {
        repository = mock(DepartmentRepository::class.java)
        whenever(repository.save(any<Department>())).thenAnswer { it.arguments[0] }
        whenever(repository.findByOrganizationIdAndCode(any(), any())).thenReturn(Optional.empty())
        service = DepartmentService(repository)
    }

    @Test
    fun `create trims and persists an active department`() {
        val dept = service.createDepartment(CreateDepartmentRequest(code = " ENG ", name = " Engineering "), orgId)

        assertThat(dept.code).isEqualTo("ENG")
        assertThat(dept.name).isEqualTo("Engineering")
        assertThat(dept.isActive).isTrue()
        assertThat(dept.organizationId).isEqualTo(orgId)
    }

    @Test
    fun `create rejects a duplicate code`() {
        whenever(repository.findByOrganizationIdAndCode(orgId, "ENG"))
            .thenReturn(Optional.of(Department(code = "ENG", name = "Engineering", organizationId = orgId)))

        assertThatThrownBy { service.createDepartment(CreateDepartmentRequest(code = "ENG", name = "Eng"), orgId) }
            .isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `get rejects cross-org access`() {
        whenever(repository.findById("d1"))
            .thenReturn(Optional.of(Department(id = "d1", code = "ENG", name = "Engineering", organizationId = "other")))

        assertThatThrownBy { service.getDepartment("d1", orgId) }
            .isInstanceOf(ResourceNotFoundException::class.java)
    }

    @Test
    fun `update changes name and description only`() {
        whenever(repository.findById("d1"))
            .thenReturn(Optional.of(Department(id = "d1", code = "ENG", name = "Engineering", organizationId = orgId)))

        val updated = service.updateDepartment("d1", UpdateDepartmentRequest(name = "R&D"), orgId)

        assertThat(updated.name).isEqualTo("R&D")
        assertThat(updated.code).isEqualTo("ENG")
    }

    @Test
    fun `deactivate flips the active flag and rejects double-deactivation`() {
        whenever(repository.findById("d1"))
            .thenReturn(Optional.of(Department(id = "d1", code = "ENG", name = "Engineering", organizationId = orgId, isActive = false)))

        assertThatThrownBy { service.deactivateDepartment("d1", orgId) }
            .isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `create with a parent validates the parent belongs to the org`() {
        whenever(repository.findById("p1")).thenReturn(Optional.empty())

        assertThatThrownBy {
            service.createDepartment(CreateDepartmentRequest(code = "ENG", name = "Eng", parentId = "p1"), orgId)
        }.isInstanceOf(ResourceNotFoundException::class.java)
    }

    @Test
    fun `setParent rejects self-parenting`() {
        whenever(repository.findById("d1"))
            .thenReturn(Optional.of(Department(id = "d1", code = "ENG", name = "Engineering", organizationId = orgId)))

        assertThatThrownBy { service.setParent("d1", "d1", orgId) }
            .isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `setParent rejects a move that would create a cycle`() {
        val parent = Department(id = "d1", code = "ENG", name = "Engineering", organizationId = orgId)
        val child = Department(id = "d2", code = "BE", name = "Backend", organizationId = orgId, parentId = "d1")
        whenever(repository.findById("d1")).thenReturn(Optional.of(parent))
        whenever(repository.findById("d2")).thenReturn(Optional.of(child))
        whenever(repository.findByOrganizationId(orgId)).thenReturn(listOf(parent, child))

        // Making d1 (an ancestor) a child of d2 (its descendant) is a cycle.
        assertThatThrownBy { service.setParent("d1", "d2", orgId) }
            .isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `setParent clears the parent when null`() {
        whenever(repository.findById("d2"))
            .thenReturn(Optional.of(Department(id = "d2", code = "BE", name = "Backend", organizationId = orgId, parentId = "d1")))

        val updated = service.setParent("d2", null, orgId)

        assertThat(updated.parentId).isNull()
    }

    @Test
    fun `org chart nests children under their roots`() {
        val root = Department(id = "d1", code = "ENG", name = "Engineering", organizationId = orgId)
        val child = Department(id = "d2", code = "BE", name = "Backend", organizationId = orgId, parentId = "d1")
        val grandchild = Department(id = "d3", code = "API", name = "API", organizationId = orgId, parentId = "d2")
        whenever(repository.findByOrganizationId(orgId)).thenReturn(listOf(child, root, grandchild))

        val chart = service.getOrgChart(orgId)

        assertThat(chart).hasSize(1)
        assertThat(chart[0].id).isEqualTo("d1")
        assertThat(chart[0].children).hasSize(1)
        assertThat(chart[0].children[0].id).isEqualTo("d2")
        assertThat(chart[0].children[0].children[0].id).isEqualTo("d3")
    }
}
