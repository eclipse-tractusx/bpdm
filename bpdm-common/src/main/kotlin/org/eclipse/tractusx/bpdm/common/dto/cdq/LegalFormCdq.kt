package org.eclipse.tractusx.bpdm.common.dto.cdq

data class LegalFormCdq(
    val name: String,
    val url: String? = null,
    val technicalKey: String,
    val mainAbbreviation: String? = null,
    val language: LanguageCdq? = null,
)
