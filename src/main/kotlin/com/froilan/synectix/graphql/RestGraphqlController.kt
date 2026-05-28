package com.froilan.synectix.graphql

import com.froilan.synectix.controller.AccountController
import com.froilan.synectix.controller.ApiKeyController
import com.froilan.synectix.controller.AuthController
import com.froilan.synectix.controller.BillController
import com.froilan.synectix.controller.EnvironmentController
import com.froilan.synectix.controller.FinancialReportController
import com.froilan.synectix.controller.FiscalYearController
import com.froilan.synectix.controller.HealthController
import com.froilan.synectix.controller.InvitationController
import com.froilan.synectix.controller.InvoiceController
import com.froilan.synectix.controller.JournalEntryController
import com.froilan.synectix.controller.SessionController
import com.froilan.synectix.controller.TaxController
import com.froilan.synectix.controller.VendorController
import com.froilan.synectix.dto.AcceptInvitationRequest
import com.froilan.synectix.dto.ChangePasswordRequest
import com.froilan.synectix.dto.CreateAccountRequest
import com.froilan.synectix.dto.CreateApiKeyRequest
import com.froilan.synectix.dto.CreateBillRequest
import com.froilan.synectix.dto.CreateFiscalYearRequest
import com.froilan.synectix.dto.CreateInvitationRequest
import com.froilan.synectix.dto.CreateInvoiceRequest
import com.froilan.synectix.dto.CreateJournalEntryRequest
import com.froilan.synectix.dto.CreateTaxGroupRequest
import com.froilan.synectix.dto.CreateTaxRateRequest
import com.froilan.synectix.dto.CreateVendorRequest
import com.froilan.synectix.dto.ForgotPasswordRequest
import com.froilan.synectix.dto.LoginRequest
import com.froilan.synectix.dto.RecordPaymentRequest
import com.froilan.synectix.dto.RecordReceiptRequest
import com.froilan.synectix.dto.RefreshRequest
import com.froilan.synectix.dto.RegisterRequest
import com.froilan.synectix.dto.ResetPasswordRequest
import com.froilan.synectix.dto.SwitchOrganizationRequest
import com.froilan.synectix.dto.UpdateAccountRequest
import com.froilan.synectix.dto.UpdateTaxGroupRequest
import com.froilan.synectix.dto.UpdateTaxRateRequest
import com.froilan.synectix.dto.UpdateVendorRequest
import com.froilan.synectix.dto.ValidateInvitationRequest
import com.froilan.synectix.dto.VoidBillRequest
import com.froilan.synectix.dto.VoidInvoiceRequest
import com.froilan.synectix.dto.VoidJournalEntryRequest
import graphql.schema.DataFetchingEnvironment
import jakarta.servlet.http.HttpServletRequest
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller
import java.time.LocalDate

