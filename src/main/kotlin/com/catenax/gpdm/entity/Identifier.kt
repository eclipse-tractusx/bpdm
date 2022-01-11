package com.catenax.gpdm.entity

import javax.persistence.*

@Entity
@Table(name = "identifiers")
class Identifier(
    value: String,
    shortName: String?,
    number: Int?,
    @Column(name="type", nullable=false)
    val type: String,
    @OneToOne
    @JoinColumn(name = "registration_id")
    val registration: Registration?
) : BaseNamedEntity(value, shortName, number)
