package com.catenax.gpdm.entity

import com.neovisionaries.i18n.CountryCode
import com.neovisionaries.i18n.LanguageCode
import javax.persistence.*

@Entity
@Table(name = "addresses")
class Address (
    @ManyToMany(cascade = [ CascadeType.ALL ])
    @JoinTable(
        name = "addresses_identifiers",
        joinColumns = [ JoinColumn(name = "address_id") ],
        inverseJoinColumns = [JoinColumn(name = "identifier_id")]
    )
    val identifiers: Set<Identifier>,
    @OneToOne
    @JoinColumn(name = "care_of_id")
    val careOf: CareOf?,
    @Column(name = "country", nullable = false)
    @Enumerated(EnumType.STRING)
    val country: CountryCode,
    @ManyToMany(cascade = [ CascadeType.ALL ])
    @JoinTable(
        name = "addresses_administrative_areas",
        joinColumns = [ JoinColumn(name = "address_id") ],
        inverseJoinColumns = [JoinColumn(name = "area_id")]
    )
    val administrativeAreas: Set<AdministrativeArea>,
    @ManyToMany(cascade = [ CascadeType.ALL ])
    @JoinTable(
        name = "addresses_post_codes",
        joinColumns = [ JoinColumn(name = "address_id") ],
        inverseJoinColumns = [JoinColumn(name = "post_id")]
    )
    val postCodes: Set<PostCode>,
    @ManyToMany(cascade = [ CascadeType.ALL ])
    @JoinTable(
        name = "addresses_localities",
        joinColumns = [ JoinColumn(name = "address_id") ],
        inverseJoinColumns = [JoinColumn(name = "locality_id")]
    )
    val localities: Set<Locality>,
    @OneToMany(mappedBy = "address")
    val thoroughfares: Set<Thoroughfare>,
    @OneToMany(mappedBy = "address")
    val premises: Set<Premise>,
    @OneToMany(mappedBy = "address")
    val postalDeliveryPoints: Set<PostalDeliveryPoint>,
    @Column(name = "type", nullable = false)
    val type: AddressType,
    @ManyToMany(cascade = [ CascadeType.ALL ])
    @JoinTable(
        name = "addresses_versions",
        joinColumns = [ JoinColumn(name = "address_id") ],
        inverseJoinColumns = [JoinColumn(name = "version_id")]
    )
    val versions: Set<AddressVersion>,

    @ManyToOne
    @JoinColumn(name="partner_id", nullable=false)
    val partner: BusinessPartner
        ) : BaseEntity()



enum class AddressType(val description: String){
    BRANCH_OFFICE("The main registered address for a branch of a legal entity."),
    CARE_OF("Care of (c/o) address"),
    HEADQUARTER("The headquarter address denotes the location where most, if not all, of the important functions of an organization are coordinated."),
    LEGAL("A legal address is the main address of a business partner the business partner is legally registered at. "),
    LEGAL_ALTERNATIVE("In rare cases the same legal entity is registered at different business registers. "),
    PO_BOX("An address which is specified by a post office (PO) box."),
    REGISTERED("A registered address is an address of a business partner that is officially registered in a business register (or similar official registers). "),
    REGISTERED_AGENT_MAIL("A registered agent mail address is a mailing address of a registered agent of a particular business"),
    REGISTERED_AGENT_PHYSICAL("A registered agent physical address is the physical address of a registered agent of a particular business"),
    VAT_REGISTERED("The address which is associated with the VAT number of a business partner, i.e. the address stored in a VAT register. "),
    UNSPECIFIC("This type is assigned as address type if no specific type is known. ")
}