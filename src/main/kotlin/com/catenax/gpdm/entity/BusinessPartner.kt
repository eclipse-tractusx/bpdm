package com.catenax.gpdm.entity

import javax.persistence.*

@Entity
@Table(name = "business_partners")
class BusinessPartner(
    @Column(name="bpn", nullable = false, unique = true)
    val bpn: String,
    @ManyToOne(cascade = [CascadeType.PERSIST])
    @JoinColumn(name="legal_form_id", nullable=false)
    val legalForm: LegalForm,
    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    val status: BusinessPartnerStatus?,
    @ElementCollection(targetClass = BusinessPartnerTypes::class)
    @JoinTable(name = "business_partner_types", joinColumns = [JoinColumn(name = "partner_id")])
    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    val types: Set<BusinessPartnerTypes>,
    @ElementCollection(targetClass = String::class)
    @JoinTable(name = "business_partner_roles", joinColumns = [JoinColumn(name = "partner_id")])
    @Column(name = "role", nullable = false)
    val roles: Set<String>
): BaseEntity(){
    @OneToMany(mappedBy = "partner", cascade = [CascadeType.PERSIST], orphanRemoval = true)
    lateinit var identifiers: Set<IdentifierPartner>
    @OneToMany(mappedBy = "partner", cascade = [CascadeType.PERSIST], orphanRemoval = true)
    lateinit var names: Set<Name>
    @OneToMany(mappedBy = "partner", cascade = [CascadeType.PERSIST], orphanRemoval = true)
    lateinit var addresses: Set<Address>
    @OneToMany(mappedBy = "partner",cascade = [CascadeType.PERSIST], orphanRemoval = true)
    lateinit var classification: Set<Classification>
    @OneToMany(mappedBy = "partner",cascade = [CascadeType.PERSIST], orphanRemoval = true)
    lateinit var bankAccounts: Set<BankAccount>
    @OneToMany(mappedBy = "startNode")
    lateinit var startNodeRelations: Set<Relation>
    @OneToMany(mappedBy = "endNode")
    lateinit var endNodeRelations: Set<Relation>
}

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