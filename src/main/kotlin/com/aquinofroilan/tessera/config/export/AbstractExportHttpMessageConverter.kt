package com.aquinofroilan.tessera.config.export

import org.springframework.data.domain.Page
import org.springframework.http.HttpInputMessage
import org.springframework.http.HttpOutputMessage
import org.springframework.http.MediaType
import org.springframework.http.converter.AbstractHttpMessageConverter
import tools.jackson.core.type.TypeReference
import tools.jackson.databind.ObjectMapper

abstract class AbstractExportHttpMessageConverter(
    supportedMediaType: MediaType,
    private val objectMapper: ObjectMapper,
) : AbstractHttpMessageConverter<Any>(supportedMediaType) {
    override fun supports(clazz: Class<*>): Boolean {
        // We support Lists and Pages
        return List::class.java.isAssignableFrom(clazz) || Page::class.java.isAssignableFrom(clazz)
    }

    override fun readInternal(
        clazz: Class<out Any>,
        inputMessage: HttpInputMessage,
    ): Any = throw UnsupportedOperationException("This converter only supports writing")

    override fun writeInternal(
        t: Any,
        outputMessage: HttpOutputMessage,
    ) {
        val list =
            when (t) {
                is Page<*> -> t.content
                is List<*> -> t
                else -> emptyList<Any>()
            }

        if (list.isEmpty()) {
            writeEmpty(outputMessage)
            return
        }

        val typeRef = object : TypeReference<Map<String, Any?>>() {}
        val maps = list.map { objectMapper.convertValue(it, typeRef) }

        val allHeaders = mutableSetOf<String>()
        maps.forEach { allHeaders.addAll(it.keys) }

        val headers = allHeaders.toList()

        writeExport(headers, maps, outputMessage)
    }

    protected abstract fun writeEmpty(outputMessage: HttpOutputMessage)

    protected abstract fun writeExport(
        headers: List<String>,
        rows: List<Map<String, Any?>>,
        outputMessage: HttpOutputMessage,
    )
}
