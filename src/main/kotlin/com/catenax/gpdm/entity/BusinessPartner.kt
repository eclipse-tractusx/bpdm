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
    val identifiers: Set<Identifier>
) : BaseEntity()