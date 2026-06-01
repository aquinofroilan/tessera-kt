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

    private val orgId = "org-1"
    private val projectId = "p-1"

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
        id: String,
        parent: String? = null,
        org: String = orgId,
        project: String = projectId,
    ) = ProjectTask(id = id, projectId = project, parentTaskId = parent, name = id, organizationId = org)

    @Test
    fun `create persists a task under a validated project`() {
        val t = service.createTask(projectId, CreateProjectTaskRequest(name = " Design "), orgId)
        assertThat(t.name).isEqualTo("Design")
        assertThat(t.projectId).isEqualTo(projectId)
        assertThat(t.status).isEqualTo(TaskStatus.TODO)
    }

    @Test
    fun `create with a parent validates the parent is in the same project`() {
        whenever(repository.findById("missing")).thenReturn(Optional.empty())
        assertThatThrownBy {
            service.createTask(projectId, CreateProjectTaskRequest(name = "Sub", parentTaskId = "missing"), orgId)
        }.isInstanceOf(ResourceNotFoundException::class.java)
    }

    @Test
    fun `setParent rejects self-parenting`() {
        whenever(repository.findById("t1")).thenReturn(Optional.of(task("t1")))
        assertThatThrownBy { service.setParent(projectId, "t1", "t1", orgId) }
            .isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `setParent rejects a descendant cycle`() {
        val t1 = task("t1")
        val t2 = task("t2", parent = "t1")
        whenever(repository.findById("t1")).thenReturn(Optional.of(t1))
        whenever(repository.findById("t2")).thenReturn(Optional.of(t2))
        whenever(repository.findByOrganizationIdAndProjectId(orgId, projectId)).thenReturn(listOf(t1, t2))

        assertThatThrownBy { service.setParent(projectId, "t1", "t2", orgId) }
            .isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `setParent clears the parent when null`() {
        whenever(repository.findById("t2")).thenReturn(Optional.of(task("t2", parent = "t1")))
        assertThat(service.setParent(projectId, "t2", null, orgId).parentTaskId).isNull()
    }

    @Test
    fun `task tree nests children under roots`() {
        val root = task("t1")
        val child = task("t2", parent = "t1")
        val grandchild = task("t3", parent = "t2")
        whenever(repository.findByOrganizationIdAndProjectId(orgId, projectId)).thenReturn(listOf(child, root, grandchild))

        val tree = service.getTaskTree(projectId, orgId)

        assertThat(tree).hasSize(1)
        assertThat(tree[0].id).isEqualTo("t1")
        assertThat(tree[0].children[0].id).isEqualTo("t2")
        assertThat(tree[0].children[0].children[0].id).isEqualTo("t3")
    }

    @Test
    fun `get rejects a task from another project`() {
        whenever(repository.findById("t1")).thenReturn(Optional.of(task("t1", project = "other")))
        assertThatThrownBy { service.getTask(projectId, "t1", orgId) }
            .isInstanceOf(ResourceNotFoundException::class.java)
    }

    @Test
    fun `update changes status`() {
        whenever(repository.findById("t1")).thenReturn(Optional.of(task("t1")))
        val updated = service.updateTask(projectId, "t1", UpdateProjectTaskRequest(status = TaskStatus.DONE), orgId)
        assertThat(updated.status).isEqualTo(TaskStatus.DONE)
    }
}
