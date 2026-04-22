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
import com.froilan.synectix.exception.AuthenticationException
import com.froilan.synectix.exception.ResourceNotFoundException
import graphql.schema.DataFetchingEnvironment
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Validator
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import tools.jackson.databind.ObjectMapper
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
    private val objectMapper: ObjectMapper,
    private val validator: Validator,
) {
    @QueryMapping
    fun health(): Any = unwrap(healthController.health())

    @QueryMapping
    fun simpleHealth(): Any = unwrap(healthController.simpleHealth())

    @QueryMapping
    fun detailedHealth(): Any = unwrap(healthController.detailedHealth())

    @QueryMapping
    fun environmentInfo(): Any = environmentController.getEnvironmentInfo()

    @QueryMapping
    fun environmentVariables(): Any = environmentController.getAllEnvironmentVariables()

    @MutationMapping
    fun signup(
        @Argument input: Any,
    ): Any = unwrap(authController.register(toRequest<RegisterRequest>(input)))

    @MutationMapping
    fun signin(
        @Argument input: Any,
        env: DataFetchingEnvironment,
    ): Any = unwrap(authController.login(toRequest<LoginRequest>(input), request(env)))

    @MutationMapping
    fun refresh(
        @Argument input: Any,
    ): Any = unwrap(authController.refresh(toRequest<RefreshRequest>(input)))

    @MutationMapping
    fun changePassword(
        @Argument input: Any,
    ): Any = unwrap(authController.changePassword(toRequest<ChangePasswordRequest>(input)))

    @MutationMapping
    fun forgotPassword(
        @Argument input: Any,
    ): Any = unwrap(authController.forgotPassword(toRequest<ForgotPasswordRequest>(input)))

    @MutationMapping
    fun resetPassword(
        @Argument input: Any,
    ): Any = unwrap(authController.resetPassword(toRequest<ResetPasswordRequest>(input)))

    @QueryMapping
    fun organizations(): Any = unwrap(authController.listOrganizations())

    @MutationMapping
    fun switchOrganization(
        @Argument input: Any,
        env: DataFetchingEnvironment,
    ): Any = unwrap(authController.switchOrganization(toRequest<SwitchOrganizationRequest>(input), request(env)))

    @MutationMapping
    fun logout(env: DataFetchingEnvironment): Any = unwrap(authController.logout(authHeader(env)))

    @QueryMapping
    fun sessions(env: DataFetchingEnvironment): Any = unwrap(sessionController.listSessions(authHeader(env).orEmpty()))

    @MutationMapping
    fun revokeSession(
        @Argument sessionId: String,
        env: DataFetchingEnvironment,
    ): Any = unwrap(sessionController.revokeSession(authHeader(env).orEmpty(), sessionId))

    @MutationMapping
    fun revokeOtherSessions(env: DataFetchingEnvironment): Any = unwrap(sessionController.revokeOtherSessions(authHeader(env).orEmpty()))

    @MutationMapping
    fun createInvitation(
        @Argument input: Any,
    ): Any = unwrap(invitationController.createInvitation(toRequest<CreateInvitationRequest>(input)))

    @QueryMapping
    fun invitations(): Any = unwrap(invitationController.listInvitations())

    @MutationMapping
    fun validateInvitation(
        @Argument input: Any,
    ): Any = unwrap(invitationController.validateInvitation(toRequest<ValidateInvitationRequest>(input)))

    @MutationMapping
    fun acceptInvitation(
        @Argument input: Any,
    ): Any = unwrap(invitationController.acceptInvitation(toRequest<AcceptInvitationRequest>(input)))

    @MutationMapping
    fun revokeInvitation(
        @Argument id: String,
    ): Any = unwrap(invitationController.revokeInvitation(id))

    @MutationMapping
    fun createApiKey(
        @Argument input: Any,
    ): Any = unwrap(apiKeyController.createApiKey(toRequest<CreateApiKeyRequest>(input)))

    @QueryMapping
    fun apiKeys(): Any = unwrap(apiKeyController.listApiKeys())

    @MutationMapping
    fun revokeApiKey(
        @Argument id: String,
    ): Any = unwrap(apiKeyController.revokeApiKey(id))

    @MutationMapping
    fun createAccount(
        @Argument input: Any,
    ): Any = unwrap(accountController.createAccount(toRequest<CreateAccountRequest>(input)))

    @QueryMapping
    fun accounts(
        @Argument type: String?,
        @Argument parentId: String?,
    ): Any = unwrap(accountController.listAccounts(type, parentId))

    @QueryMapping
    fun account(
        @Argument id: String,
    ): Any = unwrap(accountController.getAccount(id))

    @MutationMapping
    fun updateAccount(
        @Argument id: String,
        @Argument input: Any,
    ): Any = unwrap(accountController.updateAccount(id, toRequest<UpdateAccountRequest>(input)))

    @MutationMapping
    fun deleteAccount(
        @Argument id: String,
    ): Any = unwrap(accountController.deleteAccount(id))

    @QueryMapping
    fun accountBalance(
        @Argument id: String,
        @Argument asOfDate: String?,
    ): Any = unwrap(accountController.getAccountBalance(id, asOfDate?.let(LocalDate::parse)))

    @MutationMapping
    fun createFiscalYear(
        @Argument input: Any,
    ): Any = unwrap(fiscalYearController.createFiscalYear(toRequest<CreateFiscalYearRequest>(input)))

    @QueryMapping
    fun fiscalYears(): Any = unwrap(fiscalYearController.listFiscalYears())

    @QueryMapping
    fun fiscalYear(
        @Argument id: String,
    ): Any = unwrap(fiscalYearController.getFiscalYear(id))

    @MutationMapping
    fun closePeriod(
        @Argument id: String,
        @Argument periodId: String,
    ): Any = unwrap(fiscalYearController.closePeriod(id, periodId))

    @MutationMapping
    fun reopenPeriod(
        @Argument id: String,
        @Argument periodId: String,
    ): Any = unwrap(fiscalYearController.reopenPeriod(id, periodId))

    @MutationMapping
    fun closeYear(
        @Argument id: String,
    ): Any = unwrap(fiscalYearController.closeYear(id))

    @MutationMapping
    fun createJournalEntry(
        @Argument input: Any,
    ): Any = unwrap(journalEntryController.createJournalEntry(toRequest<CreateJournalEntryRequest>(input)))

    @QueryMapping
    fun journalEntries(
        @Argument status: String?,
        @Argument startDate: String?,
        @Argument endDate: String?,
    ): Any =
        unwrap(
            journalEntryController.listJournalEntries(
                status,
                startDate?.let(LocalDate::parse),
                endDate?.let(LocalDate::parse),
            ),
        )

    @QueryMapping
    fun journalEntry(
        @Argument id: String,
    ): Any = unwrap(journalEntryController.getJournalEntry(id))

    @MutationMapping
    fun postJournalEntry(
        @Argument id: String,
    ): Any = unwrap(journalEntryController.postJournalEntry(id))

    @MutationMapping
    fun voidJournalEntry(
        @Argument id: String,
        @Argument input: Any,
    ): Any = unwrap(journalEntryController.voidJournalEntry(id, toRequest<VoidJournalEntryRequest>(input)))

    @QueryMapping
    fun journalTrialBalance(
        @Argument asOfDate: String?,
    ): Any = unwrap(journalEntryController.getTrialBalance(asOfDate?.let(LocalDate::parse)))

    @QueryMapping
    fun reportTrialBalance(
        @Argument asOfDate: String?,
        @Argument compareAsOfDate: String?,
    ): Any =
        unwrap(
            financialReportController.getTrialBalance(
                asOfDate?.let(LocalDate::parse),
                compareAsOfDate?.let(LocalDate::parse),
            ),
        )

    @QueryMapping
    fun reportIncomeStatement(
        @Argument startDate: String,
        @Argument endDate: String,
        @Argument compareStartDate: String?,
        @Argument compareEndDate: String?,
    ): Any =
        unwrap(
            financialReportController.getIncomeStatement(
                LocalDate.parse(startDate),
                LocalDate.parse(endDate),
                compareStartDate?.let(LocalDate::parse),
                compareEndDate?.let(LocalDate::parse),
            ),
        )

    @QueryMapping
    fun reportBalanceSheet(
        @Argument asOfDate: String,
        @Argument compareAsOfDate: String?,
    ): Any =
        unwrap(
            financialReportController.getBalanceSheet(
                LocalDate.parse(asOfDate),
                compareAsOfDate?.let(LocalDate::parse),
            ),
        )

    @MutationMapping
    fun createTaxRate(
        @Argument input: Any,
    ): Any = unwrap(taxController.createTaxRate(toRequest<CreateTaxRateRequest>(input)))

    @QueryMapping
    fun taxRates(
        @Argument active: Boolean?,
    ): Any = unwrap(taxController.listTaxRates(active))

    @QueryMapping
    fun taxRate(
        @Argument id: String,
    ): Any = unwrap(taxController.getTaxRate(id))

    @MutationMapping
    fun updateTaxRate(
        @Argument id: String,
        @Argument input: Any,
    ): Any = unwrap(taxController.updateTaxRate(id, toRequest<UpdateTaxRateRequest>(input)))

    @MutationMapping
    fun deleteTaxRate(
        @Argument id: String,
    ): Any = unwrap(taxController.deleteTaxRate(id))

    @MutationMapping
    fun createTaxGroup(
        @Argument input: Any,
    ): Any = unwrap(taxController.createTaxGroup(toRequest<CreateTaxGroupRequest>(input)))

    @QueryMapping
    fun taxGroups(
        @Argument active: Boolean?,
    ): Any = unwrap(taxController.listTaxGroups(active))

    @QueryMapping
    fun taxGroup(
        @Argument id: String,
    ): Any = unwrap(taxController.getTaxGroup(id))

    @MutationMapping
    fun updateTaxGroup(
        @Argument id: String,
        @Argument input: Any,
    ): Any = unwrap(taxController.updateTaxGroup(id, toRequest<UpdateTaxGroupRequest>(input)))

    @MutationMapping
    fun deleteTaxGroup(
        @Argument id: String,
    ): Any = unwrap(taxController.deleteTaxGroup(id))

    @QueryMapping
    fun taxSummary(
        @Argument startDate: String,
        @Argument endDate: String,
    ): Any = unwrap(taxController.getTaxSummary(LocalDate.parse(startDate), LocalDate.parse(endDate)))

    @MutationMapping
    fun createVendor(
        @Argument input: Any,
    ): Any = unwrap(vendorController.createVendor(toRequest<CreateVendorRequest>(input)))

    @QueryMapping
    fun vendors(): Any = unwrap(vendorController.listVendors())

    @QueryMapping
    fun vendor(
        @Argument id: String,
    ): Any = unwrap(vendorController.getVendor(id))

    @MutationMapping
    fun updateVendor(
        @Argument id: String,
        @Argument input: Any,
    ): Any = unwrap(vendorController.updateVendor(id, toRequest<UpdateVendorRequest>(input)))

    @MutationMapping
    fun deleteVendor(
        @Argument id: String,
    ): Any = unwrap(vendorController.deleteVendor(id))

    @MutationMapping
    fun createBill(
        @Argument input: Any,
    ): Any = unwrap(billController.createBill(toRequest<CreateBillRequest>(input)))

    @QueryMapping
    fun bills(
        @Argument status: String?,
        @Argument vendorId: String?,
    ): Any = unwrap(billController.listBills(status, vendorId))

    @QueryMapping
    fun bill(
        @Argument id: String,
    ): Any = unwrap(billController.getBill(id))

    @MutationMapping
    fun approveBill(
        @Argument id: String,
    ): Any = unwrap(billController.approveBill(id))

    @MutationMapping
    fun voidBill(
        @Argument id: String,
        @Argument input: Any,
    ): Any = unwrap(billController.voidBill(id, toRequest<VoidBillRequest>(input)))

    @MutationMapping
    fun recordBillPayment(
        @Argument id: String,
        @Argument input: Any,
    ): Any = unwrap(billController.recordPayment(id, toRequest<RecordPaymentRequest>(input)))

    @QueryMapping
    fun billPayments(
        @Argument id: String,
    ): Any = unwrap(billController.listPayments(id))

    @QueryMapping
    fun billAging(
        @Argument asOfDate: String?,
    ): Any = unwrap(billController.getAgingReport(asOfDate?.let(LocalDate::parse)))

    @MutationMapping
    fun createInvoice(
        @Argument input: Any,
    ): Any = unwrap(invoiceController.createInvoice(toRequest<CreateInvoiceRequest>(input)))

    @QueryMapping
    fun invoices(
        @Argument status: String?,
        @Argument customerId: String?,
    ): Any = unwrap(invoiceController.listInvoices(status, customerId))

    @QueryMapping
    fun invoice(
        @Argument id: String,
    ): Any = unwrap(invoiceController.getInvoice(id))

    @MutationMapping
    fun approveInvoice(
        @Argument id: String,
    ): Any = unwrap(invoiceController.approveInvoice(id))

    @MutationMapping
    fun voidInvoice(
        @Argument id: String,
        @Argument input: Any,
    ): Any = unwrap(invoiceController.voidInvoice(id, toRequest<VoidInvoiceRequest>(input)))

    @MutationMapping
    fun recordInvoiceReceipt(
        @Argument id: String,
        @Argument input: Any,
    ): Any = unwrap(invoiceController.recordReceipt(id, toRequest<RecordReceiptRequest>(input)))

    @QueryMapping
    fun invoiceReceipts(
        @Argument id: String,
    ): Any = unwrap(invoiceController.listReceipts(id))

    @QueryMapping
    fun invoiceAging(
        @Argument asOfDate: String?,
    ): Any = unwrap(invoiceController.getAgingReport(asOfDate?.let(LocalDate::parse)))

    private fun request(env: DataFetchingEnvironment): HttpServletRequest =
        env.graphQlContext.getOrDefault(HttpServletRequest::class.java, null)
            ?: throw IllegalStateException("HTTP request context is not available")

    private fun authHeader(env: DataFetchingEnvironment): String? = request(env).getHeader("Authorization")

    private inline fun <reified T : Any> toRequest(input: Any): T {
        val request = objectMapper.convertValue(input, T::class.java)
        val violations = validator.validate(request)
        if (violations.isNotEmpty()) {
            throw IllegalArgumentException(violations.first().message)
        }
        return request
    }

    private fun unwrap(response: ResponseEntity<*>): Any {
        if (response.statusCode.is2xxSuccessful) {
            return response.body ?: emptyMap<String, Any>()
        }

        val message =
            when (val body = response.body) {
                is Map<*, *> -> body["error"]?.toString() ?: body["message"]?.toString()
                else -> body?.toString()
            } ?: "Request failed with status ${response.statusCode.value()}"

        when (response.statusCode.value()) {
            401 -> throw AuthenticationException(message)
            404 -> throw ResourceNotFoundException(message)
            422 -> throw IllegalStateException(message)
            else -> throw IllegalArgumentException(message)
        }
    }
}
