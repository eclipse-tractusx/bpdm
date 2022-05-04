package com.catenax.gpdm.dto.request

data class IdentifiersSearchRequest(
    val idType: String,
    val idValues: List<String>
)