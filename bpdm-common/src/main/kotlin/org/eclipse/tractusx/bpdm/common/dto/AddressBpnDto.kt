package org.eclipse.tractusx.bpdm.common.dto

import com.fasterxml.jackson.annotation.JsonUnwrapped
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import io.swagger.v3.oas.annotations.media.Schema

@JsonDeserialize(using = AddressBpnDtoDeserializer::class)
data class AddressBpnDto(
        @Schema(description = "Business Partner Number")
        val bpn: String? = null,
        @JsonUnwrapped
        val address: AddressDto
)

class AddressBpnDtoDeserializer(vc: Class<AddressBpnDto>?) : StdDeserializer<AddressBpnDto>(vc) {
        override fun deserialize(parser: JsonParser, ctxt: DeserializationContext): AddressBpnDto {
                val node = parser.codec.readTree<JsonNode>(parser)
                return AddressBpnDto(
                        node.get(AddressBpnDto::bpn.name)?.textValue(),
                        ctxt.readTreeAsValue(node, AddressDto::class.java)
                )
        }
}

