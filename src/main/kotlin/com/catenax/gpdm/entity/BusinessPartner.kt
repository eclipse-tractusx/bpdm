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
    val status: BusinessPartnerStatus?,
    @OneToMany(mappedBy = "partner")
    val addresses: Set<Address>,
    @ManyToMany(cascade = [ CascadeType.ALL ])
    @JoinTable(
        name = "business_partners_classifications",
        joinColumns = [ JoinColumn(name = "partner_id") ],
        inverseJoinColumns = [JoinColumn(name = "classification_id")]
    )
    val classification: Set<Classification>,
    @OneToMany(mappedBy = "startNode")
    val startNodeRelations: Set<Relation>,
    @OneToMany(mappedBy = "endNode")
    val endNodeRelations: Set<Relation>,
    @ElementCollection(targetClass = BusinessPartnerTypes::class)
    @JoinTable(name = "business_partner_types", joinColumns = [JoinColumn(name = "partner_id")])
    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    val types: Set<BusinessPartnerTypes>,
    @OneToMany(mappedBy = "partner")
    val bankAccounts: Set<BankAccount>,
    @ElementCollection(targetClass = String::class)
    @JoinTable(name = "business_partner_roles", joinColumns = [JoinColumn(name = "partner_id")])
    @Column(name = "role", nullable = false)
    val roles: Set<String>
) : BaseEntity()

enum class BusinessPartnerStatus(val description: String){
    ACTIVE("The business partner is active."),
    DISSOLVED("The business partner is no longer active as the result of an insolvency or in liquidation process."),
    IN_LIQUIDATION("Liquidation is the process by which a company is brought to an end. The assets and property of the company are redistributed."),
    INACTIVE("Generic status that indicates that a business partner existed in the past, but is as of today not operational."),
    INSOLVENCY("Status is assigned if a business partner is in the state of being unable to pay the money owed on time.")
}

enum class BusinessPartnerTypes(val description: String){
    BRAND("A business partner that has no legal standing in the eyes of law by its own, but is a brand of a legal entity."),
    LEGAL_ENTITY("A business partner that has legal standing in the eyes of law."),
    ORGANIZATIONAL_UNIT("A business partner that has no legal standing in the eyes of law by its own, but is an organizational unit, division or subsidiary of a legal entity."),
    SITE("A business partner that encodes a geographic location with one or more address to access the site."),
    UNKNOWN("This type is assigned if no specific type is known.")
}