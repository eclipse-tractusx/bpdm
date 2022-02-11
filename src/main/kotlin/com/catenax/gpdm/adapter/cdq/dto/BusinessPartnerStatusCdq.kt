package com.catenax.gpdm.adapter.cdq.dto

import java.time.LocalDateTime

data class BusinessPartnerStatusCdq(
    val type: TypeKeyNameUrlCdq,
    val officialDenotation: String,
    val validFrom: LocalDateTime,
    val validUntil: LocalDateTime?,

    )
