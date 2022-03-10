package com.catenax.gpdm.component.cdq.dto

data class LegalFormCdq(
    val name: String,
    val url: String?,
    val technicalKey: String,
    val mainAbbreviation: String?,
    val language: LanguageCdq?,
)
