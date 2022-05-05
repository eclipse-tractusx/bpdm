package com.catenax.gpdm.dto.response

import com.catenax.gpdm.dto.response.type.TypeKeyNameDto
import com.neovisionaries.i18n.CurrencyCode
import io.swagger.v3.oas.annotations.media.Schema
import java.util.*

@Schema(name = "Bank Account Response", description = "Bank account record for a business partner")
data class BankAccountResponse (
    @Schema(description = "Unique identifier for reference purposes")
    val uuid: UUID,
    @Schema(description = "Trust scores for the account", defaultValue = "[]")
    val trustScores: Collection<Float> = emptyList(),
    @Schema(description = "Used currency in the account", defaultValue = "UNDEFINED")
    val currency: TypeKeyNameDto<CurrencyCode>,
    @Schema(description = "ID used to identify this account internationally")
    val internationalBankAccountIdentifier: String,
    @Schema(description = "ID used to identify the account's bank internationally")
    val internationalBankIdentifier: String,
    @Schema(description = "ID used to identify the account domestically")
    val nationalBankAccountIdentifier: String,
    @Schema(description = "ID used to identify the account's bank domestically")
    val nationalBankIdentifier: String
    )