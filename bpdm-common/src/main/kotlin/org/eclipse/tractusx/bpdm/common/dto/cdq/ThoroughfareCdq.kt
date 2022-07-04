package org.eclipse.tractusx.bpdm.common.dto.cdq

data class ThoroughfareCdq(
    var type: TypeKeyNameUrlCdq? = null,
    val shortName: String? = null,
    val number: String? = null,
    val value: String? = null,
    val name: String? = null,
    val direction: String? = null,
    var language: LanguageCdq? = null
)
