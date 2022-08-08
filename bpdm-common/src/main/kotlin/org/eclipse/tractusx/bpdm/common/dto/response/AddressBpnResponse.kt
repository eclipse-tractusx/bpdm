package org.eclipse.tractusx.bpdm.common.dto.response

import com.fasterxml.jackson.annotation.JsonUnwrapped
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import io.swagger.v3.oas.annotations.media.Schema

@JsonDeserialize(using = AddressBpnResponseDeserializer::class)
@Schema(name = "Address Bpn Response", description = "Localized address record of a business partner")
data class AddressBpnResponse(
    @Schema(description = "Business Partner Number, main identifier value for addresses")
    val bpn: String,
    @JsonUnwrapped
    val address: AddressResponse
)

class AddressBpnResponseDeserializer(vc: Class<AddressBpnResponse>?) : StdDeserializer<AddressBpnResponse>(vc) {
    override fun deserialize(parser: JsonParser, ctxt: DeserializationContext): AddressBpnResponse {
        val node = parser.codec.readTree<JsonNode>(parser)
        return AddressBpnResponse(
            node.get(AddressBpnResponse::bpn.name).textValue(),
            ctxt.readTreeAsValue(node, AddressResponse::class.java)
        )
    }
}