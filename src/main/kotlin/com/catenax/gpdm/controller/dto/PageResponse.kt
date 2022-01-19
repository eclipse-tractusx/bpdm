package com.catenax.gpdm.controller.dto

data class PageResponse<T> (
    val totalElements: Long,
    val totalPages: Int,
    val page: Int,
    val contentSize: Int,
    val content: Collection<T>
        )