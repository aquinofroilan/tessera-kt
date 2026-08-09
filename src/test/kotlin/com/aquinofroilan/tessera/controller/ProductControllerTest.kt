package com.aquinofroilan.tessera.controller

import com.aquinofroilan.tessera.aspect.LoggingAspect
import com.aquinofroilan.tessera.config.TestSecurityConfig
import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.exception.ResourceNotFoundException
import com.aquinofroilan.tessera.model.Product
import com.aquinofroilan.tessera.model.RoleAssignment
import com.aquinofroilan.tessera.model.User
import com.aquinofroilan.tessera.repository.InvitationRepository
import com.aquinofroilan.tessera.repository.OrganizationRepository
import com.aquinofroilan.tessera.repository.PasswordResetTokenRepository
import com.aquinofroilan.tessera.repository.RefreshTokenRepository
import com.aquinofroilan.tessera.repository.SessionTokenRepository
import com.aquinofroilan.tessera.repository.UserRepository
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.security.RolePermissionCache
import com.aquinofroilan.tessera.security.SessionContext
import com.aquinofroilan.tessera.security.TesseraPermissionEvaluator
import com.aquinofroilan.tessera.service.AccountService
import com.aquinofroilan.tessera.service.ApiKeyService
import com.aquinofroilan.tessera.service.AuthService
import com.aquinofroilan.tessera.service.JournalEntryService
import com.aquinofroilan.tessera.service.ProductService
import com.aquinofroilan.tessera.util.TokenHasher
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@WebMvcTest(controllers = [ProductController::class])
@Import(LoggingAspect::class, TestSecurityConfig::class, TesseraPermissionEvaluator::class)
@ActiveProfiles("test")
class ProductControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var authService: AuthService

    @MockitoBean
    private lateinit var sessionTokenRepository: SessionTokenRepository

    @MockitoBean
    private lateinit var userRepository: UserRepository

    @MockitoBean
    private lateinit var organizationRepository: OrganizationRepository

    @MockitoBean
    private lateinit var refreshTokenRepository: RefreshTokenRepository

    @MockitoBean
    private lateinit var passwordResetTokenRepository: PasswordResetTokenRepository

    @MockitoBean
    private lateinit var invitationRepository: InvitationRepository

    @MockitoBean
    private lateinit var tokenHasher: TokenHasher

    @MockitoBean
    private lateinit var rolePermissionCache: RolePermissionCache

    @MockitoBean
    private lateinit var apiKeyService: ApiKeyService

    @MockitoBean
    private lateinit var accountService: AccountService

    @MockitoBean
    private lateinit var journalEntryService: JournalEntryService

    @MockitoBean
    private lateinit var productService: ProductService

    @MockitoBean
    private lateinit var authenticationContext: AuthenticationContext

    private val testUser =
        User(
            uuid = UUID.fromString("00000000-0000-0000-0000-000000000199"),
            username = "testuser",
            email = "test@example.com",
            firstName = "Test",
            lastName = "User",
            passwordHash = "encoded",
            organizationId = UUID.fromString("00000000-0000-0000-0000-000000000199"),
            roleAssignments = listOf(RoleAssignment("OWNER", UUID.fromString("00000000-0000-0000-0000-000000000199"))),
        )

    @BeforeEach
    fun setup() {
        setupAuthWithPermissions("inventory:read", "inventory:write")
        `when`(authenticationContext.organizationId()).thenReturn(UUID.fromString("00000000-0000-0000-0000-000000000199"))
        `when`(authenticationContext.userId()).thenReturn(UUID.fromString("00000000-0000-0000-0000-000000000199"))
    }

    private fun setupAuthWithPermissions(vararg permissions: String) {
        val roleAuthorities = testUser.roleAssignments.map { SimpleGrantedAuthority("ROLE_${it.role}") }
        val permissionAuthorities = permissions.map { SimpleGrantedAuthority(it) }
        val authentication = UsernamePasswordAuthenticationToken(testUser, null, roleAuthorities + permissionAuthorities)
        authentication.details =
            SessionContext(
                sessionId = UUID.fromString("00000000-0000-0000-0000-000000000199"),
                organizationId = UUID.fromString("00000000-0000-0000-0000-000000000199"),
            )
        SecurityContextHolder.getContext().authentication = authentication
    }

    private fun createMockProduct() =
        Product(
            id = UUID.fromString("00000000-0000-0000-0000-000000000199"),
            sku = "WIDGET-001",
            name = "Widget",
            description = "A test widget",
            category = "Hardware",
            imageUrl = "https://example.com/image.jpg",
            listPrice = BigDecimal("99.99"),
            priceCurrency = "USD",
            taxGroupId = UUID.fromString("00000000-0000-0000-0000-000000000199"),
            organizationId = UUID.fromString("00000000-0000-0000-0000-000000000199"),
            isActive = true,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
        )

    @Test
    fun `POST products should return 201 when created`() {
        val product = createMockProduct()
        `when`(productService.createProduct(any(), any())).thenReturn(product)

        mockMvc
            .perform(
                post("/inventory/products")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{
                            "sku": "WIDGET-001",
                            "name": "Widget",
                            "description": "A test widget",
                            "category": "Hardware",
                            "imageUrl": "https://example.com/image.jpg",
                            "listPrice": "99.99",
                            "priceCurrency": "USD",
                            "taxGroupId": "00000000-0000-0000-0000-000000000199"
                        }""",
                    ),
            ).andExpect(status().isCreated)
    }

    @Test
    fun `POST products should return 400 when SKU is blank`() {
        mockMvc
            .perform(
                post("/inventory/products")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{
                            "sku": "",
                            "name": "Widget",
                            "listPrice": "99.99"
                        }""",
                    ),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `POST products should return 400 when duplicate SKU in org`() {
        `when`(productService.createProduct(any(), any()))
            .thenThrow(BusinessRuleException("Product with SKU 'WIDGET-001' already exists in this organization"))

        mockMvc
            .perform(
                post("/inventory/products")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{
                            "sku": "WIDGET-001",
                            "name": "Widget",
                            "listPrice": "99.99"
                        }""",
                    ),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.error").value("Product with SKU 'WIDGET-001' already exists in this organization"))
    }

    @Test
    fun `GET products should return 200 with product list`() {
        val products = listOf(createMockProduct())
        `when`(productService.listProducts(any(), anyOrNull(), any(), anyOrNull())).thenReturn(products)

        mockMvc
            .perform(get("/inventory/products"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
    }

    @Test
    fun `GET products defaults isActive to true when param omitted`() {
        `when`(productService.listProducts(any(), anyOrNull(), any(), anyOrNull())).thenReturn(emptyList())

        mockMvc
            .perform(get("/inventory/products"))
            .andExpect(status().isOk)

        val captor = org.mockito.ArgumentCaptor.forClass(Boolean::class.javaObjectType)
        org.mockito.Mockito
            .verify(productService)
            .listProducts(any(), anyOrNull(), captor.capture(), anyOrNull())
        org.assertj.core.api.Assertions
            .assertThat(captor.value)
            .isTrue()
    }

    @Test
    fun `GET products should support category filter`() {
        val products = listOf(createMockProduct())
        `when`(productService.listProducts(any(), any(), any(), anyOrNull())).thenReturn(products)

        mockMvc
            .perform(get("/inventory/products").param("category", "Hardware"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
    }

    @Test
    fun `GET products should support isActive filter`() {
        val products = listOf(createMockProduct())
        `when`(productService.listProducts(any(), anyOrNull(), any(), anyOrNull())).thenReturn(products)

        mockMvc
            .perform(get("/inventory/products").param("isActive", "true"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
    }

    @Test
    fun `GET products should support search filter`() {
        val products = listOf(createMockProduct())
        `when`(productService.listProducts(any(), anyOrNull(), any(), any())).thenReturn(products)

        mockMvc
            .perform(get("/inventory/products").param("search", "WIDGET"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
    }

    @Test
    fun `GET products by id should return 200`() {
        val product = createMockProduct()
        `when`(productService.getProduct(any(), any())).thenReturn(product)

        mockMvc
            .perform(get("/inventory/products/00000000-0000-0000-0000-000000000199"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value("00000000-0000-0000-0000-000000000199"))
            .andExpect(jsonPath("$.sku").value("WIDGET-001"))
            .andExpect(jsonPath("$.name").value("Widget"))
    }

    @Test
    fun `GET products by id should return 404 when not found`() {
        `when`(productService.getProduct(any(), any()))
            .thenThrow(ResourceNotFoundException("Product not found"))

        mockMvc
            .perform(get("/inventory/products/00000000-0000-0000-0000-000000000000"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.error").value("Product not found"))
    }

    @Test
    fun `PATCH products should return 200 when updated`() {
        val updated = createMockProduct().apply { name = "Updated Widget" }
        `when`(productService.updateProduct(any(), any(), any())).thenReturn(updated)

        mockMvc
            .perform(
                patch("/inventory/products/00000000-0000-0000-0000-000000000199")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name": "Updated Widget"}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value("00000000-0000-0000-0000-000000000199"))
            .andExpect(jsonPath("$.name").value("Updated Widget"))
    }

    @Test
    fun `PATCH products should support partial updates`() {
        val updated = createMockProduct().apply { listPrice = BigDecimal("149.99") }
        `when`(productService.updateProduct(any(), any(), any())).thenReturn(updated)

        mockMvc
            .perform(
                patch("/inventory/products/00000000-0000-0000-0000-000000000199")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"listPrice": "149.99"}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.listPrice").value(149.99))
    }

    @Test
    fun `DELETE products should return 200 when soft deleted`() {
        val deleted = createMockProduct().apply { isActive = false }
        `when`(productService.deleteProduct(any(), any())).thenReturn(deleted)

        mockMvc
            .perform(delete("/inventory/products/00000000-0000-0000-0000-000000000199"))
            .andExpect(status().isOk)
    }

    @Test
    fun `POST products should return 403 without inventory write permission`() {
        setupAuthWithPermissions("inventory:read")

        mockMvc
            .perform(
                post("/inventory/products")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{
                            "sku": "WIDGET-001",
                            "name": "Widget",
                            "listPrice": "99.99"
                        }""",
                    ),
            ).andExpect(status().isForbidden)
    }

    @Test
    fun `GET products should return 403 without inventory read permission`() {
        setupAuthWithPermissions("inventory:write")

        mockMvc
            .perform(get("/inventory/products"))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `PATCH products should return 403 without inventory write permission`() {
        setupAuthWithPermissions("inventory:read")

        mockMvc
            .perform(
                patch("/inventory/products/00000000-0000-0000-0000-000000000199")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name": "Updated Widget"}"""),
            ).andExpect(status().isForbidden)
    }

    @Test
    fun `DELETE products should return 403 without inventory write permission`() {
        setupAuthWithPermissions("inventory:read")

        mockMvc
            .perform(delete("/inventory/products/00000000-0000-0000-0000-000000000199"))
            .andExpect(status().isForbidden)
    }
}
