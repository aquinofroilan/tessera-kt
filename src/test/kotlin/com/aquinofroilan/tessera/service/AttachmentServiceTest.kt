package com.aquinofroilan.tessera.service

import com.aquinofroilan.tessera.exception.BusinessRuleException
import com.aquinofroilan.tessera.model.Attachment
import com.aquinofroilan.tessera.repository.AttachmentRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mockito.mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.mock.web.MockMultipartFile
import java.nio.file.Files
import java.nio.file.Path
import java.util.Optional

class AttachmentServiceTest {
    private lateinit var repository: AttachmentRepository
    private lateinit var service: AttachmentService

    @TempDir
    lateinit var tempDir: Path

    private val orgId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000001")
    private val userId = java.util.UUID.fromString("00000000-0000-0000-0000-000000000002")

    @BeforeEach
    fun setup() {
        repository = mock(AttachmentRepository::class.java)
        whenever(repository.save(any<Attachment>())).thenAnswer { it.arguments[0] }
        service = AttachmentService(repository, tempDir.toString())
    }

    @Test
    fun `upload persists metadata and writes bytes to disk`() {
        val file = MockMultipartFile("file", "resume.pdf", "application/pdf", "PDF DATA".toByteArray())
        val a = service.upload(file, "candidate", java.util.UUID.fromString("00000000-0000-0000-0000-000000000003"), orgId, userId)
        assertThat(a.filename).isEqualTo("resume.pdf")
        assertThat(a.mimeType).isEqualTo("application/pdf")
        assertThat(a.sizeBytes).isEqualTo("PDF DATA".length.toLong())
        val expected = tempDir.resolve(a.storageKey)
        assertThat(Files.exists(expected)).isTrue
        assertThat(Files.readString(expected)).isEqualTo("PDF DATA")
    }

    @Test
    fun `upload rejects empty file`() {
        val empty = MockMultipartFile("file", "x.pdf", "application/pdf", ByteArray(0))
        assertThatThrownBy {
            service.upload(
                empty,
                "candidate",
                java.util.UUID.fromString("00000000-0000-0000-0000-000000000003"),
                orgId,
                userId,
            )
        }.isInstanceOf(BusinessRuleException::class.java)
    }

    @Test
    fun `upload sanitises path-escaping filename`() {
        val file = MockMultipartFile("file", "../../etc/passwd", "text/plain", "data".toByteArray())
        val a = service.upload(file, "candidate", java.util.UUID.fromString("00000000-0000-0000-0000-000000000003"), orgId, userId)
        assertThat(a.filename).doesNotContain("/")
        assertThat(a.filename).doesNotContain("..")
    }

    @Test
    fun `delete removes both row and bytes`() {
        val file = MockMultipartFile("file", "x.txt", "text/plain", "hi".toByteArray())
        val a = service.upload(file, "candidate", java.util.UUID.fromString("00000000-0000-0000-0000-000000000003"), orgId, userId)
        val path = tempDir.resolve(a.storageKey)
        whenever(repository.findById(a.id)).thenReturn(Optional.of(a))
        service.delete(a.id, orgId)
        assertThat(Files.exists(path)).isFalse
    }
}
