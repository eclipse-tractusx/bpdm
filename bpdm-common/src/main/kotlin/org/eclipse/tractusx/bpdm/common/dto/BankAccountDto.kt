package org.eclipse.tractusx.bpdm.common.dto

import com.neovisionaries.i18n.CurrencyCode
import io.swagger.v3.oas.annotations.media.Schema

@Schema(name = "Bank Account", description = "Bank account record of a business partner")
data class BankAccountDto(
    @Schema(description = "Trust scores for the account", defaultValue = "[]")
    val trustScores: Collection<Float> = emptyList(),
    @Schema(description = "Used currency in the account", defaultValue = "UNDEFINED")
    val currency: CurrencyCode = CurrencyCode.UNDEFINED,
    @Schema(description = "ID used to identify this account internationally")
    val internationalBankAccountIdentifier: String,
    @Schema(description = "ID used to identify the account's bank internationally")
    val internationalBankIdentifier: String,
    @Schema(description = "ID used to identify the account domestically")
    val nationalBankAccountIdentifier: String,
    @Schema(description = "ID used to identify the account's bank domestically")
    val nationalBankIdentifier: String,
)
