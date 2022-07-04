package org.eclipse.tractusx.bpdm.pool.component.cdq.dto

import java.time.LocalDateTime

data class BusinessPartnerStatusCdq(
    val type: TypeKeyNameUrlCdq,
    val officialDenotation: String,
    val validFrom: LocalDateTime,
    val validUntil: LocalDateTime? = null,

    )
