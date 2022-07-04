package org.eclipse.tractusx.bpdm.pool.component.cdq.dto

data class BankAccountCdq(
    val internationalBankAccountIdentifier: String,
    val internationalBankIdentifier: String,
    val nationalBankAccountIdentifier: String,
    val nationalBankIdentifier: String,
    val country: CountryCdq,
)
