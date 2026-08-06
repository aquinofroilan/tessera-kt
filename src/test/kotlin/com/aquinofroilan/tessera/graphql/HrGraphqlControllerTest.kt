package com.aquinofroilan.tessera.graphql

import java.util.UUID
import com.aquinofroilan.tessera.config.TestSecurityConfig
import com.aquinofroilan.tessera.controller.DepartmentController
import com.aquinofroilan.tessera.controller.EmployeeCompensationController
import com.aquinofroilan.tessera.controller.EmployeeController
import com.aquinofroilan.tessera.controller.LeaveRequestController
import com.aquinofroilan.tessera.controller.LeaveTypeController
import com.aquinofroilan.tessera.controller.PayrollRunController
import com.aquinofroilan.tessera.controller.PositionController
import com.aquinofroilan.tessera.security.TesseraPermissionEvaluator
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest
import org.springframework.context.annotation.Import
import org.springframework.graphql.test.tester.GraphQlTester
import org.springframework.http.ResponseEntity
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.bean.override.mockito.MockitoBean

@GraphQlTest(controllers = [HrGraphqlController::class])
@Import(
    TestSecurityConfig::class,
    TesseraPermissionEvaluator::class,
    GraphqlExceptionResolver::class,
    GraphqlScalarConfig::class,
    GraphqlBridgeSupport::class,
)
class HrGraphqlControllerTest {
    @Autowired
    private lateinit var graphQlTester: GraphQlTester

    @MockitoBean
    private lateinit var payrollRunController: PayrollRunController

    @MockitoBean
    private lateinit var departmentController: DepartmentController

    @MockitoBean
    private lateinit var employeeController: EmployeeController

    @MockitoBean
    private lateinit var positionController: PositionController

    @MockitoBean
    private lateinit var leaveTypeController: LeaveTypeController

    @MockitoBean
    private lateinit var leaveRequestController: LeaveRequestController

    @MockitoBean
    private lateinit var employeeCompensationController: EmployeeCompensationController

    @Test
    @WithMockUser(authorities = ["hr:read"])
    fun `employees query should return json payload`() {
        `when`(employeeController.listEmployees(null, null))
            .thenReturn(ResponseEntity.ok(listOf(mapOf("id" to "00000000-0000-0000-0000-000000000199", "status" to "ACTIVE"))))

        graphQlTester
            .document(
                """
                query {
                  employees
                }
                """.trimIndent(),
            ).execute()
            .path("employees[0].id")
            .entity(String::class.java)
            .isEqualTo("00000000-0000-0000-0000-000000000199")
    }

    @Test
    @WithMockUser(authorities = ["hr:read"])
    fun `departmentOrgChart query should return the nested tree`() {
        `when`(departmentController.getOrgChart())
            .thenReturn(
                ResponseEntity.ok(
                    listOf(mapOf("id" to "00000000-0000-0000-0000-000000000199", "children" to listOf(mapOf("id" to "00000000-0000-0000-0000-000000000199", "children" to emptyList<Any>())))),
                ),
            )

        graphQlTester
            .document(
                """
                query {
                  departmentOrgChart
                }
                """.trimIndent(),
            ).execute()
            .path("departmentOrgChart[0].children[0].id")
            .entity(String::class.java)
            .isEqualTo("d2")
    }

    @Test
    @WithMockUser(authorities = ["hr:approve"])
    fun `approvePayrollRun mutation should bridge to controller`() {
        `when`(payrollRunController.approvePayrollRun(UUID.fromString("00000000-0000-0000-0000-000000000199")))
            .thenReturn(ResponseEntity.ok(mapOf("id" to "00000000-0000-0000-0000-000000000199", "status" to "APPROVED")))

        graphQlTester
            .document(
                """
                mutation {
                  approvePayrollRun(id: "00000000-0000-0000-0000-000000000199")
                }
                """.trimIndent(),
            ).execute()
            .path("approvePayrollRun.status")
            .entity(String::class.java)
            .isEqualTo("APPROVED")
    }

    @Test
    @WithMockUser(authorities = ["hr:read"])
    fun `createDepartment mutation should be denied without write authority`() {
        graphQlTester
            .document(
                """
                mutation(${'$'}input: JSON!) {
                  createDepartment(input: ${'$'}input)
                }
                """.trimIndent(),
            ).variable("input", mapOf("name" to "Engineering"))
            .execute()
            .errors()
            .satisfy { errors ->
                org.assertj.core.api.Assertions
                    .assertThat(errors)
                    .isNotEmpty()
            }
    }
}
