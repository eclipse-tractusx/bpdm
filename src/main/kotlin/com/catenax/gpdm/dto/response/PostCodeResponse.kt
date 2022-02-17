package com.catenax.gpdm.dto.response

import com.catenax.gpdm.dto.response.type.TypeKeyNameUrlDto
import com.catenax.gpdm.entity.PostCodeType
import java.util.*

data class PostCodeResponse (
    val uuid: UUID,
    val value: String,
    val type: TypeKeyNameUrlDto<PostCodeType>
        )