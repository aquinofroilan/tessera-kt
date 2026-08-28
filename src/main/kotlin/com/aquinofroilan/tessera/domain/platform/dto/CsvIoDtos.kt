package com.aquinofroilan.tessera.domain.platform.dto

data class CsvImportRowError(
    val rowNumber: Int,
    val error: String,
)

data class CsvImportResult(
    val rowsParsed: Int,
    val rowsCreated: Int,
    val errors: List<CsvImportRowError>,
)
