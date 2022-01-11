package com.catenax.gpdm.entity

import javax.persistence.*

@Entity
@Table(name = "business_partners")
class BusinessPartner(
    @OneToMany(mappedBy = "partner")
    val names: Set<Name>,
    @ManyToOne
    @JoinColumn(name="legal_form_id", nullable=false)
    val legalForm: LegalForm
) : BaseEntity()