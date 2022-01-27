package com.catenax.gpdm.entity

import com.neovisionaries.i18n.CurrencyCode
import javax.persistence.*

@Entity
@Table(name = "bank_accounts")
class BankAccount (
    @ElementCollection(targetClass = Float::class)
    @JoinTable(name = "bank_account_trust_scores", joinColumns = [JoinColumn(name = "account_id")])
    @Column(name = "score", nullable = false)
    val trustScores: Set<Float>,
    @Column(name = "currency", nullable = false)
    @Enumerated(EnumType.STRING)
    val currency: CurrencyCode,
    @Column(name = "international_account_identifier", nullable = false)
    val internationalBankAccountIdentifier: String,
    @Column(name = "international_bank_identifier", nullable = false)
    val internationalBankIdentifier: String,
    @Column(name = "national_account_identifier", nullable = false)
    val nationalBankAccountIdentifier: String,
    @Column(name = "national_bank_identifier", nullable = false)
    val nationalBankIdentifier: String,

    @ManyToOne
    @JoinColumn(name = "partner_id", nullable = false)
    val partner: BusinessPartner
        ):BaseEntity()