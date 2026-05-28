package com.loom.synectix.graphql

import com.loom.synectix.config.TestSecurityConfig
import com.loom.synectix.controller.AccountController
import com.loom.synectix.controller.ApiKeyController
import com.loom.synectix.controller.AuthController
import com.loom.synectix.controller.BillController
import com.loom.synectix.controller.EnvironmentController
import com.loom.synectix.controller.FinancialReportController
import com.loom.synectix.controller.FiscalYearController
import com.loom.synectix.controller.HealthController
import com.loom.synectix.controller.InvitationController
import com.loom.synectix.controller.InvoiceController
import com.loom.synectix.controller.JournalEntryController
import com.loom.synectix.controller.SessionController
import com.loom.synectix.controller.TaxController
import com.loom.synectix.controller.VendorController
import com.loom.synectix.security.SynectixPermissionEvaluator
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest
import org.springframework.context.annotation.Import
import org.springframework.graphql.test.tester.GraphQlTester
import org.springframework.http.ResponseEntity
import org.springframework.test.context.bean.override.mockito.MockitoBean

@GraphQlTest(controllers = [RestGraphqlController::class])
@Import(
    TestSecurityConfig::class,
    SynectixPermissionEvaluator::class,
    GraphqlExceptionResolver::class,
    GraphqlScalarConfig::class,
    GraphqlBridgeSupport::class,
)
class RestGraphqlControllerTest {
    @Autowired
    private lateinit var graphQlTester: GraphQlTester

    @MockitoBean
    private lateinit var authController: AuthController

    @MockitoBean
    private lateinit var sessionController: SessionController

    @MockitoBean
    private lateinit var invitationController: InvitationController

    @MockitoBean
    private lateinit var apiKeyController: ApiKeyController

    @MockitoBean
    private lateinit var healthController: HealthController

    @MockitoBean
    private lateinit var environmentController: EnvironmentController

    @MockitoBean
    private lateinit var accountController: AccountController

    @MockitoBean
    private lateinit var fiscalYearController: FiscalYearController

    @MockitoBean
    private lateinit var journalEntryController: JournalEntryController

    @MockitoBean
    private lateinit var financialReportController: FinancialReportController

    @MockitoBean
    private lateinit var taxController: TaxController

    @MockitoBean
    private lateinit var vendorController: VendorController

    @MockitoBean
    private lateinit var billController: BillController

    @MockitoBean
    private lateinit var invoiceController: InvoiceController

    @Test
    fun `simpleHealth query should return json payload`() {
        `when`(healthController.simpleHealth()).thenReturn(ResponseEntity.ok(mapOf("status" to "UP")))

        graphQlTester
            .document(
                """
                query {
                  simpleHealth
                }
                """.trimIndent(),
            ).execute()
            .path("simpleHealth.status")
            .entity(String::class.java)
            .isEqualTo("UP")
    }
}
