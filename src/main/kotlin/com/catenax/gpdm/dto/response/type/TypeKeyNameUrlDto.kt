package com.catenax.gpdm.dto.response.type

data class TypeKeyNameUrlDto <T> (
        val technicalKey: T,
        val name: String,
        val url: String?
        )
