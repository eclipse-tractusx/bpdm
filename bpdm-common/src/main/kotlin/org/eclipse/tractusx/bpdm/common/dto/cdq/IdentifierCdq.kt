package org.eclipse.tractusx.bpdm.common.dto.cdq

data class IdentifierCdq(
    val type: TypeKeyNameUrlCdq? = null,
    val value: String,
    val issuingBody: TypeKeyNameUrlCdq? = null,
    val status: TypeKeyNameCdq? = null
)
