package com.aquinofroilan.tessera.config.export

import com.aquinofroilan.tessera.domain.platform.service.CsvCodec
import org.springframework.http.HttpOutputMessage
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

@Component
class CsvHttpMessageConverter(
    objectMapper: ObjectMapper,
) : AbstractExportHttpMessageConverter(MediaType("text", "csv"), objectMapper) {
    override fun writeEmpty(outputMessage: HttpOutputMessage) {
        val writer = OutputStreamWriter(outputMessage.body, StandardCharsets.UTF_8)
        writer.write("")
        writer.flush()
    }

    override fun writeExport(
        headers: List<String>,
        rows: List<Map<String, Any?>>,
        outputMessage: HttpOutputMessage,
    ) {
        val csvStr = CsvCodec.encode(headers, rows)
        val writer = OutputStreamWriter(outputMessage.body, StandardCharsets.UTF_8)
        writer.write(csvStr)
        writer.flush()
    }
}
