package org.eclipse.tractusx.bpdm.pool.component.cdq.dto

import java.time.LocalDateTime

data class RelationCdq(
    val startedAt: LocalDateTime?,
    val endedAt: LocalDateTime?,
    val type: TypeKeyNameCdq,
    val relationClass: TypeKeyNameCdq,
    val startNode: String,
    val startNodeDataSource: String,
    val endNode: String,
    val endNodeDataSource: String
)
