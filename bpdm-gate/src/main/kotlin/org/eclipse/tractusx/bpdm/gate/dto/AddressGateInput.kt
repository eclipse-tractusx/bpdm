package org.eclipse.tractusx.bpdm.gate.dto

import com.fasterxml.jackson.annotation.JsonUnwrapped
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.dto.AddressBpnDto

@JsonDeserialize(using = AddressGateInputDeserializer::class)
@Schema(
    name = "Address Gate Input", description = "Address with legal entity or site references. " +
            "Only one of either legal entity or site external id can be set for an address."
)
data class AddressGateInput(
    @JsonUnwrapped
    val address: AddressBpnDto,
    @Schema(description = "ID the record has in the external system where the record originates from")
    val externalId: String,
    @Schema(description = "External id of the related legal entity")
    val legalEntityExternalId: String?,
    @Schema(description = "External id of the related site")
    val siteExternalId: String?
)

class AddressGateInputDeserializer(vc: Class<AddressGateInput>?) : StdDeserializer<AddressGateInput>(vc) {
    override fun deserialize(parser: JsonParser, ctxt: DeserializationContext): AddressGateInput {
        val node = parser.codec.readTree<JsonNode>(parser)
        return AddressGateInput(
            ctxt.readTreeAsValue(node, AddressBpnDto::class.java),
            node.get(AddressGateInput::externalId.name).textValue(),
            node.get(AddressGateInput::legalEntityExternalId.name)?.textValue(),
            node.get(AddressGateInput::siteExternalId.name)?.textValue()
        )
    }
}