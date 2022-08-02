package org.eclipse.tractusx.bpdm.gate.dto

import com.fasterxml.jackson.annotation.JsonUnwrapped
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.dto.response.AddressResponse

@JsonDeserialize(using = AddressGateOutputDeserializer::class)
@Schema(
    name = "Address Gate Output", description = "Address with legal entity or site references. " +
            "Only one of either legal entity or site external id can be set for an address."
)
data class AddressGateOutput(
    @JsonUnwrapped
    val address: AddressResponse,
    @Schema(description = "ID the record has in the external system where the record originates from")
    val externalId: String,
    @Schema(description = "External id of the related legal entity")
    val legalEntityExternalId: String?,
    @Schema(description = "External id of the related site")
    val siteExternalId: String?
)

class AddressGateOutputDeserializer(vc: Class<AddressGateOutput>?) : StdDeserializer<AddressGateOutput>(vc) {
    override fun deserialize(parser: JsonParser, ctxt: DeserializationContext): AddressGateOutput {
        val node = parser.codec.readTree<JsonNode>(parser)
        return AddressGateOutput(
            ctxt.readTreeAsValue(node, AddressResponse::class.java),
            node.get(AddressGateInput::externalId.name).textValue(),
            node.get(AddressGateInput::legalEntityExternalId.name)?.textValue(),
            node.get(AddressGateInput::siteExternalId.name)?.textValue()
        )
    }
}