package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.dto.CreateCustomerRequest
import com.aquinofroilan.tessera.dto.CreateProductRequest
import com.aquinofroilan.tessera.dto.CsvImportResult
import com.aquinofroilan.tessera.dto.CsvImportRowError
import org.springframework.stereotype.Service
import java.math.BigDecimal

/**
 * CSV export/import for the catalog entities a hobby ERP user is most likely
 * to bulk-edit (products, customers, vendors). Designed to round-trip with
 * Excel and Google Sheets: a CSV exported with [exportProducts] re-imports
 * cleanly with [importProducts] (modulo duplicate-SKU rejection).
 *
 * Vendors/customers can also be exported but only customers + products
 * support import in this slice -- vendor import follows when bank details
 * land.
 */
@Service
class CsvImportExportService(
    private val productService: ProductService,
    private val customerService: CustomerService,
    private val vendorService: VendorService,
) {
    private val productHeaders =
        listOf("sku", "name", "description", "category", "imageUrl", "listPrice", "priceCurrency", "taxGroupId")
    private val customerHeaders =
        listOf("name", "contactName", "contactEmail", "contactPhone", "paymentTermDays", "defaultRevenueAccountId")
    private val vendorHeaders =
        listOf("name", "contactName", "contactEmail", "contactPhone", "paymentTermDays")

    fun exportProducts(organizationId: String): ByteArray {
        val rows =
            productService
                .listProducts(organizationId)
                .map {
                    mapOf(
                        "sku" to it.sku,
                        "name" to it.name,
                        "description" to (it.description ?: ""),
                        "category" to (it.category ?: ""),
                        "imageUrl" to (it.imageUrl ?: ""),
                        "listPrice" to it.listPrice.toPlainString(),
                        "priceCurrency" to it.priceCurrency,
                        "taxGroupId" to (it.taxGroupId ?: ""),
                    )
                }
        return CsvCodec.encode(productHeaders, rows).toByteArray()
    }

    fun exportCustomers(organizationId: String): ByteArray {
        val rows =
            customerService
                .listCustomers(organizationId)
                .map {
                    mapOf(
                        "name" to it.name,
                        "contactName" to (it.contactName ?: ""),
                        "contactEmail" to (it.contactEmail ?: ""),
                        "contactPhone" to (it.contactPhone ?: ""),
                        "paymentTermDays" to it.paymentTermDays.toString(),
                        "defaultRevenueAccountId" to (it.defaultRevenueAccountId ?: ""),
                    )
                }
        return CsvCodec.encode(customerHeaders, rows).toByteArray()
    }

    fun exportVendors(organizationId: String): ByteArray {
        val rows =
            vendorService
                .listVendors(organizationId)
                .map {
                    mapOf(
                        "name" to it.name,
                        "contactName" to (it.contactName ?: ""),
                        "contactEmail" to (it.contactEmail ?: ""),
                        "contactPhone" to (it.contactPhone ?: ""),
                        "paymentTermDays" to it.paymentTermDays.toString(),
                    )
                }
        return CsvCodec.encode(vendorHeaders, rows).toByteArray()
    }

    fun importProducts(
        bytes: ByteArray,
        organizationId: String,
    ): CsvImportResult {
        val parsed = CsvCodec.decode(String(bytes))
        val errors = mutableListOf<CsvImportRowError>()
        var created = 0
        parsed.forEachIndexed { idx, row ->
            try {
                val sku = row["sku"]?.takeIf { it.isNotBlank() } ?: throw IllegalArgumentException("sku is required")
                val name = row["name"]?.takeIf { it.isNotBlank() } ?: throw IllegalArgumentException("name is required")
                val listPrice =
                    (row["listPrice"]?.takeIf { it.isNotBlank() } ?: "0").toBigDecimalOrNull()
                        ?: throw IllegalArgumentException("listPrice must be a number")
                productService.createProduct(
                    CreateProductRequest(
                        sku = sku,
                        name = name,
                        description = row["description"]?.ifBlank { null },
                        category = row["category"]?.ifBlank { null },
                        imageUrl = row["imageUrl"]?.ifBlank { null },
                        listPrice = listPrice,
                        priceCurrency = row["priceCurrency"]?.ifBlank { null },
                        taxGroupId = row["taxGroupId"]?.ifBlank { null },
                    ),
                    organizationId,
                )
                created++
            } catch (e: Exception) {
                errors.add(CsvImportRowError(rowNumber = idx + 2, error = e.message ?: e.javaClass.simpleName))
            }
        }
        return CsvImportResult(rowsParsed = parsed.size, rowsCreated = created, errors = errors)
    }

    fun importCustomers(
        bytes: ByteArray,
        organizationId: String,
    ): CsvImportResult {
        val parsed = CsvCodec.decode(String(bytes))
        val errors = mutableListOf<CsvImportRowError>()
        var created = 0
        parsed.forEachIndexed { idx, row ->
            try {
                val name = row["name"]?.takeIf { it.isNotBlank() } ?: throw IllegalArgumentException("name is required")
                customerService.createCustomer(
                    CreateCustomerRequest(
                        name = name,
                        contactName = row["contactName"]?.ifBlank { null },
                        contactEmail = row["contactEmail"]?.ifBlank { null },
                        contactPhone = row["contactPhone"]?.ifBlank { null },
                        paymentTermDays = row["paymentTermDays"]?.toIntOrNull() ?: 30,
                        defaultRevenueAccountId = row["defaultRevenueAccountId"]?.ifBlank { null },
                    ),
                    organizationId,
                )
                created++
            } catch (e: Exception) {
                errors.add(CsvImportRowError(rowNumber = idx + 2, error = e.message ?: e.javaClass.simpleName))
            }
        }
        return CsvImportResult(rowsParsed = parsed.size, rowsCreated = created, errors = errors)
    }

    private fun String.toBigDecimalOrNull(): BigDecimal? =
        try {
            BigDecimal(this)
        } catch (_: NumberFormatException) {
            null
        }
}
