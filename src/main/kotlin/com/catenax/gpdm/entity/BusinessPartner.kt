package com.catenax.gpdm.entity

import javax.persistence.*

@Entity
@Table(name = "business_partners")
class BusinessPartner(
    @Column(name="bpn", nullable = false, unique = true)
    val bpn: String,
    @ManyToOne
    @JoinColumn(name="legal_form_id", nullable=false)
    val legalForm: LegalForm,
    @ElementCollection(targetClass = BusinessPartnerType::class)
    @JoinTable(name = "business_partner_types", joinColumns = [JoinColumn(name = "partner_id")])
    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    val types: Set<BusinessPartnerType>,
    @ManyToMany(cascade = [ CascadeType.ALL ])
    @JoinTable(
        name = "business_partners_roles",
        joinColumns = [ JoinColumn(name = "partner_id") ],
        inverseJoinColumns = [JoinColumn(name = "role_id")]
    )
    val roles: Set<Role>
): BaseEntity(){
    @OneToMany(mappedBy = "partner", cascade = [CascadeType.PERSIST], orphanRemoval = true)
    lateinit var identifiers: Set<Identifier>
    @OneToMany(mappedBy = "partner", cascade = [CascadeType.PERSIST], orphanRemoval = true)
    lateinit var names: Set<Name>
    @OneToMany(mappedBy = "partner", cascade = [CascadeType.PERSIST], orphanRemoval = true)
    lateinit var stati: Set<BusinessStatus>
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



enum class BusinessPartnerType(private val typeName: String, private val url: String): NamedUrlType{
    BRAND("Brand", ""),
    LEGAL_ENTITY("Legal Entity", ""),
    ORGANIZATIONAL_UNIT("Organizational Unit", ""),
    SITE("Site", ""),
    UNKNOWN("Unknown", "");

    override fun getTypeName(): String {
        return typeName
    }

    override fun getUrl(): String {
        return url
    }
}