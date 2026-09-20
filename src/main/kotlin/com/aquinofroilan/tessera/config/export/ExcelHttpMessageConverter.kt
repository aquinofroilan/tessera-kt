package com.aquinofroilan.tessera.config.export

import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.http.HttpOutputMessage
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class ExcelHttpMessageConverter(
    objectMapper: ObjectMapper,
) : AbstractExportHttpMessageConverter(
        MediaType("application", "vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
        objectMapper,
    ) {
    override fun writeEmpty(outputMessage: HttpOutputMessage) {
        XSSFWorkbook().use { workbook ->
            workbook.createSheet("Export")
            workbook.write(outputMessage.body)
        }
    }

    override fun writeExport(
        headers: List<String>,
        rows: List<Map<String, Any?>>,
        outputMessage: HttpOutputMessage,
    ) {
        XSSFWorkbook().use { workbook ->
            val sheet = workbook.createSheet("Export")

            // Header Row
            val headerRow = sheet.createRow(0)
            val headerFont =
                workbook.createFont().apply {
                    bold = true
                }
            val headerStyle =
                workbook.createCellStyle().apply {
                    setFont(headerFont)
                }

            headers.forEachIndexed { colIndex, header ->
                val cell = headerRow.createCell(colIndex)
                cell.setCellValue(header)
                cell.cellStyle = headerStyle
            }

            // Data Rows
            rows.forEachIndexed { rowIndex, map ->
                val row = sheet.createRow(rowIndex + 1)
                headers.forEachIndexed { colIndex, header ->
                    val cell = row.createCell(colIndex)
                    val value = map[header]
                    if (value != null) {
                        cell.setCellValue(value.toString())
                    }
                }
            }

            // Autosize columns
            headers.indices.forEach { colIndex ->
                sheet.autoSizeColumn(colIndex)
            }

            workbook.write(outputMessage.body)
        }
    }
}
