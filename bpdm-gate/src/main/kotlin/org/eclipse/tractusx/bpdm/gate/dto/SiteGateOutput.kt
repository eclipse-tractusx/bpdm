package org.eclipse.tractusx.bpdm.gate.dto

import com.fasterxml.jackson.annotation.JsonUnwrapped
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.dto.response.SiteResponse

@JsonDeserialize(using = SiteGateOutputDeserializer::class)
@Schema(name = "Site Gate Output", description = "Site with legal entity reference.")
data class SiteGateOutput(
    @JsonUnwrapped
    val site: SiteResponse,
    @Schema(description = "ID the record has in the external system where the record originates from")
    val externalId: String,
    @Schema(description = "External id of the related legal entity")
    val legalEntityExternalId: String?,
)

class SiteGateOutputDeserializer(vc: Class<SiteGateOutput>?) : StdDeserializer<SiteGateOutput>(vc) {
    override fun deserialize(parser: JsonParser, ctxt: DeserializationContext): SiteGateOutput {
        val node = parser.codec.readTree<JsonNode>(parser)
        return SiteGateOutput(
            ctxt.readTreeAsValue(node, SiteResponse::class.java),
            node.get(SiteGateOutput::externalId.name).textValue(),
            node.get(SiteGateOutput::legalEntityExternalId.name)?.textValue()
        )
    }
}