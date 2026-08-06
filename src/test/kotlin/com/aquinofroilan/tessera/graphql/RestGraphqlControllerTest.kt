package com.aquinofroilan.tessera.graphql

import java.util.UUID
import com.aquinofroilan.tessera.config.TestSecurityConfig
import com.aquinofroilan.tessera.controller.AccountController
import com.aquinofroilan.tessera.controller.ApiKeyController
import com.aquinofroilan.tessera.controller.AuthController
import com.aquinofroilan.tessera.controller.BillController
import com.aquinofroilan.tessera.controller.EnvironmentController
import com.aquinofroilan.tessera.controller.FinancialReportController
import com.aquinofroilan.tessera.controller.FiscalYearController
import com.aquinofroilan.tessera.controller.HealthController
import com.aquinofroilan.tessera.controller.InvitationController
import com.aquinofroilan.tessera.controller.InvoiceController
import com.aquinofroilan.tessera.controller.JournalEntryController
import com.aquinofroilan.tessera.controller.SessionController
import com.aquinofroilan.tessera.controller.TaxController
import com.aquinofroilan.tessera.controller.VendorController
import com.aquinofroilan.tessera.security.TesseraPermissionEvaluator
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
    TesseraPermissionEvaluator::class,
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
