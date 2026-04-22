package com.froilan.synectix.graphql

import com.froilan.synectix.config.TestSecurityConfig
import com.froilan.synectix.dto.CreateCustomerRequest
import com.froilan.synectix.model.Customer
import com.froilan.synectix.security.AuthenticationContext
import com.froilan.synectix.security.SynectixPermissionEvaluator
import com.froilan.synectix.service.CustomerService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.graphql.test.autoconfigure.GraphQlTest
import org.springframework.context.annotation.Import
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.graphql.test.tester.GraphQlTester

@GraphQlTest(controllers = [CustomerGraphqlController::class])
@Import(TestSecurityConfig::class, SynectixPermissionEvaluator::class, GraphqlExceptionResolver::class)
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
        `when`(authenticationContext.organizationId()).thenReturn("org-123")
    }

    @Test
    fun `customers query should return mapped results`() {
        `when`(customerService.listCustomers("org-123"))
            .thenReturn(
                listOf(
                    Customer(
                        id = "cust-1",
                        name = "Acme",
                        organizationId = "org-123",
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
            .isEqualTo("cust-1")
    }

    @Test
    fun `createCustomer mutation should map input and return created customer`() {
        `when`(customerService.createCustomer(CreateCustomerRequest(name = "Acme", paymentTermDays = 15), "org-123"))
            .thenReturn(
                Customer(
                    id = "cust-2",
                    name = "Acme",
                    paymentTermDays = 15,
                    organizationId = "org-123",
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
            .isEqualTo("cust-2")
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
