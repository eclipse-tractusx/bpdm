package com.catenax.gpdm.controller.dto

import com.catenax.gpdm.entity.RelationClass
import com.catenax.gpdm.entity.RelationType
import java.time.LocalDateTime

data class RelationDto (
    val relationClass: RelationClass,
    val type: RelationType,
    val startNode: String,
    val endNode: String,
    val startedAt: LocalDateTime?,
    val endedAt: LocalDateTime?
        )