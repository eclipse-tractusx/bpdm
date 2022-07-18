package org.eclipse.tractusx.bpdm.common.dto

import com.neovisionaries.i18n.LanguageCode
import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.model.CharacterSet

@Schema(name = "Address Version Request", description = "New localization record for an address")
data class AddressVersionDto(
    @Schema(description = "Character set in which the address is written", defaultValue = "UNDEFINED")
    val characterSet: CharacterSet = CharacterSet.UNDEFINED,
    @Schema(description = "Language in which the address is written", defaultValue = "undefined")
    val language: LanguageCode = LanguageCode.undefined
)