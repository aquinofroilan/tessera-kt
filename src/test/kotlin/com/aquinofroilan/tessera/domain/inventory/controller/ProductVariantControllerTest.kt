package com.aquinofroilan.tessera.domain.inventory.controller

import com.aquinofroilan.tessera.aspect.LoggingAspect
import com.aquinofroilan.tessera.config.TestSecurityConfig
import com.aquinofroilan.tessera.config.WebMvcConfig
import com.aquinofroilan.tessera.domain.auth.model.RoleAssignment
import com.aquinofroilan.tessera.domain.auth.model.User
import com.aquinofroilan.tessera.domain.auth.repository.InvitationRepository
import com.aquinofroilan.tessera.domain.auth.repository.PasswordResetTokenRepository
import com.aquinofroilan.tessera.domain.auth.repository.RefreshTokenRepository
import com.aquinofroilan.tessera.domain.auth.repository.SessionTokenRepository
import com.aquinofroilan.tessera.domain.auth.repository.UserRepository
import com.aquinofroilan.tessera.domain.auth.service.ApiKeyService
import com.aquinofroilan.tessera.domain.auth.service.AuthService
import com.aquinofroilan.tessera.domain.finance.service.AccountService
import com.aquinofroilan.tessera.domain.finance.service.JournalEntryService
import com.aquinofroilan.tessera.domain.inventory.dto.CreateProductVariantRequest
import com.aquinofroilan.tessera.domain.inventory.model.ProductVariant
import com.aquinofroilan.tessera.domain.inventory.service.ProductService
import com.aquinofroilan.tessera.domain.inventory.service.ProductVariantService
import com.aquinofroilan.tessera.domain.organization.repository.OrganizationRepository
import com.aquinofroilan.tessera.security.AuthenticationContext
import com.aquinofroilan.tessera.security.RolePermissionCache
import com.aquinofroilan.tessera.security.SessionContext
import com.aquinofroilan.tessera.security.TesseraPermissionEvaluator
import com.aquinofroilan.tessera.util.TokenHasher
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
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
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime
import java.util.UUID

@WebMvcTest(controllers = [ProductVariantController::class, OrganizationProductVariantController::class])
@Import(WebMvcConfig::class, LoggingAspect::class, TestSecurityConfig::class, TesseraPermissionEvaluator::class)
@ActiveProfiles("test")
class ProductVariantControllerTest {
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
    private lateinit var variantService: ProductVariantService

    @MockitoBean
    private lateinit var authenticationContext: AuthenticationContext

    private val organizationId = UUID.fromString("00000000-0000-0000-0000-000000000199")
    private val productId = UUID.fromString("00000000-0000-0000-0000-000000000123")
    private val variantId = UUID.fromString("00000000-0000-0000-0000-000000000456")
    private val testUser =
        User(
            uuid = organizationId,
            username = "testuser",
            email = "test@example.com",
            firstName = "Test",
            lastName = "User",
            passwordHash = "encoded",
            organizationId = organizationId,
            roleAssignments = listOf(RoleAssignment("OWNER", organizationId)),
        )

    @BeforeEach
    fun setup() {
        val authorities =
            testUser.roleAssignments.map { SimpleGrantedAuthority("ROLE_${it.role}") } +
                listOf("inventory:read", "inventory:write").map { SimpleGrantedAuthority(it) }
        val authentication = UsernamePasswordAuthenticationToken(testUser, null, authorities)
        authentication.details = SessionContext(sessionId = organizationId, organizationId = organizationId)
        SecurityContextHolder.getContext().authentication = authentication
        `when`(authenticationContext.organizationId()).thenReturn(organizationId)
        `when`(authenticationContext.userId()).thenReturn(organizationId)
    }

