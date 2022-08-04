package org.eclipse.tractusx.bpdm.gate.dto

import com.fasterxml.jackson.annotation.JsonUnwrapped
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.dto.SiteDto

@JsonDeserialize(using = SiteGateInputDeserializer::class)
@Schema(
    name = "Site Gate Input", description = " Site with legal entity reference ."
)
data class SiteGateInput(
    @JsonUnwrapped
    val site: SiteDto,
    @Schema(description = "ID the record has in the external system where the record originates from")
    val externalId: String,
    @Schema(description = "External id of the related legal entity")
    val legalEntityExternalId: String?,
)

class SiteGateInputDeserializer(vc: Class<SiteGateInput>?) : StdDeserializer<SiteGateInput>(vc) {
    override fun deserialize(parser: JsonParser, ctxt: DeserializationContext): SiteGateInput {
        val node = parser.codec.readTree<JsonNode>(parser)
        return SiteGateInput(
            ctxt.readTreeAsValue(node, SiteDto::class.java),
            node.get(SiteGateInput::externalId.name).textValue(),
            node.get(SiteGateInput::legalEntityExternalId.name)?.textValue()
        )
    }
}