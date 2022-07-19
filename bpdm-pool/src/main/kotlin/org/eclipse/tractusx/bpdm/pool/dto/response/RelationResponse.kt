package org.eclipse.tractusx.bpdm.pool.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.pool.dto.response.type.TypeKeyNameDto
import org.eclipse.tractusx.bpdm.pool.entity.RelationClass
import org.eclipse.tractusx.bpdm.pool.entity.RelationType
import java.time.LocalDateTime
import java.util.*

@Schema(name = "Relation Response", description = "Directed relation between two business partners")
data class RelationResponse (
    @Schema(description = "Unique identifier for reference purposes")
    val uuid: UUID,
    @Schema(description = "Class of relation like Catena, LEI or DNB relation")
    val relationClass: TypeKeyNameDto<RelationClass>,
    @Schema(description = "Type of relation like predecessor or ownership relation")
    val type: TypeKeyNameDto<RelationType>,
    @Schema(description = "BPN of partner which is the source of the relation")
    val startNode: String,
    @Schema(description = "BPN of partner which is the target of the relation")
    val endNode: String,
    @Schema(description = "Time when the relation started")
    val startedAt: LocalDateTime? = null,
    @Schema(description = "Time when the relation ended")
    val endedAt: LocalDateTime? = null
        )