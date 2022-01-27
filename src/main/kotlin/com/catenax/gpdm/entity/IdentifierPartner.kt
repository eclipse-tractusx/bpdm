package com.catenax.gpdm.entity

import javax.persistence.*

@Entity
@Table(name = "partner_identifiers")
class IdentifierPartner (
    value: String,
    shortName: String?,
    number: Int?,
    type: String,
    registration: Registration?,
    @ManyToOne
    @JoinColumn(name = "partner_id", nullable = false)
    val partner: BusinessPartner
        ) : Identifier(value, shortName, number, type, registration)