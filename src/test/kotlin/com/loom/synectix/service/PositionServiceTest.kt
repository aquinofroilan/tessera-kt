package com.loom.synectix.service

import com.loom.synectix.dto.CreatePositionRequest
import com.loom.synectix.exception.BusinessRuleException
import com.loom.synectix.model.Department
import com.loom.synectix.model.Position
import com.loom.synectix.repository.PositionRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import java.util.Optional

class PositionServiceTest {
    private lateinit var repository: PositionRepository
    private lateinit var departmentService: DepartmentService
    private lateinit var service: PositionService

    private val orgId = "org-1"

    @BeforeEach
    fun setup() {
        repository = mock(PositionRepository::class.java)
        departmentService = mock(DepartmentService::class.java)
        whenever(repository.save(any<Position>())).thenAnswer { it.arguments[0] }
        whenever(repository.findByOrganizationIdAndCode(any(), any())).thenReturn(Optional.empty())
        service = PositionService(repository, departmentService)
    }

    @Test
    fun `create persists a position and validates the department when provided`() {
        whenever(departmentService.getDepartment("d1", orgId))
            .thenReturn(Department(id = "d1", code = "ENG", name = "Engineering", organizationId = orgId))

        val pos =
            service.createPosition(
                CreatePositionRequest(code = " SE2 ", title = " Software Engineer II ", departmentId = "d1", payGrade = "G5"),
                orgId,
            )

        assertThat(pos.code).isEqualTo("SE2")
        assertThat(pos.title).isEqualTo("Software Engineer II")
        assertThat(pos.departmentId).isEqualTo("d1")
        assertThat(pos.payGrade).isEqualTo("G5")
    }

    @Test
    fun `create rejects a duplicate code`() {
        whenever(repository.findByOrganizationIdAndCode(orgId, "SE2"))
            .thenReturn(Optional.of(Position(code = "SE2", title = "SE II", organizationId = orgId)))

        assertThatThrownBy { service.createPosition(CreatePositionRequest(code = "SE2", title = "SE II"), orgId) }
            .isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `deactivate rejects double-deactivation`() {
        whenever(repository.findById("p1"))
            .thenReturn(Optional.of(Position(id = "p1", code = "SE2", title = "SE II", organizationId = orgId, isActive = false)))

        assertThatThrownBy { service.deactivatePosition("p1", orgId) }
            .isInstanceOf(BusinessRuleException::class.java)
    }
}
