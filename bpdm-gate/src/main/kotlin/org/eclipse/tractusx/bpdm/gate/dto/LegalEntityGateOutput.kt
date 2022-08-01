package org.eclipse.tractusx.bpdm.gate.dto


import com.fasterxml.jackson.annotation.JsonUnwrapped
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.dto.response.LegalEntityResponse

@JsonDeserialize(using = LegalEntityGateOutputDeserializer::class)
@Schema(name = "Legal Entity With References Response", description = "Legal entity with references")
data class LegalEntityGateOutput(
    @JsonUnwrapped
    val legalEntity: LegalEntityResponse,
    @Schema(description = "ID the record has in the external system where the record originates from")
    val externalId: String
)

class LegalEntityGateOutputDeserializer(vc: Class<LegalEntityGateOutput>?) : StdDeserializer<LegalEntityGateOutput>(vc) {
    override fun deserialize(parser: JsonParser, ctxt: DeserializationContext): LegalEntityGateOutput {
        val node = parser.codec.readTree<JsonNode>(parser)
        return LegalEntityGateOutput(
            ctxt.readTreeAsValue(node, LegalEntityResponse::class.java),
            node.get(LegalEntityGateInput::externalId.name).textValue()
        )
    }
}