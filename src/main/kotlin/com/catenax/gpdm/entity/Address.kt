package com.catenax.gpdm.entity

import com.neovisionaries.i18n.CountryCode
import javax.persistence.*

@Entity
@Table(
    name = "addresses",
    indexes = [
        Index(columnList = "partner_id")
    ]
)
class Address (
    @Column(name = "bpn", nullable = false, unique = true)
    var bpn: String,
    @Column(name = "care_of")
    var careOf: String?,
    @ElementCollection(targetClass = String::class)
    @JoinTable(
        name = "address_contexts",
        joinColumns = [JoinColumn(name = "address_id")],
        indexes = [Index(columnList = "address_id")]
    )
    @Column(name = "context", nullable = false)
    val contexts: MutableSet<String> = mutableSetOf(),
    @Column(name = "country", nullable = false)
    @Enumerated(EnumType.STRING)
    var country: CountryCode,
    @ElementCollection(targetClass = AddressType::class)
    @Enumerated(EnumType.STRING)
    @JoinTable(
        name = "address_types",
        joinColumns = [JoinColumn(name = "address_id")],
        indexes = [Index(columnList = "address_id")]
    )
    @Column(name = "type", nullable = false)
    val types: MutableSet<AddressType> = mutableSetOf(),
    @Embedded
    var version: AddressVersion,
    @Embedded
    var geoCoordinates: GeographicCoordinate?,
    @ManyToOne
    @JoinColumn(name = "partner_id")
    var partner: BusinessPartner?,
    @ManyToOne
    @JoinColumn(name = "site_id")
    var site: Site?,
    @Column(name = "name")
    var name: String?
) : BaseEntity() {
    @OneToMany(mappedBy = "address", cascade = [CascadeType.ALL], orphanRemoval = true)
    val administrativeAreas: MutableSet<AdministrativeArea> = mutableSetOf()

    @OneToMany(mappedBy = "address", cascade = [CascadeType.ALL], orphanRemoval = true)
    val postCodes: MutableSet<PostCode> = mutableSetOf()

    @OneToMany(mappedBy = "address", cascade = [CascadeType.ALL], orphanRemoval = true)
    val thoroughfares: MutableSet<Thoroughfare> = mutableSetOf()

    @OneToMany(mappedBy = "address", cascade = [CascadeType.ALL], orphanRemoval = true)
   val premises: MutableSet<Premise> = mutableSetOf()

    @OneToMany(mappedBy = "address", cascade = [CascadeType.ALL], orphanRemoval = true)
    val postalDeliveryPoints: MutableSet<PostalDeliveryPoint> = mutableSetOf()

    @OneToMany(mappedBy = "address", cascade = [CascadeType.ALL], orphanRemoval = true)
   val localities: MutableSet<Locality> = mutableSetOf()
}



enum class AddressType(private val typeName: String, private val url: String): NamedUrlType, HasDefaultValue<AddressType>{
    BRANCH_OFFICE("Branch Office", ""),
    CARE_OF("Care of (c/o) Address", ""),
    HEADQUARTER("Headquarter", ""),
    LEGAL("Legal", ""),
    LEGAL_ALTERNATIVE("Legal Alternative", ""),
    PO_BOX("Post Office Box", ""),
    REGISTERED("Registered", ""),
    REGISTERED_AGENT_MAIL("Registered Agent Mail", ""),
    REGISTERED_AGENT_PHYSICAL("Registered Agent Physical", ""),
    VAT_REGISTERED("Vat Registered", ""),
    UNSPECIFIC("Unspecified", "");

    override fun getTypeName(): String {
        return typeName
    }

    override fun getUrl(): String {
        return url
    }

    override fun getDefault(): AddressType {
        return UNSPECIFIC
    }
}