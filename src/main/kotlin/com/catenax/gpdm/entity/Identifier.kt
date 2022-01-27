package com.catenax.gpdm.entity

import javax.persistence.*

@MappedSuperclass
abstract class Identifier(
    value: String,
    shortName: String?,
    number: Int?,
    @Column(name="type", nullable=false)
    val type: String,
    @OneToOne(cascade = [CascadeType.PERSIST])
    @JoinColumn(name = "registration_id")
    val registration: Registration?
) : BaseNamedEntity(value, shortName, number)