@Controller
class RestGraphqlController(
    private val authController: AuthController,
    private val sessionController: SessionController,
    private val invitationController: InvitationController,
    private val apiKeyController: ApiKeyController,
    private val healthController: HealthController,
    private val environmentController: EnvironmentController,
    private val accountController: AccountController,
    private val fiscalYearController: FiscalYearController,
    private val journalEntryController: JournalEntryController,
    private val financialReportController: FinancialReportController,
    private val taxController: TaxController,
    private val vendorController: VendorController,
    private val billController: BillController,
    private val invoiceController: InvoiceController,
    private val support: GraphqlBridgeSupport,
) {
    @QueryMapping
    fun health(): Any = support.unwrap(healthController.health())

    @QueryMapping
    fun simpleHealth(): Any = support.unwrap(healthController.simpleHealth())

    @QueryMapping
    fun detailedHealth(): Any = support.unwrap(healthController.detailedHealth())

    @QueryMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('environment:read')")
    fun environmentInfo(): Any = environmentController.getEnvironmentInfo()

    @QueryMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('environment:read')")
    fun environmentVariables(): Any = environmentController.getAllEnvironmentVariables()

    @MutationMapping
    fun signup(
        @Argument input: Any,
    ): Any = support.unwrap(authController.register(support.toRequest<RegisterRequest>(input)))

    @MutationMapping
    fun signin(
        @Argument input: Any,
        env: DataFetchingEnvironment,
    ): Any = support.unwrap(authController.login(support.toRequest<LoginRequest>(input), request(env)))

    @MutationMapping
    fun refresh(
        @Argument input: Any,
    ): Any = support.unwrap(authController.refresh(support.toRequest<RefreshRequest>(input)))

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    fun changePassword(
        @Argument input: Any,
    ): Any = support.unwrap(authController.changePassword(support.toRequest<ChangePasswordRequest>(input)))

    @MutationMapping
    fun forgotPassword(
        @Argument input: Any,
    ): Any = support.unwrap(authController.forgotPassword(support.toRequest<ForgotPasswordRequest>(input)))

    @MutationMapping
    fun resetPassword(
        @Argument input: Any,
    ): Any = support.unwrap(authController.resetPassword(support.toRequest<ResetPasswordRequest>(input)))

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    fun organizations(): Any = support.unwrap(authController.listOrganizations())

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    fun switchOrganization(
        @Argument input: Any,
        env: DataFetchingEnvironment,
    ): Any = support.unwrap(authController.switchOrganization(support.toRequest<SwitchOrganizationRequest>(input), request(env)))

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    fun logout(env: DataFetchingEnvironment): Any = support.unwrap(authController.logout(authHeader(env)))

    @QueryMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('session:read')")
    fun sessions(env: DataFetchingEnvironment): Any = support.unwrap(sessionController.listSessions(authHeader(env).orEmpty()))

    @MutationMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('session:delete')")
    fun revokeSession(
        @Argument sessionId: String,
        env: DataFetchingEnvironment,
    ): Any = support.unwrap(sessionController.revokeSession(authHeader(env).orEmpty(), sessionId))

    @MutationMapping
    @PreAuthorize("hasRole('SUPER_ADMIN') or hasAuthority('session:delete')")
    fun revokeOtherSessions(env: DataFetchingEnvironment): Any =
        support.unwrap(sessionController.revokeOtherSessions(authHeader(env).orEmpty()))

    @MutationMapping
    @PreAuthorize("hasAuthority('invitation:write')")
    fun createInvitation(
        @Argument input: Any,
    ): Any = support.unwrap(invitationController.createInvitation(support.toRequest<CreateInvitationRequest>(input)))

    @QueryMapping
    @PreAuthorize("hasAuthority('invitation:read')")
    fun invitations(): Any = support.unwrap(invitationController.listInvitations())

    @MutationMapping
    fun validateInvitation(
        @Argument input: Any,
    ): Any = support.unwrap(invitationController.validateInvitation(support.toRequest<ValidateInvitationRequest>(input)))

    @MutationMapping
    fun acceptInvitation(
        @Argument input: Any,
    ): Any = support.unwrap(invitationController.acceptInvitation(support.toRequest<AcceptInvitationRequest>(input)))

    @MutationMapping
    @PreAuthorize("hasAuthority('invitation:write')")
    fun revokeInvitation(
        @Argument id: String,
    ): Any = support.unwrap(invitationController.revokeInvitation(id))

    @MutationMapping
    @PreAuthorize("hasAuthority('apikey:manage')")
    fun createApiKey(
        @Argument input: Any,
    ): Any = support.unwrap(apiKeyController.createApiKey(support.toRequest<CreateApiKeyRequest>(input)))

    @QueryMapping
    @PreAuthorize("hasAuthority('apikey:manage')")
    fun apiKeys(): Any = support.unwrap(apiKeyController.listApiKeys())

    @MutationMapping
    @PreAuthorize("hasAuthority('apikey:manage')")
    fun revokeApiKey(
        @Argument id: String,
    ): Any = support.unwrap(apiKeyController.revokeApiKey(id))

    @MutationMapping
    @PreAuthorize("hasAuthority('account:create')")
    fun createAccount(
        @Argument input: Any,
    ): Any = support.unwrap(accountController.createAccount(support.toRequest<CreateAccountRequest>(input)))

    @QueryMapping
    @PreAuthorize("hasAuthority('account:read')")
    fun accounts(
        @Argument type: String?,
        @Argument parentId: String?,
    ): Any = support.unwrap(accountController.listAccounts(type, parentId))

    @QueryMapping
    @PreAuthorize("hasAuthority('account:read')")
    fun account(
        @Argument id: String,
    ): Any = support.unwrap(accountController.getAccount(id))

    @MutationMapping
    @PreAuthorize("hasAuthority('account:update')")
    fun updateAccount(
        @Argument id: String,
        @Argument input: Any,
    ): Any = support.unwrap(accountController.updateAccount(id, support.toRequest<UpdateAccountRequest>(input)))

    @MutationMapping
    @PreAuthorize("hasAuthority('account:delete')")
    fun deleteAccount(
        @Argument id: String,
    ): Any = support.unwrap(accountController.deleteAccount(id))

    @QueryMapping
    @PreAuthorize("hasAuthority('account:read')")
    fun accountBalance(
        @Argument id: String,
        @Argument asOfDate: String?,
    ): Any = support.unwrap(accountController.getAccountBalance(id, asOfDate?.let(LocalDate::parse)))

    @MutationMapping
    @PreAuthorize("hasAuthority('fiscal:create')")
    fun createFiscalYear(
        @Argument input: Any,
    ): Any = support.unwrap(fiscalYearController.createFiscalYear(support.toRequest<CreateFiscalYearRequest>(input)))

    @QueryMapping
    @PreAuthorize("hasAuthority('fiscal:read')")
    fun fiscalYears(): Any = support.unwrap(fiscalYearController.listFiscalYears())

    @QueryMapping
    @PreAuthorize("hasAuthority('fiscal:read')")
    fun fiscalYear(
        @Argument id: String,
    ): Any = support.unwrap(fiscalYearController.getFiscalYear(id))

    @MutationMapping
    @PreAuthorize("hasAuthority('fiscal:close')")
    fun closePeriod(
        @Argument id: String,
        @Argument periodId: String,
    ): Any = support.unwrap(fiscalYearController.closePeriod(id, periodId))

    @MutationMapping
    @PreAuthorize("hasAuthority('fiscal:close')")
    fun reopenPeriod(
        @Argument id: String,
        @Argument periodId: String,
    ): Any = support.unwrap(fiscalYearController.reopenPeriod(id, periodId))

    @MutationMapping
    @PreAuthorize("hasAuthority('fiscal:close')")
    fun closeYear(
        @Argument id: String,
    ): Any = support.unwrap(fiscalYearController.closeYear(id))

    @MutationMapping
    @PreAuthorize("hasAuthority('journal:create')")
    fun createJournalEntry(
        @Argument input: Any,
    ): Any = support.unwrap(journalEntryController.createJournalEntry(support.toRequest<CreateJournalEntryRequest>(input)))

    @QueryMapping
    @PreAuthorize("hasAuthority('journal:read')")
    fun journalEntries(
        @Argument status: String?,
        @Argument startDate: String?,
        @Argument endDate: String?,
    ): Any =
        support.unwrap(
            journalEntryController.listJournalEntries(
                status,
                startDate?.let(LocalDate::parse),
                endDate?.let(LocalDate::parse),
            ),
        )

    @QueryMapping
    @PreAuthorize("hasAuthority('journal:read')")
    fun journalEntry(
        @Argument id: String,
    ): Any = support.unwrap(journalEntryController.getJournalEntry(id))

    @MutationMapping
    @PreAuthorize("hasAuthority('journal:post')")
    fun postJournalEntry(
        @Argument id: String,
    ): Any = support.unwrap(journalEntryController.postJournalEntry(id))

    @MutationMapping
    @PreAuthorize("hasAuthority('journal:void')")
    fun voidJournalEntry(
        @Argument id: String,
        @Argument input: Any,
    ): Any = support.unwrap(journalEntryController.voidJournalEntry(id, support.toRequest<VoidJournalEntryRequest>(input)))

    @QueryMapping
    @PreAuthorize("hasAuthority('journal:read')")
    fun journalTrialBalance(
        @Argument asOfDate: String?,
    ): Any = support.unwrap(journalEntryController.getTrialBalance(asOfDate?.let(LocalDate::parse)))

    @QueryMapping
    @PreAuthorize("hasAuthority('journal:read')")
    fun reportTrialBalance(
        @Argument asOfDate: String?,
        @Argument compareAsOfDate: String?,
    ): Any =
        support.unwrap(
            financialReportController.getTrialBalance(
                asOfDate?.let(LocalDate::parse),
                compareAsOfDate?.let(LocalDate::parse),
            ),
        )

    @QueryMapping
    @PreAuthorize("hasAuthority('journal:read')")
    fun reportIncomeStatement(
        @Argument startDate: String,
        @Argument endDate: String,
        @Argument compareStartDate: String?,
        @Argument compareEndDate: String?,
    ): Any =
        support.unwrap(
            financialReportController.getIncomeStatement(
                LocalDate.parse(startDate),
                LocalDate.parse(endDate),
                compareStartDate?.let(LocalDate::parse),
                compareEndDate?.let(LocalDate::parse),
            ),
        )

    @QueryMapping
    @PreAuthorize("hasAuthority('journal:read')")
    fun reportBalanceSheet(
        @Argument asOfDate: String,
        @Argument compareAsOfDate: String?,
    ): Any =
        support.unwrap(
            financialReportController.getBalanceSheet(
                LocalDate.parse(asOfDate),
                compareAsOfDate?.let(LocalDate::parse),
            ),
        )

    @MutationMapping
    @PreAuthorize("hasAuthority('tax:create')")
    fun createTaxRate(
        @Argument input: Any,
    ): Any = support.unwrap(taxController.createTaxRate(support.toRequest<CreateTaxRateRequest>(input)))

    @QueryMapping
    @PreAuthorize("hasAuthority('tax:read')")
    fun taxRates(
        @Argument active: Boolean?,
    ): Any = support.unwrap(taxController.listTaxRates(active))

    @QueryMapping
    @PreAuthorize("hasAuthority('tax:read')")
    fun taxRate(
        @Argument id: String,
    ): Any = support.unwrap(taxController.getTaxRate(id))

    @MutationMapping
    @PreAuthorize("hasAuthority('tax:create')")
    fun updateTaxRate(
        @Argument id: String,
        @Argument input: Any,
    ): Any = support.unwrap(taxController.updateTaxRate(id, support.toRequest<UpdateTaxRateRequest>(input)))

    @MutationMapping
    @PreAuthorize("hasAuthority('tax:delete')")
    fun deleteTaxRate(
        @Argument id: String,
    ): Any = support.unwrap(taxController.deleteTaxRate(id))

    @MutationMapping
    @PreAuthorize("hasAuthority('tax:create')")
    fun createTaxGroup(
        @Argument input: Any,
    ): Any = support.unwrap(taxController.createTaxGroup(support.toRequest<CreateTaxGroupRequest>(input)))

    @QueryMapping
    @PreAuthorize("hasAuthority('tax:read')")
    fun taxGroups(
        @Argument active: Boolean?,
    ): Any = support.unwrap(taxController.listTaxGroups(active))

    @QueryMapping
    @PreAuthorize("hasAuthority('tax:read')")
    fun taxGroup(
        @Argument id: String,
    ): Any = support.unwrap(taxController.getTaxGroup(id))

    @MutationMapping
    @PreAuthorize("hasAuthority('tax:create')")
    fun updateTaxGroup(
        @Argument id: String,
        @Argument input: Any,
    ): Any = support.unwrap(taxController.updateTaxGroup(id, support.toRequest<UpdateTaxGroupRequest>(input)))

    @MutationMapping
    @PreAuthorize("hasAuthority('tax:delete')")
    fun deleteTaxGroup(
        @Argument id: String,
    ): Any = support.unwrap(taxController.deleteTaxGroup(id))

    @QueryMapping
    @PreAuthorize("hasAuthority('tax:read')")
    fun taxSummary(
        @Argument startDate: String,
        @Argument endDate: String,
    ): Any = support.unwrap(taxController.getTaxSummary(LocalDate.parse(startDate), LocalDate.parse(endDate)))

    @MutationMapping
    @PreAuthorize("hasAuthority('ap:create')")
    fun createVendor(
        @Argument input: Any,
    ): Any = support.unwrap(vendorController.createVendor(support.toRequest<CreateVendorRequest>(input)))

    @QueryMapping
    @PreAuthorize("hasAuthority('ap:read')")
    fun vendors(): Any = support.unwrap(vendorController.listVendors())

    @QueryMapping
    @PreAuthorize("hasAuthority('ap:read')")
    fun vendor(
        @Argument id: String,
    ): Any = support.unwrap(vendorController.getVendor(id))

    @MutationMapping
    @PreAuthorize("hasAuthority('ap:create')")
    fun updateVendor(
        @Argument id: String,
        @Argument input: Any,
    ): Any = support.unwrap(vendorController.updateVendor(id, support.toRequest<UpdateVendorRequest>(input)))

    @MutationMapping
    @PreAuthorize("hasAuthority('ap:create')")
    fun deleteVendor(
        @Argument id: String,
    ): Any = support.unwrap(vendorController.deleteVendor(id))

    @MutationMapping
    @PreAuthorize("hasAuthority('ap:create')")
    fun createBill(
        @Argument input: Any,
    ): Any = support.unwrap(billController.createBill(support.toRequest<CreateBillRequest>(input)))

    @QueryMapping
    @PreAuthorize("hasAuthority('ap:read')")
    fun bills(
        @Argument status: String?,
        @Argument vendorId: String?,
    ): Any = support.unwrap(billController.listBills(status, vendorId))

    @QueryMapping
    @PreAuthorize("hasAuthority('ap:read')")
    fun bill(
        @Argument id: String,
    ): Any = support.unwrap(billController.getBill(id))

    @MutationMapping
    @PreAuthorize("hasAuthority('ap:approve')")
    fun approveBill(
        @Argument id: String,
    ): Any = support.unwrap(billController.approveBill(id))

    @MutationMapping
    @PreAuthorize("hasAuthority('ap:void')")
    fun voidBill(
        @Argument id: String,
        @Argument input: Any,
    ): Any = support.unwrap(billController.voidBill(id, support.toRequest<VoidBillRequest>(input)))

    @MutationMapping
    @PreAuthorize("hasAuthority('ap:pay')")
    fun recordBillPayment(
        @Argument id: String,
        @Argument input: Any,
    ): Any = support.unwrap(billController.recordPayment(id, support.toRequest<RecordPaymentRequest>(input)))

    @QueryMapping
    @PreAuthorize("hasAuthority('ap:read')")
    fun billPayments(
        @Argument id: String,
    ): Any = support.unwrap(billController.listPayments(id))

    @QueryMapping
    @PreAuthorize("hasAuthority('ap:read')")
    fun billAging(
        @Argument asOfDate: String?,
    ): Any = support.unwrap(billController.getAgingReport(asOfDate?.let(LocalDate::parse)))

    @MutationMapping
    @PreAuthorize("hasAuthority('ar:create')")
    fun createInvoice(
        @Argument input: Any,
    ): Any = support.unwrap(invoiceController.createInvoice(support.toRequest<CreateInvoiceRequest>(input)))

    @QueryMapping
    @PreAuthorize("hasAuthority('ar:read')")
    fun invoices(
        @Argument status: String?,
        @Argument customerId: String?,
    ): Any = support.unwrap(invoiceController.listInvoices(status, customerId))

    @QueryMapping
    @PreAuthorize("hasAuthority('ar:read')")
    fun invoice(
        @Argument id: String,
    ): Any = support.unwrap(invoiceController.getInvoice(id))

    @MutationMapping
    @PreAuthorize("hasAuthority('ar:approve')")
    fun approveInvoice(
        @Argument id: String,
    ): Any = support.unwrap(invoiceController.approveInvoice(id))

    @MutationMapping
    @PreAuthorize("hasAuthority('ar:void')")
    fun voidInvoice(
        @Argument id: String,
        @Argument input: Any,
    ): Any = support.unwrap(invoiceController.voidInvoice(id, support.toRequest<VoidInvoiceRequest>(input)))

    @MutationMapping
    @PreAuthorize("hasAuthority('ar:receive')")
    fun recordInvoiceReceipt(
        @Argument id: String,
        @Argument input: Any,
    ): Any = support.unwrap(invoiceController.recordReceipt(id, support.toRequest<RecordReceiptRequest>(input)))

    @QueryMapping
    @PreAuthorize("hasAuthority('ar:read')")
    fun invoiceReceipts(
        @Argument id: String,
    ): Any = support.unwrap(invoiceController.listReceipts(id))

    @QueryMapping
    @PreAuthorize("hasAuthority('ar:read')")
    fun invoiceAging(
        @Argument asOfDate: String?,
    ): Any = support.unwrap(invoiceController.getAgingReport(asOfDate?.let(LocalDate::parse)))

    private fun request(env: DataFetchingEnvironment): HttpServletRequest =
        env.graphQlContext.getOrDefault(HttpServletRequest::class.java, null)
            ?: throw IllegalStateException("HTTP request context is not available")

    private fun authHeader(env: DataFetchingEnvironment): String? = request(env).getHeader("Authorization")
}
