package com.catenax.gpdm.dto.response

import com.catenax.gpdm.dto.response.type.TypeKeyNameDto
import com.catenax.gpdm.entity.CharacterSet
import com.neovisionaries.i18n.LanguageCode
import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "Address Version Response", description = "Localization record of an address")
data class AddressVersionResponse (
    @Schema(description = "Character set in which the address is written")
    val characterSet: TypeKeyNameDto<CharacterSet>,
    @Schema(description = "Language in which the address is written")
    val language: TypeKeyNameDto<LanguageCode>
        )