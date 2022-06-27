package com.catenax.gpdm.component.cdq.dto

data class ClassificationCdq(
    val value: String,
    val code: String? = null,
    val type: TypeKeyNameUrlCdq
)
