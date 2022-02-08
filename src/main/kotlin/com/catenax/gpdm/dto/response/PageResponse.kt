package com.catenax.gpdm.dto.response

data class PageResponse<T> (
    val totalElements: Long,
    val totalPages: Int,
    val page: Int,
    val contentSize: Int,
    val content: Collection<T>
        )