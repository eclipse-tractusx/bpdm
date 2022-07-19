package org.eclipse.tractusx.bpdm.common.dto.cdq

import java.time.LocalDateTime

data class BusinessPartnerStatusCdq(
    val type: TypeKeyNameUrlCdq,
    val officialDenotation: String,
    val validFrom: LocalDateTime,
    val validUntil: LocalDateTime? = null,

    )
