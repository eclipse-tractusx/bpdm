package org.eclipse.tractusx.bpdm.common.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeKeyNameDto
import org.eclipse.tractusx.bpdm.common.model.RelationClass
import org.eclipse.tractusx.bpdm.common.model.RelationType
import java.time.LocalDateTime

@Schema(name = "Relation Response", description = "Directed relation between two business partners")
data class RelationResponse (
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