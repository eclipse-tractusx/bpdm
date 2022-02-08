package com.catenax.gpdm.dto.response.type

data class TypeKeyNameDto <T>(
    val technicalKey: T,
    val name: String,
)
