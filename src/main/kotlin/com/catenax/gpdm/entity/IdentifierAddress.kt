package com.catenax.gpdm.entity

import javax.persistence.*

@Entity
@Table(name = "address_identifiers")
class IdentifierAddress (
    value: String,
    shortName: String?,
    number: Int?,
    type: String,
    registration: Registration?,
    @ManyToOne
    @JoinColumn(name = "address_id", nullable = false)
    val address: Address
    ) : Identifier(value, shortName, number, type, registration)