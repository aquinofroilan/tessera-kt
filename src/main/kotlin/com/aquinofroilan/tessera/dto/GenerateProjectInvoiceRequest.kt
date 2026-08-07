package com.aquinofroilan.tessera.dto

import java.time.LocalDate

/**
 * Options for generating a sales invoice from a project's billable time.
 * [revenueAccountId] falls back to the customer's default revenue account;
 * [date] defaults to today and [dueDate] to date + the customer's payment term.
 */
data class GenerateProjectInvoiceRequest(
    val revenueAccountId: java.util.UUID? = null,
    val date: LocalDate? = null,
    val dueDate: LocalDate? = null,
    val currencyCode: String? = null,
)
