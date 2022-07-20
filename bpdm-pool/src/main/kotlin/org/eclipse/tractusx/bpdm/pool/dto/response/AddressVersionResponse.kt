package org.eclipse.tractusx.bpdm.pool.dto.response

import com.neovisionaries.i18n.LanguageCode
import io.swagger.v3.oas.annotations.media.Schema
import org.eclipse.tractusx.bpdm.common.dto.response.type.TypeKeyNameDto
import org.eclipse.tractusx.bpdm.common.model.CharacterSet

@Schema(name = "Address Version Response", description = "Localization record of an address")
data class AddressVersionResponse (
    @Schema(description = "Character set in which the address is written")
    val characterSet: TypeKeyNameDto<CharacterSet>,
    @Schema(description = "Language in which the address is written")
    val language: TypeKeyNameDto<LanguageCode>
        )