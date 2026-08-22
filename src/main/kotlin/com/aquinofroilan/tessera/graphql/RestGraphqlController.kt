package com.aquinofroilan.tessera.graphql

import com.aquinofroilan.tessera.domain.auth.controller.ApiKeyController
import com.aquinofroilan.tessera.domain.auth.controller.AuthController
import com.aquinofroilan.tessera.domain.auth.controller.InvitationController
import com.aquinofroilan.tessera.domain.auth.controller.SessionController
import com.aquinofroilan.tessera.domain.auth.dto.ChangePasswordRequest
import com.aquinofroilan.tessera.domain.auth.dto.CreateApiKeyRequest
import com.aquinofroilan.tessera.domain.auth.dto.ForgotPasswordRequest
import com.aquinofroilan.tessera.domain.auth.dto.LoginRequest
import com.aquinofroilan.tessera.domain.auth.dto.RefreshRequest
import com.aquinofroilan.tessera.domain.auth.dto.RegisterRequest
import com.aquinofroilan.tessera.domain.auth.dto.ResetPasswordRequest
import com.aquinofroilan.tessera.domain.auth.dto.SwitchOrganizationRequest
import com.aquinofroilan.tessera.domain.finance.controller.AccountController
import com.aquinofroilan.tessera.domain.finance.controller.BillController
import com.aquinofroilan.tessera.domain.finance.controller.FinancialReportController
import com.aquinofroilan.tessera.domain.finance.controller.FiscalYearController
import com.aquinofroilan.tessera.domain.finance.controller.InvoiceController
import com.aquinofroilan.tessera.domain.finance.controller.JournalEntryController
import com.aquinofroilan.tessera.domain.finance.controller.TaxController
import com.aquinofroilan.tessera.domain.finance.dto.CreateAccountRequest
import com.aquinofroilan.tessera.domain.finance.dto.CreateBillRequest
import com.aquinofroilan.tessera.domain.finance.dto.CreateInvoiceRequest
import com.aquinofroilan.tessera.domain.finance.dto.CreateJournalEntryRequest
import com.aquinofroilan.tessera.domain.finance.dto.CreateTaxGroupRequest
import com.aquinofroilan.tessera.domain.finance.dto.CreateTaxRateRequest
import com.aquinofroilan.tessera.domain.finance.dto.RecordPaymentRequest
import com.aquinofroilan.tessera.domain.finance.dto.RecordReceiptRequest
import com.aquinofroilan.tessera.domain.finance.dto.UpdateAccountRequest
import com.aquinofroilan.tessera.domain.finance.dto.UpdateTaxGroupRequest
import com.aquinofroilan.tessera.domain.finance.dto.UpdateTaxRateRequest
import com.aquinofroilan.tessera.domain.finance.dto.VoidBillRequest
import com.aquinofroilan.tessera.domain.finance.dto.VoidInvoiceRequest
import com.aquinofroilan.tessera.domain.finance.dto.VoidJournalEntryRequest
import com.aquinofroilan.tessera.domain.organization.controller.EnvironmentController
import com.aquinofroilan.tessera.domain.platform.controller.HealthController
import com.aquinofroilan.tessera.domain.platform.dto.AcceptInvitationRequest
import com.aquinofroilan.tessera.domain.platform.dto.CreateFiscalYearRequest
import com.aquinofroilan.tessera.domain.platform.dto.CreateInvitationRequest
import com.aquinofroilan.tessera.domain.platform.dto.ValidateInvitationRequest
import com.aquinofroilan.tessera.domain.procurement.controller.VendorController
import com.aquinofroilan.tessera.domain.procurement.dto.CreateVendorRequest
import com.aquinofroilan.tessera.domain.procurement.dto.UpdateVendorRequest
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
        @Argument sessionId: java.util.UUID,
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
        @Argument id: java.util.UUID,
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
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(apiKeyController.revokeApiKey(id))

    @MutationMapping
    @PreAuthorize("hasAuthority('account:create')")
    fun createAccount(
        @Argument input: Any,
    ): Any = support.unwrap(accountController.createAccount(support.orgId(), support.toRequest<CreateAccountRequest>(input)))

    @QueryMapping
    @PreAuthorize("hasAuthority('account:read')")
    fun accounts(
        @Argument type: String?,
        @Argument parentId: java.util.UUID?,
    ): Any = support.unwrap(accountController.listAccounts(support.orgId(), type, parentId))

    @QueryMapping
    @PreAuthorize("hasAuthority('account:read')")
    fun account(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(accountController.getAccount(support.orgId(), id))

    @MutationMapping
    @PreAuthorize("hasAuthority('account:update')")
    fun updateAccount(
        @Argument id: java.util.UUID,
        @Argument input: Any,
    ): Any = support.unwrap(accountController.updateAccount(support.orgId(), id, support.toRequest<UpdateAccountRequest>(input)))

    @MutationMapping
    @PreAuthorize("hasAuthority('account:delete')")
    fun deleteAccount(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(accountController.deleteAccount(support.orgId(), id))

    @QueryMapping
    @PreAuthorize("hasAuthority('account:read')")
    fun accountBalance(
        @Argument id: java.util.UUID,
        @Argument asOfDate: String?,
    ): Any = support.unwrap(accountController.getAccountBalance(support.orgId(), id, asOfDate?.let(LocalDate::parse)))

    @MutationMapping
    @PreAuthorize("hasAuthority('fiscal:create')")
    fun createFiscalYear(
        @Argument input: Any,
    ): Any = support.unwrap(fiscalYearController.createFiscalYear(support.orgId(), support.toRequest<CreateFiscalYearRequest>(input)))

    @QueryMapping
    @PreAuthorize("hasAuthority('fiscal:read')")
    fun fiscalYears(): Any = support.unwrap(fiscalYearController.listFiscalYears(support.orgId()))

    @QueryMapping
    @PreAuthorize("hasAuthority('fiscal:read')")
    fun fiscalYear(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(fiscalYearController.getFiscalYear(support.orgId(), id))

    @MutationMapping
    @PreAuthorize("hasAuthority('fiscal:close')")
    fun closePeriod(
        @Argument id: java.util.UUID,
        @Argument periodId: java.util.UUID,
    ): Any = support.unwrap(fiscalYearController.closePeriod(support.userId(), support.orgId(), id, periodId))

    @MutationMapping
    @PreAuthorize("hasAuthority('fiscal:close')")
    fun reopenPeriod(
        @Argument id: java.util.UUID,
        @Argument periodId: java.util.UUID,
    ): Any = support.unwrap(fiscalYearController.reopenPeriod(support.userId(), support.orgId(), id, periodId))

    @MutationMapping
    @PreAuthorize("hasAuthority('fiscal:close')")
    fun closeYear(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(fiscalYearController.closeYear(support.userId(), support.orgId(), id))

    @MutationMapping
    @PreAuthorize("hasAuthority('journal:create')")
    fun createJournalEntry(
        @Argument input: Any,
    ): Any =
        support.unwrap(
            journalEntryController.createJournalEntry(
                support.orgId(),
                support.userId(),
                support.toRequest<CreateJournalEntryRequest>(input),
            ),
        )

    @QueryMapping
    @PreAuthorize("hasAuthority('journal:read')")
    fun journalEntries(
        @Argument status: String?,
        @Argument startDate: String?,
        @Argument endDate: String?,
    ): Any =
        support.unwrap(
            journalEntryController.listJournalEntries(
                support.orgId(),
                status,
                startDate?.let(LocalDate::parse),
                endDate?.let(LocalDate::parse),
            ),
        )

    @QueryMapping
    @PreAuthorize("hasAuthority('journal:read')")
    fun journalEntry(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(journalEntryController.getJournalEntry(support.orgId(), id))

    @MutationMapping
    @PreAuthorize("hasAuthority('journal:post')")
    fun postJournalEntry(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(journalEntryController.postJournalEntry(support.orgId(), id))

    @MutationMapping
    @PreAuthorize("hasAuthority('journal:void')")
    fun voidJournalEntry(
        @Argument id: java.util.UUID,
        @Argument input: Any,
    ): Any = support.unwrap(journalEntryController.voidJournalEntry(support.orgId(), id, support.toRequest<VoidJournalEntryRequest>(input)))

    @QueryMapping
    @PreAuthorize("hasAuthority('journal:read')")
    fun journalTrialBalance(
        @Argument asOfDate: String?,
    ): Any = support.unwrap(journalEntryController.getTrialBalance(support.orgId(), asOfDate?.let(LocalDate::parse)))

    @QueryMapping
    @PreAuthorize("hasAuthority('journal:read')")
    fun reportTrialBalance(
        @Argument asOfDate: String?,
        @Argument compareAsOfDate: String?,
    ): Any =
        support.unwrap(
            financialReportController.getTrialBalance(
                support.orgId(),
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
                support.orgId(),
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
                support.orgId(),
                LocalDate.parse(asOfDate),
                compareAsOfDate?.let(LocalDate::parse),
            ),
        )

    @MutationMapping
    @PreAuthorize("hasAuthority('tax:create')")
    fun createTaxRate(
        @Argument input: Any,
    ): Any = support.unwrap(taxController.createTaxRate(support.orgId(), support.toRequest<CreateTaxRateRequest>(input)))

    @QueryMapping
    @PreAuthorize("hasAuthority('tax:read')")
    fun taxRates(
        @Argument active: Boolean?,
    ): Any = support.unwrap(taxController.listTaxRates(support.orgId(), active))

    @QueryMapping
    @PreAuthorize("hasAuthority('tax:read')")
    fun taxRate(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(taxController.getTaxRate(support.orgId(), id))

    @MutationMapping
    @PreAuthorize("hasAuthority('tax:create')")
    fun updateTaxRate(
        @Argument id: java.util.UUID,
        @Argument input: Any,
    ): Any = support.unwrap(taxController.updateTaxRate(support.orgId(), id, support.toRequest<UpdateTaxRateRequest>(input)))

    @MutationMapping
    @PreAuthorize("hasAuthority('tax:delete')")
    fun deleteTaxRate(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(taxController.deleteTaxRate(support.orgId(), id))

    @MutationMapping
    @PreAuthorize("hasAuthority('tax:create')")
    fun createTaxGroup(
        @Argument input: Any,
    ): Any = support.unwrap(taxController.createTaxGroup(support.orgId(), support.toRequest<CreateTaxGroupRequest>(input)))

    @QueryMapping
    @PreAuthorize("hasAuthority('tax:read')")
    fun taxGroups(
        @Argument active: Boolean?,
    ): Any = support.unwrap(taxController.listTaxGroups(support.orgId(), active))

    @QueryMapping
    @PreAuthorize("hasAuthority('tax:read')")
    fun taxGroup(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(taxController.getTaxGroup(support.orgId(), id))

    @MutationMapping
    @PreAuthorize("hasAuthority('tax:create')")
    fun updateTaxGroup(
        @Argument id: java.util.UUID,
        @Argument input: Any,
    ): Any = support.unwrap(taxController.updateTaxGroup(support.orgId(), id, support.toRequest<UpdateTaxGroupRequest>(input)))

    @MutationMapping
    @PreAuthorize("hasAuthority('tax:delete')")
    fun deleteTaxGroup(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(taxController.deleteTaxGroup(support.orgId(), id))

    @QueryMapping
    @PreAuthorize("hasAuthority('tax:read')")
    fun taxSummary(
        @Argument startDate: String,
        @Argument endDate: String,
    ): Any = support.unwrap(taxController.getTaxSummary(support.orgId(), LocalDate.parse(startDate), LocalDate.parse(endDate)))

    @MutationMapping
    @PreAuthorize("hasAuthority('ap:create')")
    fun createVendor(
        @Argument input: Any,
    ): Any = support.unwrap(vendorController.createVendor(support.orgId(), support.toRequest<CreateVendorRequest>(input)))

    @QueryMapping
    @PreAuthorize("hasAuthority('ap:read')")
    fun vendors(): Any = support.unwrap(vendorController.listVendors(support.orgId()))

    @QueryMapping
    @PreAuthorize("hasAuthority('ap:read')")
    fun vendor(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(vendorController.getVendor(support.orgId(), id))

    @MutationMapping
    @PreAuthorize("hasAuthority('ap:create')")
    fun updateVendor(
        @Argument id: java.util.UUID,
        @Argument input: Any,
    ): Any = support.unwrap(vendorController.updateVendor(support.orgId(), id, support.toRequest<UpdateVendorRequest>(input)))

    @MutationMapping
    @PreAuthorize("hasAuthority('ap:create')")
    fun deleteVendor(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(vendorController.deleteVendor(support.orgId(), id))

    @MutationMapping
    @PreAuthorize("hasAuthority('ap:create')")
    fun createBill(
        @Argument input: Any,
    ): Any = support.unwrap(billController.createBill(support.orgId(), support.userId(), support.toRequest<CreateBillRequest>(input)))

    @QueryMapping
    @PreAuthorize("hasAuthority('ap:read')")
    fun bills(
        @Argument status: String?,
        @Argument vendorId: java.util.UUID?,
    ): Any = support.unwrap(billController.listBills(support.orgId(), status, vendorId))

    @QueryMapping
    @PreAuthorize("hasAuthority('ap:read')")
    fun bill(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(billController.getBill(support.orgId(), id))

    @MutationMapping
    @PreAuthorize("hasAuthority('ap:approve')")
    fun approveBill(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(billController.approveBill(support.userId(), support.orgId(), id))

    @MutationMapping
    @PreAuthorize("hasAuthority('ap:void')")
    fun voidBill(
        @Argument id: java.util.UUID,
        @Argument input: Any,
    ): Any = support.unwrap(billController.voidBill(support.userId(), support.orgId(), id, support.toRequest<VoidBillRequest>(input)))

    @MutationMapping
    @PreAuthorize("hasAuthority('ap:pay')")
    fun recordBillPayment(
        @Argument id: java.util.UUID,
        @Argument input: Any,
    ): Any =
        support.unwrap(
            billController.recordPayment(
                support.orgId(),
                support.userId(),
                id,
                support.toRequest<RecordPaymentRequest>(input),
            ),
        )

    @QueryMapping
    @PreAuthorize("hasAuthority('ap:read')")
    fun billPayments(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(billController.listPayments(support.orgId(), id))

    @QueryMapping
    @PreAuthorize("hasAuthority('ap:read')")
    fun billAging(
        @Argument asOfDate: String?,
    ): Any = support.unwrap(billController.getAgingReport(support.orgId(), asOfDate?.let(LocalDate::parse)))

    @MutationMapping
    @PreAuthorize("hasAuthority('ar:create')")
    fun createInvoice(
        @Argument input: Any,
    ): Any =
        support.unwrap(
            invoiceController.createInvoice(
                support.orgId(),
                support.userId(),
                support.toRequest<CreateInvoiceRequest>(input),
            ),
        )

    @QueryMapping
    @PreAuthorize("hasAuthority('ar:read')")
    fun invoices(
        @Argument status: String?,
        @Argument customerId: java.util.UUID?,
    ): Any = support.unwrap(invoiceController.listInvoices(support.orgId(), status, customerId))

    @QueryMapping
    @PreAuthorize("hasAuthority('ar:read')")
    fun invoice(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(invoiceController.getInvoice(support.orgId(), id))

    @MutationMapping
    @PreAuthorize("hasAuthority('ar:approve')")
    fun approveInvoice(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(invoiceController.approveInvoice(support.userId(), support.orgId(), id))

    @MutationMapping
    @PreAuthorize("hasAuthority('ar:void')")
    fun voidInvoice(
        @Argument id: java.util.UUID,
        @Argument input: Any,
    ): Any =
        support.unwrap(invoiceController.voidInvoice(support.userId(), support.orgId(), id, support.toRequest<VoidInvoiceRequest>(input)))

    @MutationMapping
    @PreAuthorize("hasAuthority('ar:receive')")
    fun recordInvoiceReceipt(
        @Argument id: java.util.UUID,
        @Argument input: Any,
    ): Any =
        support.unwrap(
            invoiceController.recordReceipt(
                support.orgId(),
                support.userId(),
                id,
                support.toRequest<RecordReceiptRequest>(input),
            ),
        )

    @QueryMapping
    @PreAuthorize("hasAuthority('ar:read')")
    fun invoiceReceipts(
        @Argument id: java.util.UUID,
    ): Any = support.unwrap(invoiceController.listReceipts(support.orgId(), id))

    @QueryMapping
    @PreAuthorize("hasAuthority('ar:read')")
    fun invoiceAging(
        @Argument asOfDate: String?,
    ): Any = support.unwrap(invoiceController.getAgingReport(support.orgId(), asOfDate?.let(LocalDate::parse)))

    private fun request(env: DataFetchingEnvironment): HttpServletRequest =
        env.graphQlContext.getOrDefault(HttpServletRequest::class.java, null)
            ?: throw IllegalStateException("HTTP request context is not available")

    private fun authHeader(env: DataFetchingEnvironment): String? = request(env).getHeader("Authorization")
}
