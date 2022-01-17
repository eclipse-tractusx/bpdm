package com.catenax.gpdm.controller.dto

data class PageResponse<T> (
    val totalElements: Long,
    val totalPages: Int,
    val content: Collection<T>
        )