package com.aquinofroilan.tessera.graphql

import com.aquinofroilan.tessera.controller.DepartmentController
import com.aquinofroilan.tessera.controller.EmployeeCompensationController
import com.aquinofroilan.tessera.controller.EmployeeController
import com.aquinofroilan.tessera.controller.LeaveRequestController
import com.aquinofroilan.tessera.controller.LeaveTypeController
import com.aquinofroilan.tessera.controller.PayrollRunController
import com.aquinofroilan.tessera.controller.PositionController
import com.aquinofroilan.tessera.dto.CreateDepartmentRequest
import com.aquinofroilan.tessera.dto.CreateEmployeeCompensationRequest
import com.aquinofroilan.tessera.dto.CreateEmployeeRequest
import com.aquinofroilan.tessera.dto.CreateLeaveRequestRequest
import com.aquinofroilan.tessera.dto.CreateLeaveTypeRequest
import com.aquinofroilan.tessera.dto.CreatePayrollRunRequest
import com.aquinofroilan.tessera.dto.CreatePositionRequest
import com.aquinofroilan.tessera.dto.RejectLeaveRequestRequest
import com.aquinofroilan.tessera.dto.SetDepartmentParentRequest
import com.aquinofroilan.tessera.dto.TerminateEmployeeRequest
import com.aquinofroilan.tessera.dto.UpdateDepartmentRequest
import com.aquinofroilan.tessera.dto.UpdateEmployeeRequest
import com.aquinofroilan.tessera.dto.UpdateLeaveTypeRequest
import com.aquinofroilan.tessera.dto.UpdatePositionRequest
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
    ): Any = support.unwrap(payrollRunController.listPayrollRuns(status))

    @QueryMapping
    @PreAuthorize("hasAuthority('hr:read')")
    fun payrollRun(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(payrollRunController.getPayrollRun(id))

    @MutationMapping
    @PreAuthorize("hasAuthority('hr:write')")
    fun createPayrollRun(
        @Argument input: Any,
    ): Any = support.unwrap(payrollRunController.createPayrollRun(support.toRequest<CreatePayrollRunRequest>(input)))

    @MutationMapping
    @PreAuthorize("hasAuthority('hr:approve')")
    fun approvePayrollRun(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(payrollRunController.approvePayrollRun(id))

    @MutationMapping
    @PreAuthorize("hasAuthority('hr:approve')")
    fun payPayrollRun(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(payrollRunController.payPayrollRun(id))

    @MutationMapping
    @PreAuthorize("hasAuthority('hr:write')")
    fun cancelPayrollRun(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(payrollRunController.cancelPayrollRun(id))

    @QueryMapping
    @PreAuthorize("hasAuthority('hr:read')")
    fun departments(
        @Argument activeOnly: Boolean,
    ): Any = support.unwrap(departmentController.listDepartments(activeOnly))

    @QueryMapping
    @PreAuthorize("hasAuthority('hr:read')")
    fun department(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(departmentController.getDepartment(id))

    @QueryMapping
    @PreAuthorize("hasAuthority('hr:read')")
    fun departmentOrgChart(): Any = support.unwrap(departmentController.getOrgChart())

    @MutationMapping
    @PreAuthorize("hasAuthority('hr:write')")
    fun createDepartment(
        @Argument input: Any,
    ): Any = support.unwrap(departmentController.createDepartment(support.toRequest<CreateDepartmentRequest>(input)))

    @MutationMapping
    @PreAuthorize("hasAuthority('hr:write')")
    fun updateDepartment(
        @Argument id: java.util.UUID,
        @Argument input: Any,
    ): Any = support.unwrap(departmentController.updateDepartment(id, support.toRequest<UpdateDepartmentRequest>(input)))

    @MutationMapping
    @PreAuthorize("hasAuthority('hr:write')")
    fun setDepartmentParent(
        @Argument id: java.util.UUID,
        @Argument input: Any?,
    ): Any {
        val request = input?.let { support.toRequest<SetDepartmentParentRequest>(it) } ?: SetDepartmentParentRequest()
        return support.unwrap(departmentController.setParent(id, request))
    }

    @MutationMapping
    @PreAuthorize("hasAuthority('hr:write')")
    fun deactivateDepartment(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(departmentController.deactivateDepartment(id))

    @QueryMapping
    @PreAuthorize("hasAuthority('hr:read')")
    fun employees(
        @Argument status: String?,
        @Argument departmentId: java.util.UUID?,
    ): Any = support.unwrap(employeeController.listEmployees(status, departmentId))

    @QueryMapping
    @PreAuthorize("hasAuthority('hr:read')")
    fun employee(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(employeeController.getEmployee(id))

    @MutationMapping
    @PreAuthorize("hasAuthority('hr:write')")
    fun createEmployee(
        @Argument input: Any,
    ): Any = support.unwrap(employeeController.createEmployee(support.toRequest<CreateEmployeeRequest>(input)))

    @MutationMapping
    @PreAuthorize("hasAuthority('hr:write')")
    fun updateEmployee(
        @Argument id: java.util.UUID,
        @Argument input: Any,
    ): Any = support.unwrap(employeeController.updateEmployee(id, support.toRequest<UpdateEmployeeRequest>(input)))

    @MutationMapping
    @PreAuthorize("hasAuthority('hr:write')")
    fun assignEmployeeDepartment(
        @Argument id: java.util.UUID,
        @Argument departmentId: java.util.UUID?,
    ): Any = support.unwrap(employeeController.assignDepartment(id, departmentId))

    @MutationMapping
    @PreAuthorize("hasAuthority('hr:write')")
    fun placeEmployeeOnLeave(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(employeeController.placeOnLeave(id))

    @MutationMapping
    @PreAuthorize("hasAuthority('hr:write')")
    fun returnEmployeeFromLeave(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(employeeController.returnFromLeave(id))

    @MutationMapping
    @PreAuthorize("hasAuthority('hr:write')")
    fun terminateEmployee(
        @Argument id: java.util.UUID,
        @Argument input: Any,
    ): Any = support.unwrap(employeeController.terminate(id, support.toRequest<TerminateEmployeeRequest>(input)))

    @QueryMapping
    @PreAuthorize("hasAuthority('hr:read')")
    fun positions(
        @Argument activeOnly: Boolean,
    ): Any = support.unwrap(positionController.listPositions(activeOnly))

    @QueryMapping
    @PreAuthorize("hasAuthority('hr:read')")
    fun position(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(positionController.getPosition(id))

    @MutationMapping
    @PreAuthorize("hasAuthority('hr:write')")
    fun createPosition(
        @Argument input: Any,
    ): Any = support.unwrap(positionController.createPosition(support.toRequest<CreatePositionRequest>(input)))

    @MutationMapping
    @PreAuthorize("hasAuthority('hr:write')")
    fun updatePosition(
        @Argument id: java.util.UUID,
        @Argument input: Any,
    ): Any = support.unwrap(positionController.updatePosition(id, support.toRequest<UpdatePositionRequest>(input)))

    @MutationMapping
    @PreAuthorize("hasAuthority('hr:write')")
    fun deactivatePosition(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(positionController.deactivatePosition(id))

    @QueryMapping
    @PreAuthorize("hasAuthority('hr:read')")
    fun leaveTypes(
        @Argument activeOnly: Boolean,
    ): Any = support.unwrap(leaveTypeController.listLeaveTypes(activeOnly))

    @QueryMapping
    @PreAuthorize("hasAuthority('hr:read')")
    fun leaveType(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(leaveTypeController.getLeaveType(id))

    @MutationMapping
    @PreAuthorize("hasAuthority('hr:write')")
    fun createLeaveType(
        @Argument input: Any,
    ): Any = support.unwrap(leaveTypeController.createLeaveType(support.toRequest<CreateLeaveTypeRequest>(input)))

    @MutationMapping
    @PreAuthorize("hasAuthority('hr:write')")
    fun updateLeaveType(
        @Argument id: java.util.UUID,
        @Argument input: Any,
    ): Any = support.unwrap(leaveTypeController.updateLeaveType(id, support.toRequest<UpdateLeaveTypeRequest>(input)))

    @MutationMapping
    @PreAuthorize("hasAuthority('hr:write')")
    fun deactivateLeaveType(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(leaveTypeController.deactivateLeaveType(id))

    @QueryMapping
    @PreAuthorize("hasAuthority('hr:read')")
    fun leaveRequests(
        @Argument employeeId: java.util.UUID?,
        @Argument status: String?,
    ): Any = support.unwrap(leaveRequestController.listLeaveRequests(employeeId, status))

    @QueryMapping
    @PreAuthorize("hasAuthority('hr:read')")
    fun leaveRequest(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(leaveRequestController.getLeaveRequest(id))

    @QueryMapping
    @PreAuthorize("hasAuthority('hr:read')")
    fun leaveBalance(
        @Argument employeeId: java.util.UUID,
        @Argument leaveTypeId: java.util.UUID,
        @Argument year: Int?,
    ): Any = support.unwrap(leaveRequestController.balance(employeeId, leaveTypeId, year))

    @MutationMapping
    @PreAuthorize("hasAuthority('hr:write')")
    fun createLeaveRequest(
        @Argument input: Any,
    ): Any = support.unwrap(leaveRequestController.createLeaveRequest(support.toRequest<CreateLeaveRequestRequest>(input)))

    @MutationMapping
    @PreAuthorize("hasAuthority('hr:approve')")
    fun approveLeaveRequest(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(leaveRequestController.approveLeaveRequest(id))

    @MutationMapping
    @PreAuthorize("hasAuthority('hr:approve')")
    fun rejectLeaveRequest(
        @Argument id: java.util.UUID,
        @Argument input: Any?,
    ): Any = support.unwrap(leaveRequestController.rejectLeaveRequest(id, input?.let { support.toRequest<RejectLeaveRequestRequest>(it) }))

    @MutationMapping
    @PreAuthorize("hasAuthority('hr:write')")
    fun cancelLeaveRequest(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(leaveRequestController.cancelLeaveRequest(id))

    @QueryMapping
    @PreAuthorize("hasAuthority('hr:read')")
    fun employeeCompensations(
        @Argument employeeId: java.util.UUID,
    ): Any = support.unwrap(employeeCompensationController.listCompensation(employeeId))

    @QueryMapping
    @PreAuthorize("hasAuthority('hr:read')")
    fun currentEmployeeCompensation(
        @Argument employeeId: java.util.UUID,
        @Argument asOf: String?,
    ): Any = support.unwrap(employeeCompensationController.currentCompensation(employeeId, asOf?.let(LocalDate::parse)))

    @MutationMapping
    @PreAuthorize("hasAuthority('hr:write')")
    fun addEmployeeCompensation(
        @Argument employeeId: java.util.UUID,
        @Argument input: Any,
    ): Any =
        support.unwrap(
            employeeCompensationController.addCompensation(employeeId, support.toRequest<CreateEmployeeCompensationRequest>(input)),
        )
}
