package com.aquinofroilan.tessera.config.export

import com.lowagie.text.Document
import com.lowagie.text.FontFactory
import com.lowagie.text.PageSize
import com.lowagie.text.Phrase
import com.lowagie.text.pdf.PdfPCell
import com.lowagie.text.pdf.PdfPTable
import com.lowagie.text.pdf.PdfWriter
import org.springframework.http.HttpOutputMessage
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class PdfHttpMessageConverter(
    objectMapper: ObjectMapper,
) : AbstractExportHttpMessageConverter(MediaType("application", "pdf"), objectMapper) {
    override fun writeEmpty(outputMessage: HttpOutputMessage) {
        val document = Document(PageSize.A4.rotate())
        PdfWriter.getInstance(document, outputMessage.body)
        document.open()
        document.add(Phrase("No data available"))
        document.close()
    }

    override fun writeExport(
        headers: List<String>,
        rows: List<Map<String, Any?>>,
        outputMessage: HttpOutputMessage,
    ) {
        val document = Document(PageSize.A4.rotate())
        PdfWriter.getInstance(document, outputMessage.body)
        document.open()

        if (headers.isEmpty()) {
            document.add(Phrase("No data available"))
            document.close()
            return
        }

        val table = PdfPTable(headers.size)
        table.widthPercentage = 100f

        val headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10f)
        val cellFont = FontFactory.getFont(FontFactory.HELVETICA, 9f)

        // Add Headers
        headers.forEach { header ->
            val cell = PdfPCell(Phrase(header, headerFont))
            table.addCell(cell)
        }

        // Add Rows
        rows.forEach { map ->
            headers.forEach { header ->
                val value = map[header]?.toString() ?: ""
                val cell = PdfPCell(Phrase(value, cellFont))
                table.addCell(cell)
            }
        }

        document.add(table)
        document.close()
    }
}
