package com.catenax.gpdm.entity

import javax.persistence.*

@Entity
@Table(name = "business_partners")
class BusinessPartner(
    @OneToMany(mappedBy = "partner")
    val names: Set<Name>,
    @ManyToOne
    @JoinColumn(name="legal_form_id", nullable=false)
    val legalForm: LegalForm,
    @ManyToMany(cascade = [ CascadeType.ALL ])
    @JoinTable(
        name = "business_partners_identifiers",
        joinColumns = [ JoinColumn(name = "partner_id") ],
        inverseJoinColumns = [JoinColumn(name = "identifier_id")]
    )
    val identifiers: Set<Identifier>,
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    val status: BusinessPartnerStatus?
) : BaseEntity()

enum class BusinessPartnerStatus(val description: String){
    ACTIVE("The business partner is active."),
    DISSOLVED("The business partner is no longer active as the result of an insolvency or in liquidation process."),
    IN_LIQUIDATION("Liquidation is the process by which a company is brought to an end. The assets and property of the company are redistributed."),
    INACTIVE("Generic status that indicates that a business partner existed in the past, but is as of today not operational."),
    INSOLVENCY("Status is assigned if a business partner is in the state of being unable to pay the money owed on time.")
}