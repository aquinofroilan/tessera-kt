package com.aquinofroilan.tessera.graphql

import java.util.UUID
import com.aquinofroilan.tessera.config.TestSecurityConfig
import com.aquinofroilan.tessera.dto.CreateCustomerRequest
import com.aquinofroilan.tessera.model.Customer
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.security.TesseraPermissionEvaluator
import com.aquinofroilan.tessera.service.CustomerService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest
import org.springframework.context.annotation.Import
import org.springframework.graphql.test.tester.GraphQlTester
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.bean.override.mockito.MockitoBean

@GraphQlTest(controllers = [CustomerGraphqlController::class])
@Import(TestSecurityConfig::class, TesseraPermissionEvaluator::class, GraphqlExceptionResolver::class, GraphqlScalarConfig::class)
class CustomerGraphqlControllerTest {
    @Autowired
    private lateinit var graphQlTester: GraphQlTester

    @MockitoBean
    private lateinit var customerService: CustomerService

    @MockitoBean
    private lateinit var authenticationContext: AuthenticationContext

    @BeforeEach
    fun setup() {
        val authentication =
            UsernamePasswordAuthenticationToken(
                "test-user",
                null,
                listOf(
                    SimpleGrantedAuthority("ar:read"),
                    SimpleGrantedAuthority("ar:create"),
                ),
            )
        SecurityContextHolder.getContext().authentication = authentication
        `when`(authenticationContext.organizationId()).thenReturn(java.util.UUID.fromString("4abe9f6d-6df3-6e5c-953e-3695db9a5216"))
    }

    @Test
    fun `customers query should return mapped results`() {
        `when`(customerService.listCustomers(java.util.UUID.fromString("4abe9f6d-6df3-6e5c-953e-3695db9a5216")))
            .thenReturn(
                listOf(
                    Customer(
                        id = java.util.UUID.fromString("59904136-ac5d-0a41-c3a4-ddcac83b81cd"),
                        name = "Acme",
                        organizationId = java.util.UUID.fromString("4abe9f6d-6df3-6e5c-953e-3695db9a5216"),
                    ),
                ),
            )

        graphQlTester
            .document(
                """
                query {
                  customers {
                    id
                    name
                    organizationId
                  }
                }
                """.trimIndent(),
            ).execute()
            .path("customers[0].id")
            .entity(String::class.java)
            .isEqualTo("59904136-ac5d-0a41-c3a4-ddcac83b81cd")
    }

    @Test
    fun `createCustomer mutation should map input and return created customer`() {
        `when`(
            customerService.createCustomer(
                CreateCustomerRequest(name = "Acme", paymentTermDays = 15),
                java.util.UUID.fromString("4abe9f6d-6df3-6e5c-953e-3695db9a5216"),
            ),
        ).thenReturn(
            Customer(
                id = java.util.UUID.fromString("e9463b75-4c1b-05e7-a5e1-26e705fb5fcb"),
                name = "Acme",
                paymentTermDays = 15,
                organizationId = java.util.UUID.fromString("4abe9f6d-6df3-6e5c-953e-3695db9a5216"),
            ),
        )

        graphQlTester
            .document(
                """
                mutation(${'$'}input: CreateCustomerInput!) {
                  createCustomer(input: ${'$'}input) {
                    id
                    name
                    paymentTermDays
                  }
                }
                """.trimIndent(),
            ).variable("input", mapOf("name" to "Acme", "paymentTermDays" to 15))
            .execute()
            .path("createCustomer.id")
            .entity(String::class.java)
            .isEqualTo("e9463b75-4c1b-05e7-a5e1-26e705fb5fcb")
    }

    @Test
    fun `createCustomer mutation should reject invalid input without calling the service`() {
        graphQlTester
            .document(
                """
                mutation(${'$'}input: CreateCustomerInput!) {
                  createCustomer(input: ${'$'}input) {
                    id
                  }
                }
                """.trimIndent(),
            ).variable("input", mapOf("name" to "", "contactEmail" to "not-an-email", "paymentTermDays" to -5))
            .execute()
            .errors()
            .satisfy { errors -> assertThat(errors).isNotEmpty() }

        verify(customerService, never()).createCustomer(any(), any())
    }

    @Test
    fun `customers query should return unauthorized error when organization is missing`() {
        `when`(authenticationContext.organizationId()).thenReturn(null)

        graphQlTester
            .document(
                """
                query {
                  customers {
                    id
                  }
                }
                """.trimIndent(),
            ).execute()
            .errors()
            .satisfy { errors ->
                assertThat(errors).anySatisfy { error ->
                    assertThat(error.message).contains("Authentication required")
                }
            }
    }
}
