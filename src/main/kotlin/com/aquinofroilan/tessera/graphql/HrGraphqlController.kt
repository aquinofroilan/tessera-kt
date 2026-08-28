package com.aquinofroilan.tessera.graphql

import com.aquinofroilan.tessera.domain.hr.controller.DepartmentController
import com.aquinofroilan.tessera.domain.hr.controller.EmployeeCompensationController
import com.aquinofroilan.tessera.domain.hr.controller.EmployeeController
import com.aquinofroilan.tessera.domain.hr.controller.LeaveRequestController
import com.aquinofroilan.tessera.domain.hr.controller.LeaveTypeController
import com.aquinofroilan.tessera.domain.hr.controller.PayrollRunController
import com.aquinofroilan.tessera.domain.hr.controller.PositionController
import com.aquinofroilan.tessera.domain.hr.dto.CreateDepartmentRequest
import com.aquinofroilan.tessera.domain.hr.dto.CreateEmployeeCompensationRequest
import com.aquinofroilan.tessera.domain.hr.dto.CreateEmployeeRequest
import com.aquinofroilan.tessera.domain.hr.dto.CreateLeaveRequestRequest
import com.aquinofroilan.tessera.domain.hr.dto.CreateLeaveTypeRequest
import com.aquinofroilan.tessera.domain.hr.dto.CreatePayrollRunRequest
import com.aquinofroilan.tessera.domain.hr.dto.CreatePositionRequest
import com.aquinofroilan.tessera.domain.hr.dto.RejectLeaveRequestRequest
import com.aquinofroilan.tessera.domain.hr.dto.SetDepartmentParentRequest
import com.aquinofroilan.tessera.domain.hr.dto.TerminateEmployeeRequest
import com.aquinofroilan.tessera.domain.hr.dto.UpdateDepartmentRequest
import com.aquinofroilan.tessera.domain.hr.dto.UpdateEmployeeRequest
import com.aquinofroilan.tessera.domain.hr.dto.UpdateLeaveTypeRequest
import com.aquinofroilan.tessera.domain.hr.dto.UpdatePositionRequest
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import java.time.LocalDate

/**
 * GraphQL bridge for the HR routes — payroll runs, departments, employees,
 * positions, leave types/requests and employee compensation — delegating to the
 * existing REST controllers via the shared JSON-scalar pass-through.
 * Authorization mirrors the REST endpoints (`hr:read`/`hr:write`/`hr:approve`).
 */
