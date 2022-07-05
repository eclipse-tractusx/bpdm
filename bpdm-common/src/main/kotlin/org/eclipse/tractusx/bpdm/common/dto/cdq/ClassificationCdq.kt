package org.eclipse.tractusx.bpdm.common.dto.cdq

data class ClassificationCdq(
    val value: String,
    val code: String? = null,
    val type: TypeKeyNameUrlCdq? = null
)
