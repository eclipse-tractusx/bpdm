package org.eclipse.tractusx.bpdm.common.dto.cdq

data class BankAccountCdq(
    val internationalBankAccountIdentifier: String,
    val internationalBankIdentifier: String,
    val nationalBankAccountIdentifier: String,
    val nationalBankIdentifier: String,
    val country: CountryCdq,
)
