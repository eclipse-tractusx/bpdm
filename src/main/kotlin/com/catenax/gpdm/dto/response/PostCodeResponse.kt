package com.catenax.gpdm.dto.response

import com.catenax.gpdm.dto.response.type.TypeKeyNameUrlDto
import com.catenax.gpdm.entity.PostCodeType

data class PostCodeResponse (
    val value: String,
    val type: TypeKeyNameUrlDto<PostCodeType>
        )