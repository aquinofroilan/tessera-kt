package com.aquinofroilan.tessera.domain.platform.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CsvCodecTest {
    @Test
    fun `encode round-trips simple rows`() {
        val csv =
            CsvCodec.encode(
                listOf("sku", "name"),
                listOf(
                    mapOf("sku" to "A", "name" to "Widget"),
                    mapOf("sku" to "B", "name" to "Gadget"),
                ),
            )
        assertThat(csv).contains("sku,name").contains("A,Widget").contains("B,Gadget")
    }

    @Test
    fun `encode quotes fields containing commas and quotes`() {
        val csv =
            CsvCodec.encode(
                listOf("name"),
                listOf(mapOf("name" to "Smith, John \"Jr.\"")),
            )
        assertThat(csv).contains("\"Smith, John \"\"Jr.\"\"\"")
    }

    @Test
    fun `decode parses headers + rows including quoted commas`() {
        val csv = "sku,name\r\nA,\"Widget, Deluxe\"\r\nB,Gadget\r\n"
        val rows = CsvCodec.decode(csv)
        assertThat(rows).hasSize(2)
        assertThat(rows[0]["sku"]).isEqualTo("A")
        assertThat(rows[0]["name"]).isEqualTo("Widget, Deluxe")
        assertThat(rows[1]["name"]).isEqualTo("Gadget")
    }

    @Test
    fun `decode handles escaped quotes inside quoted field`() {
        val csv = "name\r\n\"He said \"\"hi\"\"\"\r\n"
        val rows = CsvCodec.decode(csv)
        assertThat(rows[0]["name"]).isEqualTo("He said \"hi\"")
    }

    @Test
    fun `decode skips blank trailing rows`() {
        val csv = "sku\r\nA\r\n\r\n"
        val rows = CsvCodec.decode(csv)
        assertThat(rows).hasSize(1)
    }
}
