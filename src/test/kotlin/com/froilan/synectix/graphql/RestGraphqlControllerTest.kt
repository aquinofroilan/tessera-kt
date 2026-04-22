package com.froilan.synectix.graphql

import com.froilan.synectix.config.TestSecurityConfig
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
import com.froilan.synectix.security.SynectixPermissionEvaluator
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest
import org.springframework.context.annotation.Import
import org.springframework.graphql.test.tester.GraphQlTester
import org.springframework.http.ResponseEntity
import org.springframework.test.context.bean.override.mockito.MockitoBean

@GraphQlTest(controllers = [RestGraphqlController::class])
@Import(TestSecurityConfig::class, SynectixPermissionEvaluator::class, GraphqlExceptionResolver::class, GraphqlScalarConfig::class)
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
