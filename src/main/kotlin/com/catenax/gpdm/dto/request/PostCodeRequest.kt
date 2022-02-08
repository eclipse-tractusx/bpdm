package com.catenax.gpdm.dto.request

import com.catenax.gpdm.entity.PostCodeType

data class PostCodeRequest (
    val value: String,
    val type: PostCodeType
        )