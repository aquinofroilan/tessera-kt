package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.CreatePositionRequest
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.model.Department
import com.aquinofroilan.tessera.model.Position
import com.aquinofroilan.tessera.repository.PositionRepository
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

    private val orgId = java.util.UUID.fromString("e5628ca4-87a8-3e6f-8ae2-20213cc7ef92")

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
        whenever(departmentService.getDepartment(java.util.UUID.fromString("67fcd632-bb89-3160-94c2-9367eb55276c"), orgId))
            .thenReturn(
                Department(
                    id = java.util.UUID.fromString("67fcd632-bb89-3160-94c2-9367eb55276c"),
                    code = "ENG",
                    name = "Engineering",
                    organizationId = orgId,
                ),
            )

        val pos =
            service.createPosition(
                CreatePositionRequest(
                    code = " SE2 ",
                    title = " Software Engineer II ",
                    departmentId = java.util.UUID.fromString("67fcd632-bb89-3160-94c2-9367eb55276c"),
                    payGrade = "G5",
                ),
                orgId,
            )

        assertThat(pos.code).isEqualTo("SE2")
        assertThat(pos.title).isEqualTo("Software Engineer II")
        assertThat(pos.departmentId).isEqualTo(java.util.UUID.fromString("67fcd632-bb89-3160-94c2-9367eb55276c"))
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
        whenever(repository.findById(java.util.UUID.fromString("fd2ef362-436b-3e1e-8083-ccaefc73ba78")))
            .thenReturn(
                Optional.of(
                    Position(
                        id = java.util.UUID.fromString("fd2ef362-436b-3e1e-8083-ccaefc73ba78"),
                        code = "SE2",
                        title = "SE II",
                        organizationId = orgId,
                        isActive = false,
                    ),
                ),
            )

        assertThatThrownBy { service.deactivatePosition(java.util.UUID.fromString("fd2ef362-436b-3e1e-8083-ccaefc73ba78"), orgId) }
            .isInstanceOf(BusinessRuleException::class.java)
    }
}
