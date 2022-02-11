package com.catenax.gpdm.dto.request

import com.catenax.gpdm.entity.PostalDeliveryPointType

data class PostalDeliveryPointRequest (
    val value: String,
    val shortName: String?,
    val number: String?,
    val type: PostalDeliveryPointType = PostalDeliveryPointType.OTHER
        )