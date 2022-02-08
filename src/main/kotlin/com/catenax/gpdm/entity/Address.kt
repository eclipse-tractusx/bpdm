package com.catenax.gpdm.entity

import com.neovisionaries.i18n.CountryCode
import javax.persistence.*

@Entity
@Table(name = "addresses")
class Address (
    @Column(name="care_of")
    val careOf: String?,
    @ElementCollection(targetClass = String::class)
    @JoinTable(name = "address_contexts", joinColumns = [JoinColumn(name = "address_id")])
    @Column(name = "context", nullable = false)
    val contexts: Set<String>,
    @Column(name = "country", nullable = false)
    @Enumerated(EnumType.STRING)
    val country: CountryCode,
    @ElementCollection(targetClass = AddressType::class)
    @Enumerated(EnumType.STRING)
    @JoinTable(name = "address_types", joinColumns = [JoinColumn(name = "address_id")])
    @Column(name = "type", nullable = false)
    val types: Set<AddressType>,
    @ManyToOne(cascade = [CascadeType.PERSIST])
    @JoinColumn(name = "version_id", nullable = false)
    val version: AddressVersion,
    @Embedded
    val geoCoordinates: GeographicCoordinate?,
    @ManyToOne
    @JoinColumn(name="partner_id", nullable=false)
    val partner: BusinessPartner
        ) : BaseEntity(){
    @OneToMany(mappedBy = "address", cascade = [CascadeType.PERSIST])
    lateinit var administrativeAreas: Set<AdministrativeArea>
    @OneToMany(mappedBy = "address", cascade = [CascadeType.PERSIST])
    lateinit var postCodes: Set<PostCode>
    @OneToMany(mappedBy = "address", cascade = [CascadeType.PERSIST])
    lateinit var thoroughfares: Set<Thoroughfare>
    @OneToMany(mappedBy = "address", cascade = [CascadeType.PERSIST])
    lateinit var premises: Set<Premise>
    @OneToMany(mappedBy = "address", cascade = [CascadeType.PERSIST])
    lateinit var postalDeliveryPoints: Set<PostalDeliveryPoint>
    @OneToMany(mappedBy = "address", cascade = [CascadeType.PERSIST])
    lateinit var localities: Set<Locality>
        }



enum class AddressType(private val typeName: String, private val url: String): NamedUrlType{
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
}