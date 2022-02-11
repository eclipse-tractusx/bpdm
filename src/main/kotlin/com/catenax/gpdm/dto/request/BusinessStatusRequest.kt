package com.catenax.gpdm.dto.request

import com.catenax.gpdm.entity.BusinessStatusType
import java.time.LocalDateTime

data class BusinessStatusRequest (
    val officialDenotation: String,
    val validFrom: LocalDateTime,
    val validUntil: LocalDateTime?,
    val type: BusinessStatusType
        )