package com.catenax.gpdm.dto.request

import com.neovisionaries.i18n.CurrencyCode

data class BankAccountRequest(
    val trustScores: Collection<Float>,
    val currency: CurrencyCode,
    val internationalBankAccountIdentifier: String,
    val internationalBankIdentifier: String,
    val nationalBankAccountIdentifier: String,
    val nationalBankIdentifier: String,
)
