package org.eclipse.tractusx.bpdm.common.dto.cdq

data class LegalFormCdq(
    val name: String? = null,
    val url: String? = null,
    val technicalKey: String,
    val mainAbbreviation: String? = null,
    val language: LanguageCdq? = null,
    val categories: Collection<TypeNameUrlCdq> = emptyList()
)
