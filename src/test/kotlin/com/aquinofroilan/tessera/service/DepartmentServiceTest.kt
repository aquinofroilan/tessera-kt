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

    private val orgId = java.util.UUID.fromString("e5628ca4-87a8-3e6f-8ae2-20213cc7ef92")

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
        whenever(repository.findById(java.util.UUID.fromString("67fcd632-bb89-3160-94c2-9367eb55276c")))
            .thenReturn(
                Optional.of(
                    Department(
                        id = java.util.UUID.fromString("67fcd632-bb89-3160-94c2-9367eb55276c"),
                        code = "ENG",
                        name = "Engineering",
                        organizationId = java.util.UUID.fromString("f022a845-ae01-3e07-ae04-7fc0ffb096a8"),
                    ),
                ),
            )

        assertThatThrownBy { service.getDepartment(java.util.UUID.fromString("67fcd632-bb89-3160-94c2-9367eb55276c"), orgId) }
            .isInstanceOf(ResourceNotFoundException::class.java)
    }

    @Test
    fun `update changes name and description only`() {
        whenever(repository.findById(java.util.UUID.fromString("67fcd632-bb89-3160-94c2-9367eb55276c")))
            .thenReturn(
                Optional.of(
                    Department(
                        id = java.util.UUID.fromString("67fcd632-bb89-3160-94c2-9367eb55276c"),
                        code = "ENG",
                        name = "Engineering",
                        organizationId = orgId,
                    ),
                ),
            )

        val updated =
            service.updateDepartment(
                java.util.UUID.fromString("67fcd632-bb89-3160-94c2-9367eb55276c"),
                UpdateDepartmentRequest(name = "R&D"),
                orgId,
            )

        assertThat(updated.name).isEqualTo("R&D")
        assertThat(updated.code).isEqualTo("ENG")
    }

    @Test
    fun `deactivate flips the active flag and rejects double-deactivation`() {
        whenever(repository.findById(java.util.UUID.fromString("67fcd632-bb89-3160-94c2-9367eb55276c")))
            .thenReturn(
                Optional.of(
                    Department(
                        id = java.util.UUID.fromString("67fcd632-bb89-3160-94c2-9367eb55276c"),
                        code = "ENG",
                        name = "Engineering",
                        organizationId = orgId,
                        isActive = false,
                    ),
                ),
            )

        assertThatThrownBy { service.deactivateDepartment(java.util.UUID.fromString("67fcd632-bb89-3160-94c2-9367eb55276c"), orgId) }
            .isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `create with a parent validates the parent belongs to the org`() {
        whenever(repository.findById(java.util.UUID.fromString("fd2ef362-436b-3e1e-8083-ccaefc73ba78"))).thenReturn(Optional.empty())

        assertThatThrownBy {
            service.createDepartment(
                CreateDepartmentRequest(
                    code = "ENG",
                    name = "Eng",
                    parentId = java.util.UUID.fromString("fd2ef362-436b-3e1e-8083-ccaefc73ba78"),
                ),
                orgId,
            )
        }.isInstanceOf(ResourceNotFoundException::class.java)
    }

    @Test
    fun `setParent rejects self-parenting`() {
        whenever(repository.findById(java.util.UUID.fromString("67fcd632-bb89-3160-94c2-9367eb55276c")))
            .thenReturn(
                Optional.of(
                    Department(
                        id = java.util.UUID.fromString("67fcd632-bb89-3160-94c2-9367eb55276c"),
                        code = "ENG",
                        name = "Engineering",
                        organizationId = orgId,
                    ),
                ),
            )

        assertThatThrownBy {
            service.setParent(
                java.util.UUID.fromString("67fcd632-bb89-3160-94c2-9367eb55276c"),
                java.util.UUID.fromString("67fcd632-bb89-3160-94c2-9367eb55276c"),
                orgId,
            )
        }.isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `setParent rejects a move that would create a cycle`() {
        val parent =
            Department(
                id = java.util.UUID.fromString("67fcd632-bb89-3160-94c2-9367eb55276c"),
                code = "ENG",
                name = "Engineering",
                organizationId = orgId,
            )
        val child =
            Department(
                id = java.util.UUID.fromString("f4a3552a-c7de-3d1b-af12-d405cc03de00"),
                code = "BE",
                name = "Backend",
                organizationId = orgId,
                parentId = java.util.UUID.fromString("67fcd632-bb89-3160-94c2-9367eb55276c"),
            )
        whenever(repository.findById(java.util.UUID.fromString("67fcd632-bb89-3160-94c2-9367eb55276c"))).thenReturn(Optional.of(parent))
        whenever(repository.findById(java.util.UUID.fromString("f4a3552a-c7de-3d1b-af12-d405cc03de00"))).thenReturn(Optional.of(child))
        whenever(repository.findByOrganizationId(orgId)).thenReturn(listOf(parent, child))

        // Making d1 (an ancestor) a child of d2 (its descendant) is a cycle.
        assertThatThrownBy {
            service.setParent(
                java.util.UUID.fromString("67fcd632-bb89-3160-94c2-9367eb55276c"),
                java.util.UUID.fromString("f4a3552a-c7de-3d1b-af12-d405cc03de00"),
                orgId,
            )
        }.isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `setParent clears the parent when null`() {
        whenever(repository.findById(java.util.UUID.fromString("f4a3552a-c7de-3d1b-af12-d405cc03de00")))
            .thenReturn(
                Optional.of(
                    Department(
                        id = java.util.UUID.fromString("f4a3552a-c7de-3d1b-af12-d405cc03de00"),
                        code = "BE",
                        name = "Backend",
                        organizationId = orgId,
                        parentId = java.util.UUID.fromString("67fcd632-bb89-3160-94c2-9367eb55276c"),
                    ),
                ),
            )

        val updated = service.setParent(java.util.UUID.fromString("f4a3552a-c7de-3d1b-af12-d405cc03de00"), null, orgId)

        assertThat(updated.parentId).isNull()
    }

    @Test
    fun `org chart nests children under their roots`() {
        val root =
            Department(
                id = java.util.UUID.fromString("67fcd632-bb89-3160-94c2-9367eb55276c"),
                code = "ENG",
                name = "Engineering",
                organizationId = orgId,
            )
        val child =
            Department(
                id = java.util.UUID.fromString("f4a3552a-c7de-3d1b-af12-d405cc03de00"),
                code = "BE",
                name = "Backend",
                organizationId = orgId,
                parentId = java.util.UUID.fromString("67fcd632-bb89-3160-94c2-9367eb55276c"),
            )
        val grandchild =
            Department(
                id = java.util.UUID.fromString("e29b3acf-98e5-3490-8aed-24d79551dbb9"),
                code = "API",
                name = "API",
                organizationId = orgId,
                parentId = java.util.UUID.fromString("f4a3552a-c7de-3d1b-af12-d405cc03de00"),
            )
        whenever(repository.findByOrganizationId(orgId)).thenReturn(listOf(child, root, grandchild))

        val chart = service.getOrgChart(orgId)

        assertThat(chart).hasSize(1)
        assertThat(chart[0].id).isEqualTo(java.util.UUID.fromString("67fcd632-bb89-3160-94c2-9367eb55276c"))
        assertThat(chart[0].children).hasSize(1)
        assertThat(chart[0].children[0].id).isEqualTo(java.util.UUID.fromString("f4a3552a-c7de-3d1b-af12-d405cc03de00"))
        assertThat(chart[0].children[0].children[0].id).isEqualTo(java.util.UUID.fromString("e29b3acf-98e5-3490-8aed-24d79551dbb9"))
    }
}
