package com.aquinofroilan.tessera.util

import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

private const val DEFAULT_PAGE_SIZE = 20
private const val MAX_PAGE_SIZE = 100

/** Builds a pageable only when the caller supplied pagination parameters. */
fun optionalPageable(
    page: Int?,
    size: Int?,
    sort: Sort = Sort.unsorted(),
): Pageable {
    if (page == null && size == null) return Pageable.unpaged(sort)

    val pageNumber = page ?: 0
    val pageSize = size ?: DEFAULT_PAGE_SIZE
    if (pageNumber < 0) {
        throw ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be zero or greater")
    }
    if (pageSize !in 1..MAX_PAGE_SIZE) {
        throw ResponseStatusException(HttpStatus.BAD_REQUEST, "size must be between 1 and $MAX_PAGE_SIZE")
    }
    if (pageNumber.toLong() * pageSize > Int.MAX_VALUE) {
        throw ResponseStatusException(HttpStatus.BAD_REQUEST, "requested page offset is too large")
    }
    return PageRequest.of(pageNumber, pageSize, sort)
}

data class PaginationResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
) {
    companion object {
        fun <T : Any> from(result: Page<T>): PaginationResponse<T> =
            PaginationResponse(
                content = result.content,
                page = result.number,
                size = result.size,
                totalElements = result.totalElements,
                totalPages = result.totalPages,
            )
    }
}
