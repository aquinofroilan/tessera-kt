package com.loom.synectix.controller

import com.loom.synectix.aspect.LoggingAspect
import com.loom.synectix.config.TestSecurityConfig
import com.loom.synectix.exception.BusinessRuleException
import com.loom.synectix.exception.ResourceNotFoundException
import com.loom.synectix.model.Product
import com.loom.synectix.model.RoleAssignment
import com.loom.synectix.model.User
import com.loom.synectix.repository.InvitationRepository
import com.loom.synectix.repository.OrganizationRepository
import com.loom.synectix.repository.PasswordResetTokenRepository
import com.loom.synectix.repository.RefreshTokenRepository
import com.loom.synectix.repository.SessionTokenRepository
import com.loom.synectix.repository.UserRepository
import com.loom.synectix.security.AuthenticationContext
import com.loom.synectix.security.RolePermissionCache
import com.loom.synectix.security.SessionContext
import com.loom.synectix.security.SynectixPermissionEvaluator
import com.loom.synectix.service.AccountService
import com.loom.synectix.service.ApiKeyService
import com.loom.synectix.service.AuthService
import com.loom.synectix.service.JournalEntryService
import com.loom.synectix.service.ProductService
import com.loom.synectix.util.TokenHasher
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

@WebMvcTest(controllers = [ProductController::class])
@Import(LoggingAspect::class, TestSecurityConfig::class, SynectixPermissionEvaluator::class)
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
            uuid = "user-123",
            username = "testuser",
            email = "test@example.com",
            firstName = "Test",
            lastName = "User",
            passwordHash = "encoded",
            organizationId = "org-123",
            roleAssignments = listOf(RoleAssignment("OWNER", "org-123")),
        )

    @BeforeEach
    fun setup() {
        setupAuthWithPermissions("inventory:read", "inventory:write")
        `when`(authenticationContext.organizationId()).thenReturn("org-123")
        `when`(authenticationContext.userId()).thenReturn("user-123")
    }

    private fun setupAuthWithPermissions(vararg permissions: String) {
        val roleAuthorities = testUser.roleAssignments.map { SimpleGrantedAuthority("ROLE_${it.role}") }
        val permissionAuthorities = permissions.map { SimpleGrantedAuthority(it) }
        val authentication = UsernamePasswordAuthenticationToken(testUser, null, roleAuthorities + permissionAuthorities)
        authentication.details = SessionContext(sessionId = "session-123", organizationId = "org-123")
        SecurityContextHolder.getContext().authentication = authentication
    }

    private fun createMockProduct() =
        Product(
            id = "prod-123",
            sku = "WIDGET-001",
            name = "Widget",
            description = "A test widget",
            category = "Hardware",
            imageUrl = "https://example.com/image.jpg",
            listPrice = BigDecimal("99.99"),
            priceCurrency = "USD",
            taxGroupId = "tax-123",
            organizationId = "org-123",
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
                            "taxGroupId": "tax-123"
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
            .listProducts(any(), org.mockito.kotlin.anyOrNull(), captor.capture(), org.mockito.kotlin.anyOrNull())
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
            .perform(get("/inventory/products/prod-123"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value("prod-123"))
            .andExpect(jsonPath("$.sku").value("WIDGET-001"))
            .andExpect(jsonPath("$.name").value("Widget"))
    }

    @Test
    fun `GET products by id should return 404 when not found`() {
        `when`(productService.getProduct(any(), any()))
            .thenThrow(ResourceNotFoundException("Product not found"))

        mockMvc
            .perform(get("/inventory/products/nonexistent"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.error").value("Product not found"))
    }

    @Test
    fun `PATCH products should return 200 when updated`() {
        val updated = createMockProduct().copy(name = "Updated Widget")
        `when`(productService.updateProduct(any(), any(), any())).thenReturn(updated)

        mockMvc
            .perform(
                patch("/inventory/products/prod-123")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name": "Updated Widget"}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value("prod-123"))
            .andExpect(jsonPath("$.name").value("Updated Widget"))
    }

    @Test
    fun `PATCH products should support partial updates`() {
        val updated = createMockProduct().copy(listPrice = BigDecimal("149.99"))
        `when`(productService.updateProduct(any(), any(), any())).thenReturn(updated)

        mockMvc
            .perform(
                patch("/inventory/products/prod-123")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"listPrice": "149.99"}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.listPrice").value(149.99))
    }

    @Test
    fun `DELETE products should return 200 when soft deleted`() {
        val deleted = createMockProduct().copy(isActive = false)
        `when`(productService.deleteProduct(any(), any())).thenReturn(deleted)

        mockMvc
            .perform(delete("/inventory/products/prod-123"))
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
                patch("/inventory/products/prod-123")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name": "Updated Widget"}"""),
            ).andExpect(status().isForbidden)
    }

    @Test
    fun `DELETE products should return 403 without inventory write permission`() {
        setupAuthWithPermissions("inventory:read")

        mockMvc
            .perform(delete("/inventory/products/prod-123"))
            .andExpect(status().isForbidden)
    }
}
