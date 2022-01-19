package com.catenax.gpdm.controller.dto

import com.neovisionaries.i18n.CurrencyCode
import javax.persistence.*

data class BankAccountDto (
    val trustScores: Collection<Int>,
    val currencyCode: CurrencyCode,
    val internationalBankAccountIdentifier: String,
    val internationalBankIdentifier: String,
    val nationalBankAccountIdentifier: String,
    val nationalBankIdentifier: String,
        )