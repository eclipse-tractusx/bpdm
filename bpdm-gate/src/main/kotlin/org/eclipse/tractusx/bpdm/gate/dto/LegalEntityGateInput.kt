package org.eclipse.tractusx.bpdm.gate.dto

import com.fasterxml.jackson.annotation.JsonUnwrapped
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.dto.LegalEntityDto

@JsonDeserialize(using = LegalEntityGateInputDeserializer::class)
@Schema(name = "Legal Entity Gate Input", description = "Legal entity with external id")
data class LegalEntityGateInput(
    @Schema(description = "ID the record has in the external system where the record originates from", required = true)
    val externalId: String,
    @JsonUnwrapped
    val legalEntity: LegalEntityDto
)

class LegalEntityGateInputDeserializer(vc: Class<LegalEntityGateInput>?) : StdDeserializer<LegalEntityGateInput>(vc) {
    override fun deserialize(parser: JsonParser, ctxt: DeserializationContext): LegalEntityGateInput {
        val node = parser.codec.readTree<JsonNode>(parser)
        return LegalEntityGateInput(
            node.get(LegalEntityGateInput::externalId.name).textValue(),
            ctxt.readTreeAsValue(node, LegalEntityDto::class.java)
        )
    }
}