@Controller
class HrGraphqlController(
    private val payrollRunController: PayrollRunController,
    private val departmentController: DepartmentController,
    private val employeeController: EmployeeController,
    private val positionController: PositionController,
    private val leaveTypeController: LeaveTypeController,
    private val leaveRequestController: LeaveRequestController,
    private val employeeCompensationController: EmployeeCompensationController,
    private val support: GraphqlBridgeSupport,
) {
    @QueryMapping
    @PreAuthorize("hasAuthority('hr:read')")
    fun payrollRuns(
        @Argument status: String?,
    ): Any = support.unwrap(payrollRunController.listPayrollRuns(support.orgId(), status))

    @QueryMapping
    @PreAuthorize("hasAuthority('hr:read')")
    fun payrollRun(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(payrollRunController.getPayrollRun(support.orgId(), id))

    @MutationMapping
    @PreAuthorize("hasAuthority('hr:write')")
    fun createPayrollRun(
        @Argument input: Any,
    ): Any =
        support.unwrap(
            payrollRunController.createPayrollRun(support.orgId(), support.userId(), support.toRequest<CreatePayrollRunRequest>(input)),
        )

    @MutationMapping
    @PreAuthorize("hasAuthority('hr:approve')")
    fun approvePayrollRun(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(payrollRunController.approvePayrollRun(support.orgId(), support.userId(), id))

    @MutationMapping
    @PreAuthorize("hasAuthority('hr:approve')")
    fun payPayrollRun(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(payrollRunController.payPayrollRun(support.orgId(), support.userId(), id))

    @MutationMapping
    @PreAuthorize("hasAuthority('hr:write')")
    fun cancelPayrollRun(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(payrollRunController.cancelPayrollRun(support.orgId(), id))

    @QueryMapping
    @PreAuthorize("hasAuthority('hr:read')")
    fun departments(
        @Argument activeOnly: Boolean,
    ): Any = support.unwrap(departmentController.listDepartments(support.orgId(), activeOnly))

    @QueryMapping
    @PreAuthorize("hasAuthority('hr:read')")
    fun department(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(departmentController.getDepartment(support.orgId(), id))

    @QueryMapping
    @PreAuthorize("hasAuthority('hr:read')")
    fun departmentOrgChart(): Any = support.unwrap(departmentController.getOrgChart(support.orgId()))

    @MutationMapping
    @PreAuthorize("hasAuthority('hr:write')")
    fun createDepartment(
        @Argument input: Any,
    ): Any = support.unwrap(departmentController.createDepartment(support.orgId(), support.toRequest<CreateDepartmentRequest>(input)))

    @MutationMapping
    @PreAuthorize("hasAuthority('hr:write')")
    fun updateDepartment(
        @Argument id: java.util.UUID,
        @Argument input: Any,
    ): Any = support.unwrap(departmentController.updateDepartment(support.orgId(), id, support.toRequest<UpdateDepartmentRequest>(input)))

    @MutationMapping
    @PreAuthorize("hasAuthority('hr:write')")
    fun setDepartmentParent(
        @Argument id: java.util.UUID,
        @Argument input: Any?,
    ): Any {
        val request = input?.let { support.toRequest<SetDepartmentParentRequest>(it) } ?: SetDepartmentParentRequest()
        return support.unwrap(departmentController.setParent(support.orgId(), id, request))
    }

    @MutationMapping
    @PreAuthorize("hasAuthority('hr:write')")
    fun deactivateDepartment(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(departmentController.deactivateDepartment(support.orgId(), id))

    @QueryMapping
    @PreAuthorize("hasAuthority('hr:read')")
    fun employees(
        @Argument status: String?,
        @Argument departmentId: java.util.UUID?,
    ): Any = support.unwrap(employeeController.listEmployees(support.orgId(), status, departmentId))

    @QueryMapping
    @PreAuthorize("hasAuthority('hr:read')")
    fun employee(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(employeeController.getEmployee(support.orgId(), id))

    @MutationMapping
    @PreAuthorize("hasAuthority('hr:write')")
    fun createEmployee(
        @Argument input: Any,
    ): Any = support.unwrap(employeeController.createEmployee(support.orgId(), support.toRequest<CreateEmployeeRequest>(input)))

    @MutationMapping
    @PreAuthorize("hasAuthority('hr:write')")
    fun updateEmployee(
        @Argument id: java.util.UUID,
        @Argument input: Any,
    ): Any = support.unwrap(employeeController.updateEmployee(support.orgId(), id, support.toRequest<UpdateEmployeeRequest>(input)))

    @MutationMapping
    @PreAuthorize("hasAuthority('hr:write')")
    fun assignEmployeeDepartment(
        @Argument id: java.util.UUID,
        @Argument departmentId: java.util.UUID?,
    ): Any = support.unwrap(employeeController.assignDepartment(support.orgId(), id, departmentId))

    @MutationMapping
    @PreAuthorize("hasAuthority('hr:write')")
    fun placeEmployeeOnLeave(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(employeeController.placeOnLeave(support.orgId(), id))

    @MutationMapping
    @PreAuthorize("hasAuthority('hr:write')")
    fun returnEmployeeFromLeave(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(employeeController.returnFromLeave(support.orgId(), id))

    @MutationMapping
    @PreAuthorize("hasAuthority('hr:write')")
    fun terminateEmployee(
        @Argument id: java.util.UUID,
        @Argument input: Any,
    ): Any = support.unwrap(employeeController.terminate(support.orgId(), id, support.toRequest<TerminateEmployeeRequest>(input)))

    @QueryMapping
    @PreAuthorize("hasAuthority('hr:read')")
    fun positions(
        @Argument activeOnly: Boolean,
    ): Any = support.unwrap(positionController.listPositions(support.orgId(), activeOnly))

    @QueryMapping
    @PreAuthorize("hasAuthority('hr:read')")
    fun position(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(positionController.getPosition(support.orgId(), id))

    @MutationMapping
    @PreAuthorize("hasAuthority('hr:write')")
    fun createPosition(
        @Argument input: Any,
    ): Any = support.unwrap(positionController.createPosition(support.orgId(), support.toRequest<CreatePositionRequest>(input)))

    @MutationMapping
    @PreAuthorize("hasAuthority('hr:write')")
    fun updatePosition(
        @Argument id: java.util.UUID,
        @Argument input: Any,
    ): Any = support.unwrap(positionController.updatePosition(support.orgId(), id, support.toRequest<UpdatePositionRequest>(input)))

    @MutationMapping
    @PreAuthorize("hasAuthority('hr:write')")
    fun deactivatePosition(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(positionController.deactivatePosition(support.orgId(), id))

    @QueryMapping
    @PreAuthorize("hasAuthority('hr:read')")
    fun leaveTypes(
        @Argument activeOnly: Boolean,
    ): Any = support.unwrap(leaveTypeController.listLeaveTypes(support.orgId(), activeOnly))

    @QueryMapping
    @PreAuthorize("hasAuthority('hr:read')")
    fun leaveType(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(leaveTypeController.getLeaveType(support.orgId(), id))

    @MutationMapping
    @PreAuthorize("hasAuthority('hr:write')")
    fun createLeaveType(
        @Argument input: Any,
    ): Any = support.unwrap(leaveTypeController.createLeaveType(support.orgId(), support.toRequest<CreateLeaveTypeRequest>(input)))

    @MutationMapping
    @PreAuthorize("hasAuthority('hr:write')")
    fun updateLeaveType(
        @Argument id: java.util.UUID,
        @Argument input: Any,
    ): Any = support.unwrap(leaveTypeController.updateLeaveType(support.orgId(), id, support.toRequest<UpdateLeaveTypeRequest>(input)))

    @MutationMapping
    @PreAuthorize("hasAuthority('hr:write')")
    fun deactivateLeaveType(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(leaveTypeController.deactivateLeaveType(support.orgId(), id))

    @QueryMapping
    @PreAuthorize("hasAuthority('hr:read')")
    fun leaveRequests(
        @Argument employeeId: java.util.UUID?,
        @Argument status: String?,
    ): Any = support.unwrap(leaveRequestController.listLeaveRequests(support.orgId(), employeeId, status))

    @QueryMapping
    @PreAuthorize("hasAuthority('hr:read')")
    fun leaveRequest(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(leaveRequestController.getLeaveRequest(support.orgId(), id))

    @QueryMapping
    @PreAuthorize("hasAuthority('hr:read')")
    fun leaveBalance(
        @Argument employeeId: java.util.UUID,
        @Argument leaveTypeId: java.util.UUID,
        @Argument year: Int?,
    ): Any = support.unwrap(leaveRequestController.balance(support.orgId(), employeeId, leaveTypeId, year))

    @MutationMapping
    @PreAuthorize("hasAuthority('hr:write')")
    fun createLeaveRequest(
        @Argument input: Any,
    ): Any =
        support.unwrap(
            leaveRequestController.createLeaveRequest(
                support.orgId(),
                support.userId(),
                support.toRequest<CreateLeaveRequestRequest>(input),
            ),
        )

    @MutationMapping
    @PreAuthorize("hasAuthority('hr:approve')")
    fun approveLeaveRequest(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(leaveRequestController.approveLeaveRequest(support.orgId(), support.userId(), id))

    @MutationMapping
    @PreAuthorize("hasAuthority('hr:approve')")
    fun rejectLeaveRequest(
        @Argument id: java.util.UUID,
        @Argument input: Any?,
    ): Any =
        support.unwrap(
            leaveRequestController.rejectLeaveRequest(
                support.orgId(),
                support.userId(),
                id,
                input?.let {
                    support.toRequest<RejectLeaveRequestRequest>(it)
                },
            ),
        )

    @MutationMapping
    @PreAuthorize("hasAuthority('hr:write')")
    fun cancelLeaveRequest(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(leaveRequestController.cancelLeaveRequest(support.orgId(), id))

    @QueryMapping
    @PreAuthorize("hasAuthority('hr:read')")
    fun employeeCompensations(
        @Argument employeeId: java.util.UUID,
    ): Any = support.unwrap(employeeCompensationController.listCompensation(support.orgId(), employeeId))

    @QueryMapping
    @PreAuthorize("hasAuthority('hr:read')")
    fun currentEmployeeCompensation(
        @Argument employeeId: java.util.UUID,
        @Argument asOf: String?,
    ): Any = support.unwrap(employeeCompensationController.currentCompensation(support.orgId(), employeeId, asOf?.let(LocalDate::parse)))

    @MutationMapping
    @PreAuthorize("hasAuthority('hr:write')")
    fun addEmployeeCompensation(
        @Argument employeeId: java.util.UUID,
        @Argument input: Any,
    ): Any =
        support.unwrap(
            employeeCompensationController.addCompensation(
                support.orgId(),
                support.userId(),
                employeeId,
                support.toRequest<CreateEmployeeCompensationRequest>(input),
            ),
        )
}
