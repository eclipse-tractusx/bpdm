package com.catenax.gpdm.entity

import javax.persistence.*

@Entity
@Table(name = "business_partners")
class BusinessPartner(
    @OneToMany(mappedBy = "partner")
    val names : Set<Name>
) : BaseEntity()