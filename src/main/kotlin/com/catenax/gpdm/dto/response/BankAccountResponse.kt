package com.catenax.gpdm.dto.response

import com.catenax.gpdm.dto.response.type.TypeKeyNameDto
import com.neovisionaries.i18n.CurrencyCode
import java.util.*

data class BankAccountResponse (
    val uuid: UUID,
    val trustScores: Collection<Float>,
    val currency: TypeKeyNameDto<CurrencyCode>,
    val internationalBankAccountIdentifier: String,
    val internationalBankIdentifier: String,
    val nationalBankAccountIdentifier: String,
    val nationalBankIdentifier: String,
        )