    @Test
    fun `POST variant should deserialize request and return the consistent variant shape`() {
        `when`(variantService.createVariant(any(), any(), any())).thenReturn(createMockVariant())

        mockMvc
            .perform(
                post(path())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{
                        "code": "ETCH-PMP-3200-480V",
                        "name": "High Vacuum Etch Chamber Pump",
                        "skuSuffix": "-480V",
                        "attributes": {"phase": "3", "voltage": "480V"}
                    }""",
                    ),
            ).andExpect(status().isCreated)
            .assertVariantShape()

        val requestCaptor = argumentCaptor<CreateProductVariantRequest>()
        verify(variantService).createVariant(any(), requestCaptor.capture(), any())
        org.assertj.core.api.Assertions
            .assertThat(requestCaptor.firstValue.code)
            .isEqualTo("ETCH-PMP-3200-480V")
        org.assertj.core.api.Assertions
            .assertThat(requestCaptor.firstValue.attributes)
            .containsEntry("voltage", "480V")
    }

    @Test
    fun `PUT variant should deserialize isActive and return the consistent variant shape`() {
        `when`(variantService.updateVariant(any(), any(), any())).thenReturn(createMockVariant(isActive = false))

        mockMvc
            .perform(
                put("${path()}/$variantId")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name":"Inactive Pump","isActive":false}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("High Vacuum Etch Chamber Pump"))
            .andExpect(jsonPath("$.isActive").value(false))
            .andExpect(jsonPath("$.active").doesNotExist())
            .andExpect(jsonPath("$.organizationId").value(organizationId.toString()))
            .andExpect(jsonPath("$.createdAt").exists())
            .andExpect(jsonPath("$.updatedAt").exists())

        val requestCaptor = argumentCaptor<com.aquinofroilan.tessera.domain.inventory.dto.UpdateProductVariantRequest>()
        verify(variantService).updateVariant(any(), requestCaptor.capture(), any())
        org.assertj.core.api.Assertions
            .assertThat(requestCaptor.firstValue.isActive)
            .isFalse()
    }

    @Test
    fun `GET variants should return the consistent variant shape`() {
        `when`(variantService.listVariants(any(), any())).thenReturn(listOf(createMockVariant()))

        mockMvc
            .perform(get(path()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(variantId.toString()))
            .andExpect(jsonPath("$[0].organizationId").value(organizationId.toString()))
            .andExpect(jsonPath("$[0].productId").value(productId.toString()))
            .andExpect(jsonPath("$[0].isActive").value(true))
            .andExpect(jsonPath("$[0].createdAt").exists())
            .andExpect(jsonPath("$[0].updatedAt").exists())
    }

    @Test
    fun `GET organization variants uses current organization and returns product id`() {
        `when`(variantService.listOrganizationVariants(organizationId, null, null))
            .thenReturn(listOf(createMockVariant()))

        mockMvc
            .perform(get("/api/v1/inventory/variants"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(variantId.toString()))
            .andExpect(jsonPath("$[0].organizationId").value(organizationId.toString()))
            .andExpect(jsonPath("$[0].productId").value(productId.toString()))
            .andExpect(jsonPath("$[0].code").value("ETCH-PMP-3200-480V"))

        verify(variantService).listOrganizationVariants(organizationId, null, null)
    }

    @Test
    fun `GET organization variants accepts product and active filters`() {
        `when`(variantService.listOrganizationVariants(organizationId, productId, true))
            .thenReturn(listOf(createMockVariant()))

        mockMvc
            .perform(
                get("/api/v1/inventory/variants")
                    .param("productId", productId.toString())
                    .param("isActive", "true"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$[0].productId").value(productId.toString()))

        verify(variantService).listOrganizationVariants(organizationId, productId, true)
    }

    @Test
    fun `GET variant should return the consistent variant shape`() {
        `when`(variantService.getVariant(any(), any())).thenReturn(createMockVariant())

        mockMvc
            .perform(get("${path()}/$variantId"))
            .andExpect(status().isOk)
            .assertVariantShape()
    }

    @Test
    fun `DELETE variant should return the consistent inactive variant shape`() {
        `when`(variantService.deactivateVariant(any(), any())).thenReturn(createMockVariant(isActive = false))

        mockMvc
            .perform(delete("${path()}/$variantId"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.isActive").value(false))
            .andExpect(jsonPath("$.organizationId").value(organizationId.toString()))
            .andExpect(jsonPath("$.createdAt").exists())
            .andExpect(jsonPath("$.updatedAt").exists())
    }

    private fun ResultActions.assertVariantShape(): ResultActions =
        andExpect(jsonPath("$.id").value(variantId.toString()))
            .andExpect(jsonPath("$.organizationId").value(organizationId.toString()))
            .andExpect(jsonPath("$.productId").value(productId.toString()))
            .andExpect(jsonPath("$.code").value("ETCH-PMP-3200-480V"))
            .andExpect(jsonPath("$.name").value("High Vacuum Etch Chamber Pump"))
            .andExpect(jsonPath("$.skuSuffix").value("-480V"))
            .andExpect(jsonPath("$.attributes.voltage").value("480V"))
            .andExpect(jsonPath("$.isActive").value(true))
            .andExpect(jsonPath("$.active").doesNotExist())
            .andExpect(jsonPath("$.createdAt").value("2026-11-06T10:00:01"))
            .andExpect(jsonPath("$.updatedAt").value("2026-11-06T10:00:01"))

    private fun createMockVariant(isActive: Boolean = true) =
        ProductVariant(
            id = variantId,
            organizationId = organizationId,
            productId = productId,
            code = "ETCH-PMP-3200-480V",
            name = "High Vacuum Etch Chamber Pump",
            skuSuffix = "-480V",
            attributes = mapOf("phase" to "3", "voltage" to "480V"),
            isActive = isActive,
            createdAt = LocalDateTime.parse("2026-11-06T10:00:01"),
            updatedAt = LocalDateTime.parse("2026-11-06T10:00:01"),
        )

    private fun path() = "/api/v1/inventory/products/$productId/variants"
}
