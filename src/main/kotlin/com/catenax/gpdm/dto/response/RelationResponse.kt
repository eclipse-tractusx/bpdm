package com.catenax.gpdm.dto.response

import com.catenax.gpdm.dto.response.type.TypeKeyNameDto
import com.catenax.gpdm.entity.RelationClass
import com.catenax.gpdm.entity.RelationType
import java.time.LocalDateTime
import java.util.*

data class RelationResponse (
    val uuid: UUID,
    val relationClass: TypeKeyNameDto<RelationClass>,
    val type: TypeKeyNameDto<RelationType>,
    val startNode: String,
    val endNode: String,
    val startedAt: LocalDateTime?,
    val endedAt: LocalDateTime?
        )