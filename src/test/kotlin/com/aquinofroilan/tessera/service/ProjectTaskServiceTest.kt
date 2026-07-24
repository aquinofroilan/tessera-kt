package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.CreateProjectTaskRequest
import com.aquinofroilan.tessera.dto.UpdateProjectTaskRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import com.aquinofroilan.tessera.model.Project
import com.aquinofroilan.tessera.model.ProjectTask
import com.aquinofroilan.tessera.model.TaskStatus
import com.aquinofroilan.tessera.repository.ProjectTaskRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.util.Optional

class ProjectTaskServiceTest {
    private lateinit var repository: ProjectTaskRepository
    private lateinit var projectService: ProjectService
    private lateinit var employeeService: EmployeeService
    private lateinit var service: ProjectTaskService

    private val orgId = java.util.UUID.fromString("6c2f6004-070c-3d2d-9893-030d9211c19d")
    private val projectId = java.util.UUID.fromString("297fd989-49e7-378b-9562-907dbf28876d")

    @BeforeEach
    fun setup() {
        repository = mock(ProjectTaskRepository::class.java)
        projectService = mock(ProjectService::class.java)
        employeeService = mock(EmployeeService::class.java)
        whenever(repository.save(any<ProjectTask>())).thenAnswer { it.arguments[0] }
        whenever(projectService.getProject(projectId, orgId)).thenReturn(
            Project(
                id = projectId,
                projectNumber = "PRJ-0001",
                name = "Apollo",
                startDate = LocalDate.of(2026, 1, 1),
                organizationId = orgId,
            ),
        )
        service = ProjectTaskService(repository, projectService, employeeService)
    }

    private fun task(
        id: java.util.UUID,
        parent: java.util.UUID? = null,
        org: java.util.UUID = orgId,
        project: java.util.UUID = projectId,
    ) = ProjectTask(id = id, projectId = project, parentTaskId = parent, name = id.toString(), organizationId = org)

    @Test
    fun `create persists a task under a validated project`() {
        val t = service.createTask(projectId, CreateProjectTaskRequest(name = " Design "), orgId)
        assertThat(t.name).isEqualTo("Design")
        assertThat(t.projectId).isEqualTo(projectId)
        assertThat(t.status).isEqualTo(TaskStatus.TODO)
    }

    @Test
    fun `create with a parent validates the parent is in the same project`() {
        whenever(repository.findById(java.util.UUID.fromString("00000000-0000-0000-0000-000000000999"))).thenReturn(Optional.empty())
        assertThatThrownBy {
            service.createTask(
                projectId,
                CreateProjectTaskRequest(name = "Sub", parentTaskId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000999")),
                orgId,
            )
        }.isInstanceOf(ResourceNotFoundException::class.java)
    }

    @Test
    fun `setParent rejects self-parenting`() {
        whenever(
            repository.findById(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001")),
        ).thenReturn(Optional.of(task(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"))))
        assertThatThrownBy {
            service.setParent(
                projectId,
                java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"),
                java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"),
                orgId,
            )
        }.isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `setParent rejects a descendant cycle`() {
        val t1 = task(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"))
        val t2 =
            task(
                java.util.UUID.fromString("00000000-0000-0000-0000-000000000002"),
                parent = java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"),
            )
        whenever(repository.findById(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"))).thenReturn(Optional.of(t1))
        whenever(repository.findById(java.util.UUID.fromString("00000000-0000-0000-0000-000000000002"))).thenReturn(Optional.of(t2))
        whenever(repository.findByOrganizationIdAndProjectId(orgId, projectId)).thenReturn(listOf(t1, t2))

        assertThatThrownBy {
            service.setParent(
                projectId,
                java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"),
                java.util.UUID.fromString("00000000-0000-0000-0000-000000000002"),
                orgId,
            )
        }.isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `setParent clears the parent when null`() {
        whenever(
            repository.findById(java.util.UUID.fromString("00000000-0000-0000-0000-000000000002")),
        ).thenReturn(
            Optional.of(
                task(
                    java.util.UUID.fromString("00000000-0000-0000-0000-000000000002"),
                    parent = java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"),
                ),
            ),
        )
        assertThat(
            service.setParent(projectId, java.util.UUID.fromString("00000000-0000-0000-0000-000000000002"), null, orgId).parentTaskId,
        ).isNull()
    }

    @Test
    fun `task tree nests children under roots`() {
        val root = task(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"))
        val child =
            task(
                java.util.UUID.fromString("00000000-0000-0000-0000-000000000002"),
                parent = java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"),
            )
        val grandchild =
            task(
                java.util.UUID.fromString("00000000-0000-0000-0000-000000000003"),
                parent = java.util.UUID.fromString("00000000-0000-0000-0000-000000000002"),
            )
        whenever(repository.findByOrganizationIdAndProjectId(orgId, projectId)).thenReturn(listOf(child, root, grandchild))

        val tree = service.getTaskTree(projectId, orgId)

        assertThat(tree).hasSize(1)
        assertThat(tree[0].id).isEqualTo(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"))
        assertThat(tree[0].children[0].id).isEqualTo(java.util.UUID.fromString("00000000-0000-0000-0000-000000000002"))
        assertThat(tree[0].children[0].children[0].id).isEqualTo(java.util.UUID.fromString("00000000-0000-0000-0000-000000000003"))
    }

    @Test
    fun `get rejects a task from another project`() {
        whenever(
            repository.findById(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001")),
        ).thenReturn(
            Optional.of(task(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"), project = java.util.UUID.randomUUID())),
        )
        assertThatThrownBy { service.getTask(projectId, java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"), orgId) }
            .isInstanceOf(ResourceNotFoundException::class.java)
    }

    @Test
    fun `update changes status`() {
        whenever(
            repository.findById(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001")),
        ).thenReturn(Optional.of(task(java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"))))
        val updated =
            service.updateTask(
                projectId,
                java.util.UUID.fromString("00000000-0000-0000-0000-000000000001"),
                UpdateProjectTaskRequest(status = TaskStatus.DONE),
                orgId,
            )
        assertThat(updated.status).isEqualTo(TaskStatus.DONE)
    }
}
